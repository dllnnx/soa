package ru.itmo.oscars.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import ru.itmo.oscars.model.OperationCommand;
import ru.itmo.oscars.service.JobService;

@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(JobService jobService) {
        ConsumerRecordRecoverer recoverer = (record, exception) -> {
            if (record.value() instanceof OperationCommand command) {
                jobService.fail(command.jobId(), "Превышено число попыток: " + exception.getMessage());
            } else {
                log.warn("Unrecoverable record at partition {} offset {}: {}",
                        record.partition(), record.offset(), exception.getMessage(), exception);
            }
        };
        var backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(6);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }
}
