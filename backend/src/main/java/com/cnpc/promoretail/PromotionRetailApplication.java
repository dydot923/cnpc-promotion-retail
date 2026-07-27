package com.cnpc.promoretail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class PromotionRetailApplication {

    public static void main(String[] args) {
        if (DesktopApplicationLifecycle.openRunningInstance()) {
            return;
        }

        ConfigurableApplicationContext context = SpringApplication.run(PromotionRetailApplication.class, args);
        DesktopApplicationLifecycle.start(context);
    }
}
