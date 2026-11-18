package ru.itmo.oscars.config;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Component("messaging")
public class MessagingHealthIndicator extends AbstractHealthIndicator {
    private final AdminClient admin;
    private final KafkaListenerEndpointRegistry registry;

    public MessagingHealthIndicator(AdminClient admin, KafkaListenerEndpointRegistry registry) {
        this.admin = admin;
        this.registry = registry;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        var listener = registry.getListenerContainer("movie-jobs-worker");
        if (listener == null || !listener.isRunning()) {
            builder.down().withDetail("consumer", "not running");
            return;
        }
        var nodes = admin.describeCluster().nodes().get(2, TimeUnit.SECONDS);
        if (nodes.isEmpty()) builder.down();
        else builder.up();
    }

    @Configuration(proxyBeanMethods = false)
    static class AdminConfiguration {
        @Bean(destroyMethod = "close")
        AdminClient healthAdmin(KafkaAdmin kafkaAdmin) {
            var properties = new HashMap<String, Object>(kafkaAdmin.getConfigurationProperties());
            properties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 2000);
            properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
            return AdminClient.create(properties);
        }
    }
}
