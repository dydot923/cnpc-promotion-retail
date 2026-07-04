package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutApplicationService checkoutApplicationService;

    public CheckoutController(CheckoutApplicationService checkoutApplicationService) {
        this.checkoutApplicationService = checkoutApplicationService;
    }

    @PostMapping("/calculate")
    public ApiResponse<CalculationResult> calculate(@Valid @RequestBody CheckoutCalculateRequest request) {
        return ApiResponse.ok(checkoutApplicationService.calculate(request));
    }

    @PostMapping("/confirm")
    public ApiResponse<String> confirm(@Valid @RequestBody CheckoutConfirmRequest request) {
        return ApiResponse.ok(checkoutApplicationService.confirm(request));
    }
}

