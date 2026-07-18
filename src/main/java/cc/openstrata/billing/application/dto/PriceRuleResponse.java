package cc.openstrata.billing.application.dto;

/** A price-table entry (SPECS §1.2 /price-rules). */
public record PriceRuleResponse(String dimension, long unitPriceCents, String currency) {}
