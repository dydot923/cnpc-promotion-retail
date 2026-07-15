package com.cnpc.promoretail.product;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    @GetMapping("/search")
    public ApiResponse<List<ProductCatalogItem>> search(@RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.ok(productQueryService.search(keyword));
    }

    @GetMapping("/by-barcode/{barcode}")
    public ApiResponse<ProductCatalogItem> byBarcode(@PathVariable String barcode) {
        return ApiResponse.ok(productQueryService.findByBarcode(barcode));
    }

    @GetMapping("/{productCode}")
    public ApiResponse<ProductCatalogItem> byProductCode(@PathVariable String productCode) {
        return ApiResponse.ok(productQueryService.findByProductCode(productCode));
    }
}
