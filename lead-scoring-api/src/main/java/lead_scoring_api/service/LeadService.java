package lead_scoring_api.service;

import lead_scoring_api.dto.AiScoreResponse;
import lead_scoring_api.dto.LeadRequest;
import lead_scoring_api.entity.Lead;
import lead_scoring_api.repository.LeadRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import lead_scoring_api.exception.LeadNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final RestClient restClient;
    private final ScoreAuditService scoreAuditService;
    private final HybridScoringService hybridScoringService;

    public LeadService(
            LeadRepository leadRepository,
            ScoreAuditService scoreAuditService,
            HybridScoringService hybridScoringService) {

        this.leadRepository = leadRepository;
        this.scoreAuditService = scoreAuditService;
        this.hybridScoringService = hybridScoringService;

        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8000")
                .build();
    }

    // ============================================================
    // CREATE LEAD
    // ============================================================

    public Lead createLead(LeadRequest request) {

        // --------------------------------------------------------
        // 1. Check duplicate using email
        // --------------------------------------------------------

        Optional<Lead> existingLead = Optional.empty();

        if (request.getEmail() != null
                && !request.getEmail().isBlank()) {

            String email = request.getEmail()
                    .trim()
                    .toLowerCase();

            existingLead =
                    leadRepository.findByContact(email);
        }

        // --------------------------------------------------------
        // 2. If email not found, check duplicate using phone
        // --------------------------------------------------------

        if (existingLead.isEmpty()
                && request.getPhone() != null
                && !request.getPhone().isBlank()) {

            String phone = request.getPhone().trim();

            existingLead =
                    leadRepository.findByContact(phone);
        }

        // --------------------------------------------------------
        // 3. Duplicate / Repeat Lead
        // --------------------------------------------------------

        if (existingLead.isPresent()) {

            Lead lead = existingLead.get();

            lead.setName(request.getName());
            lead.setSource(request.getSource());
            lead.setMessage(request.getMessage());
            lead.setBudget(request.getBudget());
            lead.setCompany(request.getCompany());

            lead.setStatus("DUPLICATE");
            lead.setRepeatLead(true);
            lead.setUpdatedAt(LocalDateTime.now());

            return leadRepository.save(lead);
        }

        // --------------------------------------------------------
        // 4. Determine contact value
        // --------------------------------------------------------

        String contact = request.getEmail();

        if (contact == null || contact.isBlank()) {
            contact = request.getPhone();
        }

        if (contact != null) {
            contact = contact.trim();

            // Normalize email
            if (contact.contains("@")) {
                contact = contact.toLowerCase();
            }
        }

        // --------------------------------------------------------
        // 5. Create new lead
        // --------------------------------------------------------

        Lead lead = new Lead();

        lead.setName(request.getName());
        lead.setContact(contact);
        lead.setSource(request.getSource());
        lead.setMessage(request.getMessage());
        lead.setBudget(request.getBudget());
        lead.setCompany(request.getCompany());

        lead.setStatus("PENDING_SCORE");
        lead.setRepeatLead(false);

        LocalDateTime now = LocalDateTime.now();

        lead.setCreatedAt(now);
        lead.setUpdatedAt(now);

        // --------------------------------------------------------
        // 6. Save lead in MySQL
        // --------------------------------------------------------

        lead = leadRepository.save(lead);

        // --------------------------------------------------------
        // 7. Call Python AI service
        // --------------------------------------------------------

        try {

            AiScoreResponse aiResponse =
                    restClient.post()
                            .uri("/score")
                            .body(request)
                            .retrieve()
                            .body(AiScoreResponse.class);

            // ----------------------------------------------------
            // 8. Process AI response
            // ----------------------------------------------------

            if (aiResponse != null) {

                // ------------------------------------------------
                // Calculate rule score
                // ------------------------------------------------

                double ruleScore =
                        hybridScoringService
                                .calculateRuleScore(request);

                System.out.println(
                        "AI Score: " + aiResponse.getScore()
                );

                System.out.println(
                        "Rule Score: " + ruleScore
                );

                // ------------------------------------------------
                // Calculate hybrid score
                // ------------------------------------------------

                double finalScore =
                        hybridScoringService
                                .calculateFinalScore(
                                        aiResponse.getScore(),
                                        ruleScore
                                );

                System.out.println(
                        "Final Hybrid Score: " + finalScore
                );

                // ------------------------------------------------
                // Update lead
                // ------------------------------------------------

                lead.setLatestScore(finalScore);

                lead.setCategory(
                        getCategoryFromScore(finalScore)
                );

                lead.setStatus("SCORED");
                lead.setUpdatedAt(LocalDateTime.now());

                lead = leadRepository.save(lead);

                // ------------------------------------------------
                // 9. Save audit in PostgreSQL
                // ------------------------------------------------

                String rawAiResponse = String.format(
                        "{\"score\":%s,\"category\":\"%s\",\"reason\":\"%s\"}",
                        aiResponse.getScore(),
                        aiResponse.getCategory(),
                        aiResponse.getReason()
                );

                String ruleBreakdown = String.format(
                        "{\"ai_score\":%s,\"rule_score\":%s,\"rules_applied\":true}",
                        aiResponse.getScore(),
                        ruleScore
                );

                scoreAuditService.saveAudit(
                        lead.getId(),
                        "gemini-3.6-flash",
                        rawAiResponse,
                        ruleBreakdown,
                        finalScore
                );
            }

        } catch (Exception e) {

            // ----------------------------------------------------
            // AI service unavailable
            // ----------------------------------------------------

            System.out.println(
                    "AI scoring service unavailable: "
                            + e.getMessage()
            );

            lead.setStatus("PENDING_SCORE");
            lead.setUpdatedAt(LocalDateTime.now());

            lead = leadRepository.save(lead);
        }

        // --------------------------------------------------------
        // 10. Return lead
        // --------------------------------------------------------

        return lead;
    }

    // ============================================================
    // RESCORE EXISTING LEAD
    // ============================================================

    public Lead rescoreLead(Long leadId) {

        // --------------------------------------------------------
        // 1. Find lead
        // --------------------------------------------------------

        Lead lead =
                leadRepository.findById(leadId)
                        .orElseThrow(() ->
        new LeadNotFoundException("Lead not found"));

        // --------------------------------------------------------
        // 2. Convert Lead → LeadRequest
        // --------------------------------------------------------

        LeadRequest request = new LeadRequest();

        request.setName(lead.getName());
        request.setMessage(lead.getMessage());
        request.setSource(lead.getSource());
        request.setBudget(lead.getBudget());
        request.setCompany(lead.getCompany());

        // Restore email or phone from contact
        if (lead.getContact() != null) {

            if (lead.getContact().contains("@")) {

                request.setEmail(
                        lead.getContact()
                );

            } else {

                request.setPhone(
                        lead.getContact()
                );
            }
        }

        // --------------------------------------------------------
        // 3. Call Python AI service
        // --------------------------------------------------------

        try {

            AiScoreResponse aiResponse =
                    restClient.post()
                            .uri("/score")
                            .body(request)
                            .retrieve()
                            .body(AiScoreResponse.class);

            // ----------------------------------------------------
            // Validate AI response
            // ----------------------------------------------------

            if (aiResponse == null) {

                throw new RuntimeException(
                        "Empty AI response"
                );
            }

            // ----------------------------------------------------
            // 4. Calculate rule score
            // ----------------------------------------------------

            double ruleScore =
                    hybridScoringService
                            .calculateRuleScore(request);

            // ----------------------------------------------------
            // 5. Calculate hybrid score
            // ----------------------------------------------------

            double finalScore =
                    hybridScoringService
                            .calculateFinalScore(
                                    aiResponse.getScore(),
                                    ruleScore
                            );

            // ----------------------------------------------------
            // 6. Update lead
            // ----------------------------------------------------

            lead.setLatestScore(finalScore);

            lead.setCategory(
                    getCategoryFromScore(finalScore)
            );

            lead.setStatus("SCORED");
            lead.setUpdatedAt(LocalDateTime.now());

            lead = leadRepository.save(lead);

            // ----------------------------------------------------
            // 7. Save PostgreSQL audit
            // ----------------------------------------------------

            String rawAiResponse = String.format(
                    "{\"score\":%s,\"category\":\"%s\",\"reason\":\"%s\"}",
                    aiResponse.getScore(),
                    aiResponse.getCategory(),
                    aiResponse.getReason()
            );

            String ruleBreakdown = String.format(
                    "{\"ai_score\":%s,\"rule_score\":%s,\"ai_weight\":70,\"rule_weight\":30}",
                    aiResponse.getScore(),
                    ruleScore
            );

            scoreAuditService.saveAudit(
                    lead.getId(),
                    "gemini-3.6-flash",
                    rawAiResponse,
                    ruleBreakdown,
                    finalScore
            );

            return lead;

        } catch (Exception e) {

            // ----------------------------------------------------
            // AI unavailable during rescore
            // ----------------------------------------------------

            System.out.println(
                    "Rescore failed: "
                            + e.getMessage()
            );

            lead.setStatus("PENDING_SCORE");
            lead.setUpdatedAt(LocalDateTime.now());

            return leadRepository.save(lead);
        }
    }

    // ============================================================
    // SCORE → CATEGORY
    // ============================================================

    private String getCategoryFromScore(double score) {

        if (score >= 80) {
            return "HOT";
        }

        if (score >= 50) {
            return "WARM";
        }

        return "COLD";
    }
}