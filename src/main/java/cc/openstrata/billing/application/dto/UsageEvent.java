package cc.openstrata.billing.application.dto;

/** Wire shape for a single metering event in a {@code POST /usage} batch (SPECS §1.2). */
public record UsageEvent(
    String recordId,
    String tenantId,
    String appId,
    String model,
    String dimension, // UsageDimension name, e.g. TOKEN_INPUT
    long amount
) {}
