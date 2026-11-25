package com.qykj.finmaster.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("\n" +
                "╔════════════════════════════════════════════════════════════╗\n" +
                "║                                                             ║\n" +
                "║                FinMaster 系统启动成功！                      ║\n" +
                "║                                                             ║\n" +
                "╚════════════════════════════════════════════════════════════╝\n");
    }
} 