# Test cards

## Cards that are approved
- 4242 4242 4242 4242: Visa
- 5555 5555 5555 4444: Mastercard
- 3782 822463 10005: American Express
Use any future expiry date and any cardholder name.

## Cards that are declined
- 4000 0000 0000 0002: declined with card_declined
- 4000 0000 0000 9995: declined with insufficient_funds
- Any card with an amount above RM 50,000.00: declined with amount_limit_exceeded

## Invalid cards
Card numbers that fail the Luhn checksum, unsupported brands and expired cards are rejected when tokenizing,
before any payment is attempted.
