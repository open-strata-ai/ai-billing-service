package cc.openstrata.billing.domain;

/** Metering usage dimension (DESIGN §3 / SPECS §2.4). */
public enum UsageDimension {
    TOKEN_INPUT,
    TOKEN_OUTPUT,
    GPU_HOUR,
    VECTOR_COUNT,
    API_CALL,
    AGENT_RUN;

    /** Case-insensitive lookup from the wire/proto string (e.g. "token_input"). */
    public static UsageDimension from(String s) {
        if (s == null) {
            throw new IllegalArgumentException("null dimension");
        }
        return UsageDimension.valueOf(s.trim().toUpperCase());
    }
}
