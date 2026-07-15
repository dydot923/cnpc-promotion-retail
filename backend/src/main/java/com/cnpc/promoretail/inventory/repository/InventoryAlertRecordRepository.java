package com.cnpc.promoretail.inventory.repository;

import com.cnpc.promoretail.inventory.model.InventoryAlertRecord;
import java.util.List;
import java.util.Optional;

public interface InventoryAlertRecordRepository {

    InventoryAlertRecord save(InventoryAlertRecord record);

    Optional<InventoryAlertRecord> findByAlertId(String alertId);

    List<InventoryAlertRecord> findByAlertIds(List<String> alertIds);
}
