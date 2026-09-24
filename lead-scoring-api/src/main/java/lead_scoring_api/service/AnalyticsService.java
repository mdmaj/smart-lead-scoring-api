package lead_scoring_api.service;

import lead_scoring_api.entity.Lead;
import lead_scoring_api.repository.LeadRepository;
import org.springframework.stereotype.Service;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final LeadRepository leadRepository;

    public AnalyticsService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public Map<String, Object> getAnalytics() {

        Map<String, Object> analytics = new LinkedHashMap<>();

        // 1. Total leads
        long totalLeads = leadRepository.count();

        // 2. HOT / WARM / COLD breakdown
        long hotLeads = leadRepository.countByCategory("HOT");
        long warmLeads = leadRepository.countByCategory("WARM");
        long coldLeads = leadRepository.countByCategory("COLD");

        // 3. Other statuses
        long pendingLeads =
                leadRepository.countByStatus("PENDING_SCORE");

        long duplicateLeads =
                leadRepository.countByStatus("DUPLICATE");

        // 4. Average score per source
        List<Object[]> averageScoreResults =
                leadRepository.findAverageScoreBySource();

        Map<String, Double> averageScoreBySource =
                new LinkedHashMap<>();

        for (Object[] row : averageScoreResults) {

            String source = (String) row[0];
            Double averageScore = ((Number) row[1]).doubleValue();

            averageScoreBySource.put(
                    source,
                    Math.round(averageScore * 100.0) / 100.0
            );
        }

        // 5. Top 5 hottest leads
        List<Lead> topLeads =
                leadRepository
                        .findTop5ByLatestScoreIsNotNullOrderByLatestScoreDesc();

        // Build response
        analytics.put("totalLeads", totalLeads);

        analytics.put("categoryBreakdown", Map.of(
                "HOT", hotLeads,
                "WARM", warmLeads,
                "COLD", coldLeads
        ));

        analytics.put("pendingLeads", pendingLeads);

        analytics.put("duplicateLeads", duplicateLeads);

        analytics.put(
                "averageScoreBySource",
                averageScoreBySource
        );

        analytics.put(
                "top5HottestLeads",
                topLeads
        );

        return analytics;
    }
}