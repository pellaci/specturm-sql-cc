package org.spectrum.sqlchecker.domain.shared.event;

import lombok.Getter;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;
import org.spectrum.sqlchecker.domain.shared.valueobject.SqlHash;

/**
 * SQL 提取事件
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
public class SqlExtractedEvent extends DomainEvent {

    private final String scanId;
    private final String sqlId;
    private final SqlHash sqlHash;
    private final String filePath;
    private final int lineNumber;

    public SqlExtractedEvent(String scanId, String sqlId, SqlHash sqlHash, String filePath, int lineNumber) {
        super();
        this.scanId = scanId;
        this.sqlId = sqlId;
        this.sqlHash = sqlHash;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
    }
}
