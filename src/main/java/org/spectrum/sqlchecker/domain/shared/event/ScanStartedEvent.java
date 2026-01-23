package org.spectrum.sqlchecker.domain.shared.event;

import lombok.Getter;

/**
 * 扫描开始事件
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
public class ScanStartedEvent extends DomainEvent {

    private final String scanId;
    private final String repositoryPath;

    public ScanStartedEvent(String scanId, String repositoryPath) {
        super();
        this.scanId = scanId;
        this.repositoryPath = repositoryPath;
    }
}
