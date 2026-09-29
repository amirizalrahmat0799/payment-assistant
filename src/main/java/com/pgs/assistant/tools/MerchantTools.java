package com.pgs.assistant.tools;

import com.pgs.assistant.gateway.GatewayClient;
import com.pgs.assistant.gateway.GatewayModels.LedgerEntry;
import com.pgs.assistant.gateway.GatewayModels.Payment;
import com.pgs.assistant.gateway.GatewayModels.Refund;
import com.pgs.assistant.gateway.GatewayModels.Settlement;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tools the model can call to look up the merchant's own data.
 * <p>
 * Deliberately <b>read-only</b>: the assistant can explain a refund but never issue one. Anything
 * that moves money stays a human action in the dashboard.
 */
@Component
public class MerchantTools {

    private static final Set<String> STATUSES =
            Set.of("AUTHORIZED", "DECLINED", "CAPTURED", "PARTIALLY_REFUNDED", "REFUNDED", "VOIDED");
    private static final int MAX_LIST = 20;

    private final GatewayClient gateway;

    public MerchantTools(GatewayClient gateway) {
        this.gateway = gateway;
    }

    // ---------- Results returned to the model (compact, with formatted money) ----------

    public record PaymentSummary(String id, String status, String amount, String captured, String refunded,
                                 String refundable, String card, String declineReason, String description,
                                 String createdAt) {

        static PaymentSummary of(Payment p) {
            return new PaymentSummary(p.id(), p.status(), Money.format(p.amount(), p.currency()),
                    Money.format(p.capturedAmount(), p.currency()), Money.format(p.refundedAmount(), p.currency()),
                    Money.format(p.refundableAmount(), p.currency()),
                    p.card() == null ? null : p.card().brand() + " ending " + p.card().last4(),
                    p.declineReason(), p.description(), p.createdAt());
        }
    }

    public record RefundSummary(String id, String amount, String createdAt) {
    }

    public record PayoutSummary(String pendingNet, int pendingEntries, String pendingCaptured, String pendingRefunded,
                                String pendingFees, String lastSettlementDate, String lastSettlementNet) {
    }

    public record SettlementSummary(String date, String captured, String refunded, String fees, String net, int entries) {
    }

    // ---------- Tools ----------

    @Tool(description = """
            Look up a single payment by its ID (a UUID). Returns status, amounts, card brand and last 4 digits, \
            and the decline reason if it was declined.""")
    public PaymentSummary getPayment(@ToolParam(description = "The payment ID, a UUID") String paymentId,
                                     ToolContext context) {
        log(context, "getPayment");
        return PaymentSummary.of(gateway.payment(apiKey(context), paymentId.trim()));
    }

    @Tool(description = """
            List the merchant's most recent payments, newest first, optionally filtered by status. \
            Valid statuses: AUTHORIZED, DECLINED, CAPTURED, PARTIALLY_REFUNDED, REFUNDED, VOIDED.""")
    public List<PaymentSummary> listRecentPayments(
            @ToolParam(description = "Optional status filter", required = false) String status,
            @ToolParam(description = "How many payments to return, 1-20 (default 10)", required = false) Integer limit,
            ToolContext context) {
        log(context, "listRecentPayments");
        int n = limit == null ? 10 : Math.max(1, Math.min(limit, MAX_LIST));
        String wanted = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (wanted != null && !STATUSES.contains(wanted)) {
            throw new IllegalArgumentException("Unknown status " + status + ". Use one of " + STATUSES);
        }
        // Look at a wider window when filtering, since the API pages by date only.
        var page = gateway.recentPayments(apiKey(context), wanted == null ? n : 100);
        return page.items().stream()
                .filter(p -> wanted == null || wanted.equals(p.status()))
                .limit(n)
                .map(PaymentSummary::of)
                .toList();
    }

    @Tool(description = "List the refunds issued for a payment.")
    public List<RefundSummary> getRefunds(@ToolParam(description = "The payment ID, a UUID") String paymentId,
                                          ToolContext context) {
        log(context, "getRefunds");
        String key = apiKey(context);
        Payment payment = gateway.payment(key, paymentId.trim());
        List<Refund> refunds = gateway.refunds(key, paymentId.trim());
        return refunds.stream()
                .map(r -> new RefundSummary(r.id(), Money.format(r.amount(), payment.currency()), r.createdAt()))
                .toList();
    }

    @Tool(description = """
            Summarise money that has not been paid out yet (captured minus refunds minus fees, not yet settled) \
            and the most recent settlement. Use for questions like "how much will I be paid" or "when is my payout".""")
    public PayoutSummary getPayoutSummary(ToolContext context) {
        log(context, "getPayoutSummary");
        String merchantId = merchantId(context);
        List<LedgerEntry> pending = gateway.ledger(merchantId, 500).stream()
                .filter(e -> e.settlementId() == null)
                .toList();
        String currency = pending.isEmpty() ? "MYR" : pending.get(0).currency();
        long captured = pending.stream().filter(e -> "CAPTURE".equals(e.entryType())).mapToLong(LedgerEntry::amount).sum();
        long refunded = pending.stream().filter(e -> "REFUND".equals(e.entryType())).mapToLong(LedgerEntry::amount).sum();
        long fees = pending.stream().mapToLong(LedgerEntry::fee).sum();

        List<Settlement> settlements = gateway.settlements(merchantId);
        Settlement last = settlements.isEmpty() ? null : settlements.get(0);
        return new PayoutSummary(
                Money.format(captured - refunded - fees, currency), pending.size(),
                Money.format(captured, currency), Money.format(refunded, currency), Money.format(fees, currency),
                last == null ? null : last.settlementDate(),
                last == null ? null : Money.format(last.netAmount(), last.currency()));
    }

    @Tool(description = "List past settlements (payouts), newest first, with captured, refunded, fees and net amounts.")
    public List<SettlementSummary> listSettlements(
            @ToolParam(description = "How many settlements to return, 1-20 (default 7)", required = false) Integer limit,
            ToolContext context) {
        log(context, "listSettlements");
        int n = limit == null ? 7 : Math.max(1, Math.min(limit, MAX_LIST));
        return gateway.settlements(merchantId(context)).stream()
                .limit(n)
                .map(s -> new SettlementSummary(s.settlementDate(), Money.format(s.grossCaptured(), s.currency()),
                        Money.format(s.grossRefunded(), s.currency()), Money.format(s.fees(), s.currency()),
                        Money.format(s.netAmount(), s.currency()), s.entryCount()))
                .toList();
    }

    // ---------- helpers ----------

    private static String apiKey(ToolContext context) {
        return (String) context.getContext().get(ToolContextKeys.API_KEY);
    }

    private static String merchantId(ToolContext context) {
        return (String) context.getContext().get(ToolContextKeys.MERCHANT_ID);
    }

    @SuppressWarnings("unchecked")
    private static void log(ToolContext context, String tool) {
        Object log = context.getContext().get(ToolContextKeys.TOOL_LOG);
        if (log instanceof List<?> list) {
            ((List<String>) list).add(tool);
        }
    }
}
