package org.spectrum.sqlchecker.infrastructure.analysis.explain;

import lombok.RequiredArgsConstructor;
import org.spectrum.sqlchecker.application.analysis.dto.PlanNode;
import org.spectrum.sqlchecker.domain.shared.exception.ConnectionException;
import org.spectrum.sqlchecker.infrastructure.database.ConnectionManager;
import org.spectrum.sqlchecker.infrastructure.database.adapter.DatabaseAdapter;
import org.spectrum.sqlchecker.infrastructure.database.adapter.DatabaseAdapterFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * Execute EXPLAIN queries using the appropriate database adapter.
 */
@Component
@RequiredArgsConstructor
public class ExplainPlanExecutor {

    private final ConnectionManager connectionManager;
    private final DatabaseAdapterFactory adapterFactory;
    private final ExplainAnalysisSettings settings;

    public ExplainExecutionResult execute(String sql, String connectionId) throws SQLException {
        ConnectionManager.ConnectionConfig config = connectionManager.getConfig(connectionId);
        if (config == null) {
            throw new ConnectionException("Connection not found: " + connectionId);
        }

        DatabaseAdapter adapter = adapterFactory.getAdapter(config.getType());
        String explainSql = adapter.buildExplainSql(sql);

        Connection conn = connectionManager.getConnection(connectionId);
        try (Statement stmt = conn.createStatement()) {
            int timeoutSeconds = settings.getTimeoutSeconds();
            if (timeoutSeconds > 0) {
                stmt.setQueryTimeout(timeoutSeconds);
            }
            try (ResultSet rs = stmt.executeQuery(explainSql)) {
                DatabaseAdapter.ResultSetParseResult result = adapter.parseExplainResult(rs);
                return new ExplainExecutionResult(result.nodes(), result.metadata(), adapter.getDatabaseName());
            }
        }
    }

    public record ExplainExecutionResult(
            List<PlanNode> nodes,
            Map<String, Object> metadata,
            String databaseName
    ) {
    }
}
