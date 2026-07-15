package com.cnpc.promoretail.common.api;

import com.cnpc.promoretail.checkout.CheckoutCalculationNotFoundException;
import com.cnpc.promoretail.checkout.CheckoutCalculationAlreadyConfirmedException;
import com.cnpc.promoretail.checkout.CheckoutCandidateNotFoundException;
import com.cnpc.promoretail.checkout.CheckoutConfirmationNotFoundException;
import com.cnpc.promoretail.checkout.CheckoutCouponException;
import com.cnpc.promoretail.checkout.CheckoutTransactionAlreadyExistsException;
import com.cnpc.promoretail.checkout.CheckoutTransactionNotFoundException;
import com.cnpc.promoretail.inventory.InventoryAlertNotFoundException;
import com.cnpc.promoretail.member.MemberAlreadyExistsException;
import com.cnpc.promoretail.member.MemberNotFoundException;
import com.cnpc.promoretail.product.ProductNotFoundException;
import com.cnpc.promoretail.promotion.benefitpackage.BenefitPackageNotFoundException;
import com.cnpc.promoretail.replenishment.ReplenishmentListNotFoundException;
import com.cnpc.promoretail.station.StationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> productNotFound(ProductNotFoundException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(MemberNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> memberNotFound(MemberNotFoundException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(BenefitPackageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> benefitPackageNotFound(BenefitPackageNotFoundException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(StationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> stationNotFound(StationNotFoundException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(MemberAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> memberAlreadyExists(MemberAlreadyExistsException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(ReplenishmentListNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> replenishmentListNotFound(ReplenishmentListNotFoundException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(InventoryAlertNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> inventoryAlertNotFound(InventoryAlertNotFoundException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler({
            CheckoutCalculationNotFoundException.class,
            CheckoutConfirmationNotFoundException.class,
            CheckoutTransactionNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> checkoutRecordNotFound(RuntimeException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(CheckoutCandidateNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> checkoutCandidateNotFound(CheckoutCandidateNotFoundException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler({
            CheckoutCalculationAlreadyConfirmedException.class,
            CheckoutTransactionAlreadyExistsException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> checkoutAlreadyConfirmed(RuntimeException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(CheckoutCouponException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> checkoutCouponConflict(CheckoutCouponException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalidRequestBody(HttpMessageNotReadableException exception) {
        return ApiResponse.fail("请求体格式错误：" + exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> typeMismatch(MethodArgumentTypeMismatchException exception) {
        return ApiResponse.fail("参数类型不匹配：" + exception.getName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> illegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> unexpectedException(Exception exception) {
        log.error("未处理的异常", exception);
        return ApiResponse.fail("服务器内部错误，请稍后重试或联系技术支持");
    }
}
