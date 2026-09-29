package com.mizal.pgs.assistant.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void formatsMinorUnitsWithCurrencySymbol() {
        assertThat(Money.format(15_000, "MYR")).isEqualTo("RM 150.00");
        assertThat(Money.format(5, "MYR")).isEqualTo("RM 0.05");
        assertThat(Money.format(-4_025, "MYR")).isEqualTo("RM -40.25");
        assertThat(Money.format(1_999, "USD")).isEqualTo("USD 19.99");
    }
}
