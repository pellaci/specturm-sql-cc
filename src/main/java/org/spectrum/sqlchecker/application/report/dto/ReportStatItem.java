package org.spectrum.sqlchecker.application.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Report statistics item.
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatItem {

    /**
     * Display label.
     */
    private String label;

    /**
     * Count for the label.
     */
    private int count;

    /**
     * Ratio in percentage (0-100).
     */
    private double ratio;
}
