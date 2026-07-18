package cc.openstrata.billing.domain;

import cc.openstrata.billing.web.ErrorCode;

/** Thrown for business-rule violations; rendered by {@link cc.openstrata.billing.web.GlobalExceptionHandler}. */
public class DomainException extends RuntimeException {

    private final ErrorCode code;

    public DomainException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
