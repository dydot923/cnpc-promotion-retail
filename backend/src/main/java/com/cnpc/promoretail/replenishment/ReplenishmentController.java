package com.cnpc.promoretail.replenishment;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.replenishment.model.ReplenishmentList;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replenishment/lists")
public class ReplenishmentController {

    private final ReplenishmentService replenishmentService;

    public ReplenishmentController(ReplenishmentService replenishmentService) {
        this.replenishmentService = replenishmentService;
    }

    @PostMapping
    public ApiResponse<ReplenishmentList> create() {
        return ApiResponse.ok(replenishmentService.createFromCurrentAlerts());
    }

    @GetMapping("/{id}")
    public ApiResponse<ReplenishmentList> get(@PathVariable String id) {
        return ApiResponse.ok(replenishmentService.get(id));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable String id) {
        byte[] csv = replenishmentService.exportCsv(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(id + ".csv").build().toString())
                .body(csv);
    }
}
