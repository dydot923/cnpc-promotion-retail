package com.cnpc.promoretail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

@SpringBootApplication
public class PromotionRetailApplication {

    public static void main(String[] args) {
        if (DesktopApplicationLifecycle.openRunningInstance()) {
            return;
        }

        try {
            DesktopEmbeddedPostgres.configure();
            ConfigurableApplicationContext context = SpringApplication.run(PromotionRetailApplication.class, args);
            context.addApplicationListener((ContextClosedEvent event) -> DesktopEmbeddedPostgres.stop());
            DesktopApplicationLifecycle.start(context);
        } catch (RuntimeException exception) {
            DesktopEmbeddedPostgres.stop();
            throw exception;
        }
    }
}
