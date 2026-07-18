package cc.openstrata.billing.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLineEntity, String> {
    List<InvoiceLineEntity> findByInvoiceInvoiceId(String invoiceId);
}
