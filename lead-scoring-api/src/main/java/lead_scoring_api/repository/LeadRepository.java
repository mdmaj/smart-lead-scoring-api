package lead_scoring_api.repository;

import lead_scoring_api.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByContact(String contact);

    long countByStatus(String status);

    long countByCategory(String category);

    List<Lead> findTop5ByLatestScoreIsNotNullOrderByLatestScoreDesc();

    List<Lead> findBySourceAndLatestScoreIsNotNull(String source);

    @Query("""
        SELECT l.source, AVG(l.latestScore)
        FROM Lead l
        WHERE l.latestScore IS NOT NULL
        GROUP BY l.source
    """)
    List<Object[]> findAverageScoreBySource();
}