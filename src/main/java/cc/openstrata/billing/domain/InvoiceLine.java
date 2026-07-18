package cc.openstrata.billing.domain;

/** A line item in an {@link Invoice} (DESIGN §3). */
public class InvoiceLine {

    private final String lineId;
    private final UsageDimension dimension;
    private final long quantity;
    private final long unitPrice; // cents
    private final long subtotal;  // cents

    public InvoiceLine(String lineId, UsageDimension dimension, long quantity,
                       long unitPrice, long subtotal) {
        this.lineId = lineId;
        this.dimension = dimension;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public String lineId() { return lineId; }
    public UsageDimension dimension() { return dimension; }
    public long quantity() { return quantity; }
    public long unitPrice() { return unitPrice; }
    public long subtotal() { return subtotal; }
}
