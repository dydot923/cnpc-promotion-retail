package com.cnpc.promoretail.checkout.repository;

import com.cnpc.promoretail.checkout.model.CheckoutCalculationRecord;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryCheckoutCalculationRecordRepository implements CheckoutCalculationRecordRepository {

    private final ConcurrentMap<String, CheckoutCalculationRecord> records = new ConcurrentHashMap<>();

    @Override
    public CheckoutCalculationRecord save(CheckoutCalculationRecord record) {
        records.put(record.calculationId(), record);
        return record;
    }

    @Override
    public Optional<CheckoutCalculationRecord> findByCalculationId(String calculationId) {
        return Optional.ofNullable(records.get(calculationId));
    }

    @Override
    public List<CheckoutCalculationRecord> findAll() {
        return records.values().stream()
                .sorted(Comparator.comparing(CheckoutCalculationRecord::createdAt))
                .toList();
    }
}
