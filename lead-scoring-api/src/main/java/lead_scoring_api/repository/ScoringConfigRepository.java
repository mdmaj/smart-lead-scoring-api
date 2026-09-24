package lead_scoring_api.repository;

import lead_scoring_api.entity.ScoringConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoringConfigRepository
        extends JpaRepository<ScoringConfig, Long> {

    List<ScoringConfig> findByIsActiveTrue();
}