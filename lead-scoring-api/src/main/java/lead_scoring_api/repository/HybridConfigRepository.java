package lead_scoring_api.repository;

import lead_scoring_api.entity.HybridConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HybridConfigRepository
        extends JpaRepository<HybridConfig, Long> {

    Optional<HybridConfig> findFirstByIsActiveTrue();
}