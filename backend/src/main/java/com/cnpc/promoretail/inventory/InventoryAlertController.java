package com.cnpc.promoretail.inventory;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.inventory.model.InventoryAlert;
import com.cnpc.promoretail.inventory.model.InventoryItem;
import com.cnpc.promoretail.inventory.model.InventoryReplenishmentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryAlertController {

    private final InventoryAlertService inventoryAlertService;
    private final InventoryManagementService inventoryManagementService;

    public InventoryAlertController(
            InventoryAlertService inventoryAlertService,
            InventoryManagementService inventoryManagementService
    ) {
        this.inventoryAlertService = inventoryAlertService;
        this.inventoryManagementService = inventoryManagementService;
    }

    @GetMapping("/items")
    public ApiResponse<List<InventoryItem>> items(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String stockStatus
    ) {
        return ApiResponse.ok(inventoryManagementService.items(keyword, stockStatus));
    }

    @PostMapping("/items/{productCode}/replenish")
    public ApiResponse<InventoryReplenishmentResponse> replenish(
            @PathVariable String productCode,
            @Valid @RequestBody InventoryReplenishmentRequest request
    ) {
        return ApiResponse.ok(inventoryManagementService.replenish(productCode, request));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<InventoryAlert>> alerts() {
        return ApiResponse.ok(inventoryAlertService.alerts());
    }

    @PatchMapping("/alerts/{alertId}/handled")
    public ApiResponse<InventoryAlert> handle(
            @PathVariable String alertId,
            @RequestBody(required = false) InventoryAlertHandleRequest request
    ) {
        return ApiResponse.ok(inventoryAlertService.handle(
                alertId,
                request == null ? new InventoryAlertHandleRequest("system", "") : request
        ));
    }
}
