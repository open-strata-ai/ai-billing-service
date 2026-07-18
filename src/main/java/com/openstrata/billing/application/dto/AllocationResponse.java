package com.openstrata.billing.application.dto;

/** A department/cost-center Showback or Chargeback line of an invoice (DESIGN §3/§5). */
public record AllocationResponse(String allocationId, String costCenter, long amountCents) {}
