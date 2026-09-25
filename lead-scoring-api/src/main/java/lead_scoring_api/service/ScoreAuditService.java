package lead_scoring_api.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScoreAuditService {

    private final JdbcTemplate postgresJdbcTemplate;

    public ScoreAuditService(
            @Qualifier("postgresJdbcTemplate")
            JdbcTemplate postgresJdbcTemplate) {

        this.postgresJdbcTemplate = postgresJdbcTemplate;
    }

    public void saveAudit(
            Long leadId,
            String modelName,
            String rawAiResponse,
            String ruleBreakdown,
            Double finalScore) {

        String sql = """
                INSERT INTO score_audit
                (lead_id, model_name, raw_ai_response, rule_breakdown, final_score)
                VALUES (?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?)
                """;

        try {

            postgresJdbcTemplate.update(
                    sql,
                    leadId,
                    modelName,
                    rawAiResponse,
                    ruleBreakdown,
                    finalScore
            );

            System.out.println("PostgreSQL audit saved successfully.");

        } catch (Exception e) {

            System.out.println(
                    "PostgreSQL audit save failed: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}