package org.spectrum.sqlchecker.domain.shared.event;

import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

/**
 * 领域事件基类
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
public abstract class DomainEvent {

    /**
     * 事件 ID
     */
    private final String eventId = UUID.randomUUID().toString();

    /**
     * 事件发生时间
     */
    private final Instant occurredAt;

    protected DomainEvent(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    protected DomainEvent() {
        this(Instant.now());
    }
}
