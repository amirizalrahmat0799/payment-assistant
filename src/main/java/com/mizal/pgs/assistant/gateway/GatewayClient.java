package com.mizal.pgs.assistant.gateway;

import com.mizal.pgs.assistant.config.AssistantProperties;
import com.mizal.pgs.assistant.gateway.GatewayModels.LedgerEntry;
import com.mizal.pgs.assistant.gateway.GatewayModels.Merchant;
import com.mizal.pgs.assistant.gateway.GatewayModels.Payment;
import com.mizal.pgs.assistant.gateway.GatewayModels.PaymentPage;
import com.mizal.pgs.assistant.gateway.GatewayModels.Refund;
import com.mizal.pgs.assistant.gateway.GatewayModels.Settlement;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Read-only client for the payment gateway. Every call is made with the merchant's own API key,
 * so the assistant can never see data the merchant couldn't see in the dashboard.
 */
@Component
public class GatewayClient {

    private static final String API_KEY = "X-Api-Key";

    private final RestClient merchant;
    private final RestClient payment;
    private final RestClient settlement;

    public GatewayClient(RestClient.Builder builder, AssistantProperties properties) {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(2_000);
        timeouts.setReadTimeout(5_000);
        this.merchant = builder.clone().baseUrl(properties.gateway().merchantUrl()).requestFactory(timeouts).build();
        this.payment = builder.clone().baseUrl(properties.gateway().paymentUrl()).requestFactory(timeouts).build();
        this.settlement = builder.clone().baseUrl(properties.gateway().settlementUrl()).requestFactory(timeouts).build();
    }

    public Merchant authenticate(String apiKey) {
        return merchant.get().uri("/api/v1/merchants/me")
                .header(API_KEY, apiKey)
                .retrieve()
                .onStatus(s -> s.value() == HttpStatus.UNAUTHORIZED.value(), (req, res) -> {
                    throw new InvalidApiKeyException();
                })
                .body(Merchant.class);
    }

    public PaymentPage recentPayments(String apiKey, int size) {
        return payment.get().uri("/api/v1/payments?page=0&size={size}", size)
                .header(API_KEY, apiKey)
                .retrieve()
                .body(PaymentPage.class);
    }

    public Payment payment(String apiKey, String paymentId) {
        return payment.get().uri("/api/v1/payments/{id}", paymentId)
                .header(API_KEY, apiKey)
                .retrieve()
                .onStatus(s -> s.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                    throw new NotFoundException("Payment " + paymentId + " not found");
                })
                .body(Payment.class);
    }

    public List<Refund> refunds(String apiKey, String paymentId) {
        return payment.get().uri("/api/v1/payments/{id}/refunds", paymentId)
                .header(API_KEY, apiKey)
                .retrieve()
                .onStatus(s -> s.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                    throw new NotFoundException("Payment " + paymentId + " not found");
                })
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public List<LedgerEntry> ledger(String merchantId, int limit) {
        return settlement.get().uri("/api/v1/ledger?merchantId={id}&limit={limit}", merchantId, limit)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public List<Settlement> settlements(String merchantId) {
        return settlement.get().uri("/api/v1/settlements?merchantId={id}", merchantId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public static class InvalidApiKeyException extends RuntimeException {
        public InvalidApiKeyException() {
            super("Missing, invalid or inactive API key");
        }
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }
}
