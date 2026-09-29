# Payment lifecycle

## Statuses
Every payment has one of six statuses:
- AUTHORIZED: the card issuer approved the payment and the money is on hold. Nothing has been charged yet.
- CAPTURED: the held amount (or part of it) has been charged. Captured money appears in the next settlement.
- PARTIALLY_REFUNDED: part of the captured amount was refunded.
- REFUNDED: the full captured amount was refunded.
- VOIDED: an authorization was cancelled before capture. The customer is never charged.
- DECLINED: the issuer rejected the payment. It is final and cannot be retried; create a new payment instead.

## Authorize and capture
A payment can be created with capture set to true (authorize and charge in one step) or false (authorize only).
Authorize-only is useful when the final amount isn't known yet, for example a restaurant bill before the tip.
An authorized payment can be captured later for the full amount or a smaller amount. A partial capture releases
the rest of the hold. A payment can only be captured once.

## Voiding an authorization
An AUTHORIZED payment can be voided from the dashboard (Payments, open the payment, Void). Voiding releases the hold
and the customer is not charged. Captured payments cannot be voided; refund them instead.

## Refunds
Only CAPTURED or PARTIALLY_REFUNDED payments can be refunded. Several partial refunds are allowed as long as the
total does not exceed the captured amount. The payment becomes REFUNDED when the refunds add up to the captured
amount. Refunds are issued from the dashboard: Payments, open the payment, enter an amount (or leave it empty for
the remaining amount) and click Refund. The assistant cannot issue refunds.

## Why a status change might fail
The gateway rejects operations that don't fit the current status with a 422 error, for example capturing twice,
refunding an authorization that was never captured, or refunding more than the refundable amount. If two people
change the same payment at the same moment, one of them gets a 409 conflict and should reload and retry.
