package cc.openstrata.billing.domain;

/** Invoice lifecycle (DESIGN §3). Immutable after FINALIZED (ADR-6). */
public enum InvoiceStatus {
    DRAFT,
    FINALIZED,
    SENT;

    public boolean isFinalized() {
        return this == FINALIZED || this == SENT;
    }
}
