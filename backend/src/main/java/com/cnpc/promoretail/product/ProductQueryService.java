package com.cnpc.promoretail.product;

import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductQueryService {

    private static final int DEFAULT_SEARCH_LIMIT = 20;

    private final ProductCatalogRepository productCatalogRepository;

    public ProductQueryService(ProductCatalogRepository productCatalogRepository) {
        this.productCatalogRepository = productCatalogRepository;
    }

    public List<ProductCatalogItem> search(String keyword) {
        return productCatalogRepository.search(keyword, DEFAULT_SEARCH_LIMIT);
    }

    public ProductCatalogItem findByBarcode(String barcode) {
        return productCatalogRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ProductNotFoundException("未找到条码对应商品: " + barcode));
    }

    public ProductCatalogItem findByProductCode(String productCode) {
        return productCatalogRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException("未找到商品编码对应商品: " + productCode));
    }
}
