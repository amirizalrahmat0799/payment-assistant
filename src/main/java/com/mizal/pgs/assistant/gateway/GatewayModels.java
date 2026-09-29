package com.mizal.pgs.assistant.gateway;

import java.util.List;

/** Subsets of the gateway's JSON responses: only the fields the assistant needs. */
public final class GatewayModels {

    private GatewayModels() {
    }

    public record Merchant(String id, String name, String email, String status, int feeBps) {
    }

    public record Card(String brand, String last4) {
    }

    public record Payment(String id, String status, long amount, String currency, long capturedAmount,
                          long refundedAmount, long refundableAmount, Card card, String authCode,
                          String declineReason, String description, String createdAt, String updatedAt) {
    }

    public record PaymentPage(List<Payment> items, int page, int size, long totalItems, int totalPages) {
    }

    public record Refund(String id, String paymentId, long amount, String createdAt) {
    }

    public record LedgerEntry(String id, String paymentId, String entryType, long amount, String currency,
                              long fee, String occurredAt, String settlementId) {
    }

    public record Settlement(String id, String currency, String settlementDate, long grossCaptured,
                             long grossRefunded, long fees, long netAmount, int entryCount) {
    }
}
