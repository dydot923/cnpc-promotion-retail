package com.cnpc.promoretail.checkout.repository;

import com.cnpc.promoretail.checkout.model.CheckoutCalculationRecord;
import java.util.List;
import java.util.Optional;

public interface CheckoutCalculationRecordRepository {

    CheckoutCalculationRecord save(CheckoutCalculationRecord record);

    Optional<CheckoutCalculationRecord> findByCalculationId(String calculationId);

    List<CheckoutCalculationRecord> findAll();
}
