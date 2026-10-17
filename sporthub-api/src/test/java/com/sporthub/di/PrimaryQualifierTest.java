package com.sporthub.di;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimaryQualifierTest {

    interface MessageSender {
        String channel();
    }

    static class EmailSender implements MessageSender {
        @Override
        public String channel() {
            return "EMAIL";
        }
    }

    static class SmsSender implements MessageSender {
        @Override
        public String channel() {
            return "SMS";
        }
    }

    record MessageService(
            MessageSender defaultSender,
            MessageSender selectedSender) {
    }

    @Configuration(proxyBeanMethods = false)
    static class DemoConfig {

        @Bean
        @Primary
        MessageSender emailSender() {
            return new EmailSender();
        }

        @Bean
        MessageSender smsSender() {
            return new SmsSender();
        }

        @Bean
        MessageService messageService(
                MessageSender defaultSender,
                @Qualifier("smsSender")
                MessageSender selectedSender) {

            return new MessageService(
                    defaultSender,
                    selectedSender
            );
        }
    }

    @Test
    void primarySelectsDefaultBean() {
        try (var context =
                     new AnnotationConfigApplicationContext(DemoConfig.class)) {

            MessageService service =
                    context.getBean(MessageService.class);

            assertEquals("EMAIL", service.defaultSender().channel());
        }
    }

    @Test
    void qualifierSelectsNamedBeanInsteadOfPrimary() {
        try (var context =
                     new AnnotationConfigApplicationContext(DemoConfig.class)) {

            MessageService service =
                    context.getBean(MessageService.class);

            assertEquals("SMS", service.selectedSender().channel());
        }
    }
}
