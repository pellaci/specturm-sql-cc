package org.spectrum.sqlchecker.application.preprocess;

import org.spectrum.sqlchecker.application.preprocess.dto.PreprocessRequest;
import org.spectrum.sqlchecker.application.preprocess.dto.PreprocessResult;

/**
 * SQL 预处理应用服务
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
public interface SqlPreprocessService {

    /**
     * 预处理 SQL
     *
     * @param request 预处理请求
     * @return 预处理结果
     */
    PreprocessResult preprocess(PreprocessRequest request);
}
