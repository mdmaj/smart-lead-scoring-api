package lead_scoring_api.service;

import lead_scoring_api.dto.LeadRequest;
import lead_scoring_api.entity.HybridConfig;
import lead_scoring_api.entity.ScoringConfig;
import lead_scoring_api.repository.HybridConfigRepository;
import lead_scoring_api.repository.ScoringConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HybridScoringService {

    private final ScoringConfigRepository scoringConfigRepository;
    private final HybridConfigRepository hybridConfigRepository;

    public HybridScoringService(
            ScoringConfigRepository scoringConfigRepository,
            HybridConfigRepository hybridConfigRepository) {

        this.scoringConfigRepository = scoringConfigRepository;
        this.hybridConfigRepository = hybridConfigRepository;
    }

    public double calculateRuleScore(LeadRequest request) {

        List<ScoringConfig> configs =
                scoringConfigRepository.findByIsActiveTrue();

        double score = 0.0;

        for (ScoringConfig config : configs) {

            String factor = config.getFactorName();
            double weight = config.getWeight();

            switch (factor) {

                case "buying_intent":
                    if (request.getMessage() != null
                            && !request.getMessage().isBlank()) {
                        score += weight;
                    }
                    break;

                case "urgency":
                    if (request.getMessage() != null
                            && containsUrgency(request.getMessage())) {
                        score += weight;
                    }
                    break;

                case "budget":
                    if (request.getBudget() != null
                            && request.getBudget() > 0) {
                        score += weight;
                    }
                    break;

                case "message_quality":
                    if (request.getMessage() != null
                            && request.getMessage().length() >= 30) {
                        score += weight;
                    }
                    break;

                case "company_info":
                    if (request.getCompany() != null
                            && !request.getCompany().isBlank()) {
                        score += weight;
                    }
                    break;

                default:
                    break;
            }
        }

        return Math.min(score, 100.0);
    }

    public double calculateFinalScore(
            double aiScore,
            double ruleScore) {

        HybridConfig config =
                hybridConfigRepository
                        .findFirstByIsActiveTrue()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Active hybrid scoring configuration not found"
                                ));

        double aiWeight = config.getAiWeight() / 100.0;
        double ruleWeight = config.getRuleWeight() / 100.0;

        double finalScore =
                (aiScore * aiWeight)
                        + (ruleScore * ruleWeight);

        return Math.min(Math.max(finalScore, 0.0), 100.0);
    }

    private boolean containsUrgency(String message) {

        String text = message.toLowerCase();

        return text.contains("urgent")
                || text.contains("urgently")
                || text.contains("asap")
                || text.contains("immediately")
                || text.contains("today");
    }
}