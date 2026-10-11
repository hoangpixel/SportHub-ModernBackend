package com.sporthub.integration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeanLifecycleTest {

    @Test
    void callbacksRunWhenContextStartsAndCloses() {
        List<String> events = new ArrayList<>();

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {

            context.registerBean(
                    LifecycleProbe.class,
                    () -> new LifecycleProbe(events)
            );

            context.refresh();
            assertEquals(List.of("initialized"), events);

            LifecycleProbe bean = context.getBean(LifecycleProbe.class);
            bean.use();

            assertEquals(List.of("initialized", "used"), events);
        }

        assertEquals(
                List.of("initialized", "used", "destroyed"),
                events
        );

        System.out.println("Verified lifecycle order: " + events);
    }

    static class LifecycleProbe {

        private final List<String> events;

        LifecycleProbe(List<String> events) {
            this.events = events;
        }

        @PostConstruct
        public void initialize() {
            events.add("initialized");
            System.out.println("Lifecycle: initialized");
        }

        public void use() {
            events.add("used");
            System.out.println("Lifecycle: used");
        }

        @PreDestroy
        public void destroy() {
            events.add("destroyed");
            System.out.println("Lifecycle: destroyed");
        }
    }
}