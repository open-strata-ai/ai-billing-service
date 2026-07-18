package com.openstrata.billing.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllocationRepository extends JpaRepository<AllocationEntity, String> {
    List<AllocationEntity> findByInvoiceInvoiceId(String invoiceId);
}
