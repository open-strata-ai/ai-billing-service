package cc.openstrata.billing.application.dto;

/** A line item of an invoice (DESIGN §3). */
public record InvoiceLineResponse(String lineId, String dimension, long quantity, long unitPrice, long subtotal) {}
