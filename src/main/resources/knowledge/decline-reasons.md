# Decline reasons

## card_declined
The issuing bank declined the card without giving a specific reason. Ask the customer to use a different card
or contact their bank. Retrying the same card usually fails again.

## insufficient_funds
The card doesn't have enough available balance or credit for this amount. The customer can try a smaller amount,
another card, or pay later.

## expired_card
The card's expiry date has passed. The customer needs to use a card that is still valid. Cards that are already
expired are also rejected earlier, when the card number is tokenized.

## amount_limit_exceeded
The amount is above the per-transaction limit of RM 50,000.00. Split the purchase into smaller payments.

## What to do after a decline
A declined payment is final. Create a new payment (with a new Idempotency-Key) once the customer has fixed the
problem. Declined payments are never charged, never appear in settlements and incur no fee.
