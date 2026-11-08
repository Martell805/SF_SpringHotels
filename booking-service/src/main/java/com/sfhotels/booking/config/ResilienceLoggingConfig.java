package com.sfhotels.booking.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ResilienceLoggingConfig {

    @Bean
    public RegistryEventConsumer<Retry> retryEventLogger() {
        return new AddedEntryConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> event) {
                event.getAddedEntry().getEventPublisher().onRetry(retry -> log.warn(
                        "Remote call '{}' retry attempt #{} in {} ms after error: {}", retry.getName(),
                        retry.getNumberOfRetryAttempts(), retry.getWaitInterval().toMillis(),
                        String.valueOf(retry.getLastThrowable())));
            }
        };
    }

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventLogger() {
        return new AddedEntryConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                event.getAddedEntry().getEventPublisher().onStateTransition(transition -> log.warn(
                        "Circuit breaker '{}' state {}", transition.getCircuitBreakerName(),
                        transition.getStateTransition()));
            }
        };
    }

    private interface AddedEntryConsumer<E> extends RegistryEventConsumer<E> {

        @Override
        default void onEntryRemovedEvent(EntryRemovedEvent<E> event) {
        }

        @Override
        default void onEntryReplacedEvent(EntryReplacedEvent<E> event) {
        }
    }
}
