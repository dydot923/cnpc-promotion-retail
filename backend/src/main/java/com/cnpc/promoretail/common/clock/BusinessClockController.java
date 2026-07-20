package com.cnpc.promoretail.common.clock;

import com.cnpc.promoretail.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/business-clock")
public class BusinessClockController {

    private final BusinessClockService businessClockService;

    public BusinessClockController(BusinessClockService businessClockService) {
        this.businessClockService = businessClockService;
    }

    @GetMapping
    public ApiResponse<BusinessClockService.BusinessClockState> current() {
        return ApiResponse.ok(businessClockService.current());
    }

    @PutMapping
    public ApiResponse<BusinessClockService.BusinessClockState> update(
            @Valid @RequestBody UpdateBusinessClockRequest request
    ) {
        return ApiResponse.ok(businessClockService.setBusinessDate(request.businessDate()));
    }

    @DeleteMapping
    public ApiResponse<BusinessClockService.BusinessClockState> reset() {
        return ApiResponse.ok(businessClockService.reset());
    }

    public record UpdateBusinessClockRequest(@NotNull LocalDate businessDate) {
    }
}
