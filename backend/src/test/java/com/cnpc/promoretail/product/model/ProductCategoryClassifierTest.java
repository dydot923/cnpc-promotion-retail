package com.cnpc.promoretail.product.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductCategoryClassifierTest {

    @Test
    void classifiesRealPromotionCategoriesFromCatalogNames() {
        assertThat(ProductCategoryClassifier.resolve("中华 (软) 20支", null)).isEqualTo("香烟");
        assertThat(ProductCategoryClassifier.resolve("云烟 冬虫夏草和润 5MG", null)).isEqualTo("香烟");
        assertThat(ProductCategoryClassifier.resolve("乌苏 小麦白罐装啤酒 500ML", null)).isEqualTo("啤酒");
        assertThat(ProductCategoryClassifier.resolve("星巴克 星选美式咖啡饮料", null)).isEqualTo("咖啡");
        assertThat(ProductCategoryClassifier.resolve("新疆老月饼礼盒", null)).isEqualTo("月饼礼盒");
        assertThat(ProductCategoryClassifier.resolve("昆仑 尿素（大颗粒）", null)).isEqualTo("化肥");
    }

    @Test
    void preservesExplicitCategoryAndRejectsTwentyStickFoodPackaging() {
        assertThat(ProductCategoryClassifier.resolve("任意商品", "日用品")).isEqualTo("日用品");
        assertThat(ProductCategoryClassifier.resolve("独库情 十二红枣饮 20G*20支", null)).isNull();
        assertThat(ProductCategoryClassifier.resolve("中华 健齿白牙膏 155G", null)).isNull();
    }
}
