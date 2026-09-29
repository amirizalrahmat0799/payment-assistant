package com.pgs.assistant.tools;

/**
 * Keys for values passed to tools through Spring AI's ToolContext. The ToolContext is never
 * sent to the model, so the merchant's API key stays server-side.
 */
public final class ToolContextKeys {

    public static final String API_KEY = "apiKey";
    public static final String MERCHANT_ID = "merchantId";
    /** A mutable list the tools append their names to, so the API can report which tools ran. */
    public static final String TOOL_LOG = "toolLog";

    private ToolContextKeys() {
    }
}
