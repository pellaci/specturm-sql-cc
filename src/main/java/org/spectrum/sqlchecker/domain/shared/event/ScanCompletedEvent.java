package org.spectrum.sqlchecker.domain.shared.event;

import lombok.Getter;

/**
 * 扫描完成事件
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
public class ScanCompletedEvent extends DomainEvent {

    private final String scanId;
    private final int sqlCount;
    private final int issueCount;

    public ScanCompletedEvent(String scanId, int sqlCount) {
        super();
        this.scanId = scanId;
        this.sqlCount = sqlCount;
        this.issueCount = 0;
    }

    public ScanCompletedEvent(String scanId, int sqlCount, int issueCount) {
        super();
        this.scanId = scanId;
        this.sqlCount = sqlCount;
        this.issueCount = issueCount;
    }
}
