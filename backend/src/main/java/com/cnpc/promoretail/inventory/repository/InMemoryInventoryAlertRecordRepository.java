package com.cnpc.promoretail.inventory.repository;

import com.cnpc.promoretail.inventory.model.InventoryAlertRecord;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryInventoryAlertRecordRepository implements InventoryAlertRecordRepository {

    private final ConcurrentMap<String, InventoryAlertRecord> records = new ConcurrentHashMap<>();

    @Override
    public InventoryAlertRecord save(InventoryAlertRecord record) {
        records.put(record.alertId(), record);
        return record;
    }

    @Override
    public Optional<InventoryAlertRecord> findByAlertId(String alertId) {
        return Optional.ofNullable(records.get(alertId));
    }

    @Override
    public List<InventoryAlertRecord> findByAlertIds(List<String> alertIds) {
        if (alertIds == null || alertIds.isEmpty()) {
            return List.of();
        }
        return alertIds.stream()
                .map(records::get)
                .filter(record -> record != null)
                .toList();
    }
}
