package org.spectrum.sqlchecker.domain.shared.aggregate;

import lombok.Getter;
import org.springframework.data.domain.AbstractAggregateRoot;
import java.time.Instant;
import java.util.UUID;

/**
 * 聚合根基类
 *
 * @param <T> 聚合根类型
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
public abstract class AggregateRoot<T extends AggregateRoot<T>> extends AbstractAggregateRoot<T> {

    /**
     * 唯一标识
     */
    private String id = UUID.randomUUID().toString();

    /**
     * 创建时间
     */
    private Instant createdAt = Instant.now();

    /**
     * 更新时间
     */
    private Instant updatedAt = Instant.now();

    /**
     * 版本号（乐观锁）
     */
    private Long version;

    /**
     * 标记为更新
     */
    public void markAsUpdated() {
        this.updatedAt = Instant.now();
    }
}
