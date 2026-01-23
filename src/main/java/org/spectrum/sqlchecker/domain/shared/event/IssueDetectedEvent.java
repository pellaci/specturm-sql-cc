package org.spectrum.sqlchecker.domain.shared.event;

import lombok.Getter;
import org.spectrum.sqlchecker.domain.shared.enumeration.IssueType;
import org.spectrum.sqlchecker.domain.shared.enumeration.SeverityLevel;

/**
 * 问题检测事件
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
public class IssueDetectedEvent extends DomainEvent {

    private final String scanId;
    private final String sqlId;
    private final IssueType issueType;
    private final SeverityLevel severity;
    private final String message;

    public IssueDetectedEvent(String scanId, String sqlId, IssueType issueType,
                             SeverityLevel severity, String message) {
        super();
        this.scanId = scanId;
        this.sqlId = sqlId;
        this.issueType = issueType;
        this.severity = severity;
        this.message = message;
    }
}
