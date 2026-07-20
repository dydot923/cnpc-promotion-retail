package com.cnpc.promoretail.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.inventory.model.InventoryReplenishmentResponse;
import com.cnpc.promoretail.support.PostgresIntegrationTestSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class InventoryWorkbookDatabaseTest extends PostgresIntegrationTestSupport {

    @Autowired
    private InventoryManagementService inventoryManagementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void inventoryWorkbookRowsAreImportedOneToOneAndQueryable() {
        Integer importedRows = jdbcTemplate.queryForObject("""
                select count(*)
                from inventory_snapshot
                where station_code = 'default'
                  and import_version = 'inventory-workbook-20260720'
                """, Integer.class);
        Integer uniqueProducts = jdbcTemplate.queryForObject("""
                select count(distinct product_code)
                from inventory_snapshot
                where station_code = 'default'
                  and import_version = 'inventory-workbook-20260720'
                """, Integer.class);
        Integer uniqueBarcodes = jdbcTemplate.queryForObject("""
                select count(distinct product.barcode)
                from inventory_snapshot snapshot
                join product using (product_code)
                where snapshot.station_code = 'default'
                  and snapshot.import_version = 'inventory-workbook-20260720'
                """, Integer.class);

        assertThat(importedRows).isEqualTo(454);
        assertThat(uniqueProducts).isEqualTo(454);
        assertThat(uniqueBarcodes).isEqualTo(454);
        assertThat(inventoryManagementService.items("70030041", ""))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.productName()).isEqualTo("黄山 金皖硬盒香烟(包) 13MG");
                    assertThat(item.barcode()).isEqualTo("6901028225106");
                    assertThat(item.currentQuantity()).isEqualByComparingTo("24.00");
                    assertThat(item.stockStatus()).isEqualTo("NORMAL");
                });
        assertThat(inventoryManagementService.items("70206433", "CRITICAL"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.currentQuantity()).isEqualByComparingTo("1.00");
                    assertThat(item.suggestedReplenishmentQuantity()).isEqualByComparingTo("19.00");
                });
    }

    @Test
    @Transactional
    void replenishmentCreatesLatestInventorySnapshotAndAuditRecord() {
        InventoryReplenishmentResponse response = inventoryManagementService.replenish(
                "70206433",
                new InventoryReplenishmentRequest(new BigDecimal("19"), "stock-manager", "验收入库")
        );

        assertThat(response.quantityBefore()).isEqualByComparingTo("1.00");
        assertThat(response.quantityAfter()).isEqualByComparingTo("20.00");
        assertThat(productCatalogRepository.findInventoryQuantity("70206433"))
                .hasValueSatisfying(quantity -> assertThat(quantity).isEqualByComparingTo("20.00"));
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from audit_log
                where action = 'INVENTORY_REPLENISH'
                  and target_type = 'PRODUCT_INVENTORY'
                  and target_id = '70206433'
                """, Integer.class)).isEqualTo(1);
    }
}
