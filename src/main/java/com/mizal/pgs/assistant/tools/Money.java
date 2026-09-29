package com.mizal.pgs.assistant.tools;

import java.math.BigDecimal;

/**
 * Tool results carry pre-formatted amounts ("RM 150.00") next to the raw minor units, so the
 * model never has to do currency arithmetic itself (small models are unreliable at it).
 */
public final class Money {

    private Money() {
    }

    public static String format(long minor, String currency) {
        String symbol = "MYR".equals(currency) ? "RM" : currency;
        return symbol + " " + BigDecimal.valueOf(minor, 2).toPlainString();
    }
}
