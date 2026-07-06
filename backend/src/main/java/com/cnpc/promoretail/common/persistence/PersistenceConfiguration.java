package com.cnpc.promoretail.common.persistence;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"dev-db", "postgres"})
@MapperScan({
        "com.cnpc.promoretail.promotion.persistence.mapper",
        "com.cnpc.promoretail.checkout.persistence.mapper",
        "com.cnpc.promoretail.importcenter.persistence.mapper"
})
public class PersistenceConfiguration {

    public PersistenceConfiguration(ObjectMapper objectMapper) {
        JacksonTypeHandler.setObjectMapper(objectMapper.copy().findAndRegisterModules());
    }
}
