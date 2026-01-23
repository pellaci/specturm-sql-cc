package org.spectrum.sqlchecker.domain.shared.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 领域事件发布器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    /**
     * 发布领域事件
     *
     * @param event 事件
     */
    public void publish(DomainEvent event) {
        log.debug("发布领域事件: {}", event.getClass().getSimpleName());
        publisher.publishEvent(event);
    }
}
