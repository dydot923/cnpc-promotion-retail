package com.cnpc.promoretail.replenishment;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.inventory.InventoryAlertService;
import com.cnpc.promoretail.inventory.model.InventoryAlert;
import com.cnpc.promoretail.replenishment.model.ReplenishmentItem;
import com.cnpc.promoretail.replenishment.model.ReplenishmentList;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.cnpc.promoretail.replenishment.repository.InMemoryReplenishmentListRepository;
import com.cnpc.promoretail.replenishment.repository.ReplenishmentListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReplenishmentService {

    private final InventoryAlertService inventoryAlertService;
    private final ReplenishmentListRepository replenishmentListRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public ReplenishmentService(
            InventoryAlertService inventoryAlertService,
            ReplenishmentListRepository replenishmentListRepository,
            AuditLogService auditLogService
    ) {
        this.inventoryAlertService = inventoryAlertService;
        this.replenishmentListRepository = replenishmentListRepository;
        this.auditLogService = auditLogService;
    }

    public ReplenishmentService(InventoryAlertService inventoryAlertService) {
        this.inventoryAlertService = inventoryAlertService;
        this.replenishmentListRepository = new InMemoryReplenishmentListRepository();
        this.auditLogService = AuditLogService.noop();
    }

    public ReplenishmentList createFromCurrentAlerts() {
        return createFromCurrentAlerts("system");
    }

    public ReplenishmentList createFromCurrentAlerts(String operatorId) {
        List<InventoryAlert> openAlerts = inventoryAlertService.openAlerts();
        List<ReplenishmentItem> items = openAlerts.stream()
                .map(this::toReplenishmentItem)
                .toList();
        Instant now = Instant.now();
        ReplenishmentList list = new ReplenishmentList(
                "repl-" + UUID.randomUUID(),
                "replenishment_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE),
                "DRAFT",
                items,
                items.size(),
                operatorId,
                now,
                operatorId,
                now
        );
        ReplenishmentList saved = replenishmentListRepository.save(list);
        inventoryAlertService.linkReplenishmentList(openAlerts, saved.listId(), operatorId);
        auditLogService.record("REPLENISHMENT_GENERATE", "REPLENISHMENT_LIST", saved.listId(),
                null, saved, operatorId, "", "Generate replenishment list from inventory alerts");
        return saved;
    }

    public ReplenishmentList get(String listId) {
        return replenishmentListRepository.findByListId(listId)
                .orElseThrow(() -> new ReplenishmentListNotFoundException(listId));
    }

    public byte[] exportCsv(String listId) {
        return exportCsv(listId, "system");
    }

    public byte[] exportCsv(String listId, String operatorId) {
        ReplenishmentList list = get(listId);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("productCode,barcode,productName,category,currentQuantity,threshold,suggestedQuantity,")
                .append("relatedPromotion,reason\n");
        for (ReplenishmentItem item : list.items()) {
            csv.append(escape(item.productCode())).append(',')
                    .append(escape(item.barcode())).append(',')
                    .append(escape(item.productName())).append(',')
                    .append(escape(item.category())).append(',')
                    .append(item.currentQuantity()).append(',')
                    .append(item.threshold()).append(',')
                    .append(item.suggestedQuantity()).append(',')
                    .append(escape(item.relatedPromotion())).append(',')
                    .append(escape(item.reason())).append('\n');
        }
        ReplenishmentList exported = list.withStatus("EXPORTED", operatorId, Instant.now());
        replenishmentListRepository.save(exported);
        auditLogService.record("REPLENISHMENT_EXPORT", "REPLENISHMENT_LIST", exported.listId(),
                list, exported, operatorId, "", "Export replenishment list as CSV");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private ReplenishmentItem toReplenishmentItem(InventoryAlert alert) {
        return new ReplenishmentItem(
                alert.productCode(),
                alert.barcode(),
                alert.productName(),
                alert.category(),
                alert.currentQuantity(),
                alert.threshold(),
                alert.suggestedReplenishmentQuantity(),
                alert.relatedRuleId() + " / " + alert.relatedRuleType(),
                alert.reason()
        );
    }

    private String escape(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
