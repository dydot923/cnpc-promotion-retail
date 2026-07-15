package com.cnpc.promoretail.common.persistence;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev-db", "postgres"})
@MapperScan({
        "com.cnpc.promoretail.audit.persistence.mapper",
        "com.cnpc.promoretail.promotion.persistence.mapper",
        "com.cnpc.promoretail.promotion.bundle.persistence",
        "com.cnpc.promoretail.promotion.benefitpackage.persistence",
        "com.cnpc.promoretail.promotion.coupon.persistence",
        "com.cnpc.promoretail.promotion.excludedcategory.persistence",
        "com.cnpc.promoretail.promotion.points.persistence",
        "com.cnpc.promoretail.promotion.productgroup.persistence",
        "com.cnpc.promoretail.checkout.persistence.mapper",
        "com.cnpc.promoretail.importcenter.persistence.mapper",
        "com.cnpc.promoretail.member.persistence",
        "com.cnpc.promoretail.product.persistence.mapper",
        "com.cnpc.promoretail.inventory.persistence.mapper",
        "com.cnpc.promoretail.replenishment.persistence.mapper",
        "com.cnpc.promoretail.station.persistence",
        "com.cnpc.promoretail.ruleengine.datetrigger.persistence"
})
public class PersistenceConfiguration {

    public PersistenceConfiguration(ObjectMapper objectMapper) {
        JacksonTypeHandler.setObjectMapper(objectMapper.copy().findAndRegisterModules());
    }
}
