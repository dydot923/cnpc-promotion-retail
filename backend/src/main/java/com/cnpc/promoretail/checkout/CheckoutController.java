package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutApplicationService checkoutApplicationService;
    private final CheckoutExchangeOfferService checkoutExchangeOfferService;

    public CheckoutController(
            CheckoutApplicationService checkoutApplicationService,
            CheckoutExchangeOfferService checkoutExchangeOfferService
    ) {
        this.checkoutApplicationService = checkoutApplicationService;
        this.checkoutExchangeOfferService = checkoutExchangeOfferService;
    }

    @GetMapping("/capabilities")
    public ApiResponse<CheckoutCapabilities> capabilities() {
        return ApiResponse.ok(new CheckoutCapabilities(
                "cnpc-promotion-retail",
                "checkout-v2",
                true,
                true
        ));
    }

    @PostMapping("/calculate")
    public ApiResponse<CheckoutCalculateResponse> calculate(@Valid @RequestBody CheckoutCalculateRequest request) {
        return ApiResponse.ok(checkoutApplicationService.calculate(request));
    }

    @GetMapping("/exchange-offers")
    public ApiResponse<List<CheckoutExchangeOfferResponse>> exchangeOffers(
            @RequestParam(defaultValue = "GASOLINE") FuelType fuelType,
            @RequestParam(defaultValue = "0") BigDecimal fuelAmount,
            @RequestParam(required = false) LocalDate businessDate,
            @RequestParam(defaultValue = "gas_station") String stationType,
            @RequestParam(defaultValue = "新疆") String stationProvince
    ) {
        return ApiResponse.ok(checkoutExchangeOfferService.findOffers(
                fuelType, fuelAmount, businessDate, stationType, stationProvince));
    }

    @PostMapping("/confirm")
    public ApiResponse<CheckoutConfirmationResponse> confirm(@Valid @RequestBody CheckoutConfirmRequest request) {
        return ApiResponse.ok(checkoutApplicationService.confirm(request));
    }

    @GetMapping("/confirmations/{confirmationId}")
    public ApiResponse<CheckoutConfirmationResponse> confirmation(@PathVariable String confirmationId) {
        return ApiResponse.ok(checkoutApplicationService.getConfirmation(confirmationId));
    }

    @GetMapping("/confirmations")
    public ApiResponse<List<CheckoutConfirmationResponse>> confirmationsByCalculationId(
            @RequestParam String calculationId
    ) {
        return ApiResponse.ok(checkoutApplicationService.findConfirmationsByCalculationId(calculationId));
    }

    @GetMapping("/transactions/{txnNo}")
    public ApiResponse<CheckoutTransactionResponse> transaction(@PathVariable String txnNo) {
        return ApiResponse.ok(checkoutApplicationService.getTransaction(txnNo));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<CheckoutTransactionResponse>> transactions(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(checkoutApplicationService.findRecentTransactions(limit));
    }

    @GetMapping("/records/{txnNo}")
    public ApiResponse<CheckoutTransactionResponse> checkoutRecord(@PathVariable String txnNo) {
        return ApiResponse.ok(checkoutApplicationService.getTransaction(txnNo));
    }

    @GetMapping("/records")
    public ApiResponse<List<CheckoutTransactionResponse>> checkoutRecords(
            @RequestParam(required = false) String memberCode,
            @RequestParam(required = false) String stationCode,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(checkoutApplicationService.findTransactions(
                new CheckoutTransactionQuery(memberCode, stationCode, startDate, endDate, limit)));
    }

    public record CheckoutCapabilities(
            String service,
            String apiVersion,
            boolean calculate,
            boolean confirm
    ) {
    }
}
