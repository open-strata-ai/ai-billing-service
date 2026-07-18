package com.openstrata.billing.web;

/**
 * Business error codes (SPECS §1.5). Each maps to a single HTTP status so the
 * {@link GlobalExceptionHandler} can render a consistent error envelope.
 */
public enum ErrorCode {
    BILLING_REQUIRES_MULTITENANCY(422),
    INVOICE_NOT_FOUND(404),
    INVOICE_ALREADY_FINALIZED(409),
    BUDGET_NOT_FOUND(404),
    BUDGET_EXCEEDED(409),
    METERING_FORMAT_INVALID(400),
    PRICE_RULE_NOT_FOUND(404),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    INTERNAL(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
