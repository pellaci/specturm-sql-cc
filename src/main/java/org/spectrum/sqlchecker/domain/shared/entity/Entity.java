package org.spectrum.sqlchecker.domain.shared.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.domain.AbstractAggregateRoot;
import java.util.UUID;

/**
 * 实体基类
 *
 * @param <T> 实体类型
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode(of = "id")
public abstract class Entity<T> {

    /**
     * 唯一标识
     */
    protected final String id;

    /**
     * 版本号（乐观锁）
     */
    protected Long version;

    protected Entity() {
        this.id = UUID.randomUUID().toString();
    }

    protected Entity(String id) {
        this.id = id;
    }

    protected Entity(String id, Long version) {
        this.id = id;
        this.version = version;
    }
}
