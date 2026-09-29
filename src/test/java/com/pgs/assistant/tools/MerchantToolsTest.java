package com.pgs.assistant.tools;

import com.pgs.assistant.gateway.GatewayClient;
import com.pgs.assistant.gateway.GatewayModels.Card;
import com.pgs.assistant.gateway.GatewayModels.LedgerEntry;
import com.pgs.assistant.gateway.GatewayModels.Payment;
import com.pgs.assistant.gateway.GatewayModels.PaymentPage;
import com.pgs.assistant.gateway.GatewayModels.Settlement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MerchantToolsTest {

    private final GatewayClient gateway = mock(GatewayClient.class);
    private final MerchantTools tools = new MerchantTools(gateway);
    private List<String> toolLog;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        toolLog = new ArrayList<>();
        context = new ToolContext(Map.of(
                ToolContextKeys.API_KEY, "sk_test_key",
                ToolContextKeys.MERCHANT_ID, "m-1",
                ToolContextKeys.TOOL_LOG, toolLog));
    }

    private static Payment payment(String id, String status, long amount, long captured, long refunded, String decline) {
        return new Payment(id, status, amount, "MYR", captured, refunded, captured - refunded,
                new Card("VISA", "4242"), null, decline, "Order", "2026-09-29T10:00:00Z", "2026-09-29T10:00:00Z");
    }

    @Test
    void getPaymentUsesTheMerchantsOwnKeyAndFormatsMoney() {
        when(gateway.payment("sk_test_key", "p-1")).thenReturn(payment("p-1", "CAPTURED", 15_000, 15_000, 2_000, null));

        MerchantTools.PaymentSummary summary = tools.getPayment(" p-1 ", context);

        assertThat(summary.amount()).isEqualTo("RM 150.00");
        assertThat(summary.refundable()).isEqualTo("RM 130.00");
        assertThat(summary.card()).isEqualTo("VISA ending 4242");
        assertThat(toolLog).containsExactly("getPayment");
    }

    @Test
    void listRecentPaymentsFiltersByStatusWithinAWiderWindow() {
        when(gateway.recentPayments("sk_test_key", 100)).thenReturn(new PaymentPage(List.of(
                payment("a", "DECLINED", 500, 0, 0, "card_declined"),
                payment("b", "CAPTURED", 900, 900, 0, null),
                payment("c", "DECLINED", 700, 0, 0, "insufficient_funds")), 0, 100, 3, 1));

        var declined = tools.listRecentPayments("declined", 5, context);

        assertThat(declined).extracting(MerchantTools.PaymentSummary::id).containsExactly("a", "c");
        assertThat(declined).extracting(MerchantTools.PaymentSummary::declineReason)
                .containsExactly("card_declined", "insufficient_funds");
    }

    @Test
    void listRecentPaymentsClampsTheLimit() {
        when(gateway.recentPayments(eq("sk_test_key"), anyInt())).thenReturn(new PaymentPage(List.of(), 0, 20, 0, 0));

        tools.listRecentPayments(null, 500, context);

        verify(gateway).recentPayments("sk_test_key", 20);
    }

    @Test
    void rejectsUnknownStatus() {
        assertThatThrownBy(() -> tools.listRecentPayments("PENDING", 5, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown status");
    }

    @Test
    void payoutSummaryCountsOnlyUnsettledLedgerEntries() {
        when(gateway.ledger("m-1", 500)).thenReturn(List.of(
                new LedgerEntry("1", "p1", "CAPTURE", 10_000, "MYR", 250, "2026-09-29T01:00:00Z", null),
                new LedgerEntry("2", "p1", "REFUND", 2_000, "MYR", 0, "2026-09-29T02:00:00Z", null),
                new LedgerEntry("3", "p2", "CAPTURE", 50_000, "MYR", 1_250, "2026-09-28T02:00:00Z", "s-1")));
        when(gateway.settlements("m-1")).thenReturn(List.of(
                new Settlement("s-1", "MYR", "2026-09-28", 50_000, 0, 1_250, 48_750, 1)));

        MerchantTools.PayoutSummary payout = tools.getPayoutSummary(context);

        assertThat(payout.pendingNet()).isEqualTo("RM 77.50");      // 100.00 - 20.00 - 2.50
        assertThat(payout.pendingEntries()).isEqualTo(2);
        assertThat(payout.lastSettlementDate()).isEqualTo("2026-09-28");
        assertThat(payout.lastSettlementNet()).isEqualTo("RM 487.50");
    }
}
