# Fees and settlement

## Processing fee
Each merchant has a processing fee in basis points (bps), agreed at onboarding. 100 bps = 1%, so 250 bps = 2.50%.
The fee is charged on every captured amount and rounded half-up to the nearest sen. Example: capturing RM 10.20 at
250 bps costs RM 0.26 (25.5 sen rounds up to 26 sen). Declined, authorized-only and voided payments have no fee.

## Refunds and fees
Refunds do not return the processing fee on the original capture, which is common acquirer practice.

## How settlement works
Every capture and refund is written to the merchant's ledger. Once a day a settlement job pays out everything that
is not yet settled: net payout = captured - refunded - fees. One settlement is created per merchant and currency.

## Settlement timing
The nightly settlement runs at 00:05 UTC (08:05 in Malaysia) and settles all activity up to the end of the
previous UTC day. Activity after midnight UTC is included in the next day's settlement.
The Settlements page shows the pending payout (not yet settled) and the history of past settlements.

## Negative settlements
If refunds on a day are larger than new captures, the settlement's net amount is negative. It is shown in
red on the Settlements page.

## Settling twice
Each date can only be settled once. Running settlement again for the same date is rejected, so a merchant is
never paid twice for the same activity.
