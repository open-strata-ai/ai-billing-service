package com.openstrata.billing.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {
    List<InvoiceEntity> findByTenantId(String tenantId);
    Optional<InvoiceEntity> findByTenantIdAndInvoiceId(String tenantId, String invoiceId);
}
