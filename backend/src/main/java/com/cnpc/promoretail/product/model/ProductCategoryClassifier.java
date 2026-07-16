package com.cnpc.promoretail.product.model;

import java.util.regex.Pattern;

public final class ProductCategoryClassifier {

    private static final Pattern CIGARETTE_STRENGTH = Pattern.compile("\\d+(?:\\.\\d+)?\\s*mg", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_CIGARETTE_TWENTY_STICKS = Pattern.compile("红枣饮|棉签|牙签|吸管|火腿|铅笔|中性笔");

    private ProductCategoryClassifier() {
    }

    public static String resolve(String productName, String category) {
        if (category != null && !category.isBlank()) {
            return category.trim();
        }
        String name = productName == null ? "" : productName.trim();
        if (isCigarette(name)) {
            return "香烟";
        }
        if (containsAny(name, "化肥", "尿素", "复合肥", "磷酸二铵", "钾肥")) {
            return "化肥";
        }
        if (name.contains("啤酒")) {
            return "啤酒";
        }
        if (name.contains("咖啡")) {
            return "咖啡";
        }
        if (name.contains("月饼")) {
            return "月饼礼盒";
        }
        if (name.contains("瓜子")) {
            return "瓜子";
        }
        if (containsAny(name, "雪糕", "冰淇淋", "冰激凌")) {
            return "雪糕";
        }
        if (containsAny(name, "肉脯", "肉干")) {
            return "肉脯";
        }
        if (containsAny(name, "薯片", "虾条", "锅巴", "爆米花", "好多鱼", "雪饼", "仙贝")) {
            return "膨化";
        }
        return null;
    }

    static boolean isCigarette(String productName) {
        return productName.contains("香烟")
                || CIGARETTE_STRENGTH.matcher(productName).find()
                || (productName.contains("20支") && !NON_CIGARETTE_TWENTY_STICKS.matcher(productName).find());
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
