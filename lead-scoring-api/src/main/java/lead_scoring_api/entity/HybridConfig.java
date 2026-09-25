package lead_scoring_api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "hybrid_config")
public class HybridConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double aiWeight;

    private Double ruleWeight;

    private Boolean isActive;

    public HybridConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAiWeight() {
        return aiWeight;
    }

    public void setAiWeight(Double aiWeight) {
        this.aiWeight = aiWeight;
    }

    public Double getRuleWeight() {
        return ruleWeight;
    }

    public void setRuleWeight(Double ruleWeight) {
        this.ruleWeight = ruleWeight;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}