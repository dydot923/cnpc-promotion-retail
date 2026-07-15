package com.cnpc.promoretail.promotion.productgroup;

import com.cnpc.promoretail.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-groups")
public class ProductGroupController {

    private final ProductGroupService productGroupService;

    public ProductGroupController(ProductGroupService productGroupService) {
        this.productGroupService = productGroupService;
    }

    @GetMapping
    public ApiResponse<List<ProductGroupMapping>> list() {
        return ApiResponse.ok(productGroupService.findAll());
    }

    @GetMapping("/{groupId}")
    public ApiResponse<ProductGroupMapping> get(@PathVariable String groupId) {
        return productGroupService.findByGroupId(groupId)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("商品组不存在"));
    }
}
