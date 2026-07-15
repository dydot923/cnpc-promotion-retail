package com.cnpc.promoretail.inventory;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.inventory.model.InventoryAlert;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryAlertController {

    private final InventoryAlertService inventoryAlertService;

    public InventoryAlertController(InventoryAlertService inventoryAlertService) {
        this.inventoryAlertService = inventoryAlertService;
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
