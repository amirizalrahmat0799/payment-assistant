# API and idempotency

## API keys
Merchants authenticate every API call with the X-Api-Key header. Keys start with sk_test_ and are shown only once,
when the account is created or the key is rotated; the gateway stores only a hash. If a key may have leaked,
rotate it in Settings. The old key stops working immediately. Never share your API key, including with support.

## Idempotency keys
Creating a payment requires an Idempotency-Key header (8 to 100 characters), for example the order number.
If a request times out and is retried with the same key and the same body, the gateway returns the original
payment instead of charging the customer twice; the response has the header Idempotent-Replayed: true.
Reusing a key with a different body (for example a different amount) is rejected with a 422 error.
Use a new key for every new purchase.

## Card tokenization
Card numbers are sent only to the tokenization service, which returns a token such as tok_... The payment API is
then called with the token. The gateway stores card numbers encrypted (AES-256-GCM) and never shows them in full:
only the brand and last 4 digits are visible.

## Amounts
All amounts in the API are integers in minor units (sen). 15000 means RM 150.00.

## Errors
Errors use the RFC 9457 problem details format with a status and a human-readable detail. Common statuses:
400 invalid input, 401 invalid API key, 404 not found, 409 concurrent update, 422 not allowed in the current state,
503 a downstream service is unavailable.
