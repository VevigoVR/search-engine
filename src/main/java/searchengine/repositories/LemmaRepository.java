package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.dto.entity.LemmaEntity;
import searchengine.dto.entity.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    List<LemmaEntity> findAllByLemmaInAndSiteEntityId(List<String> lemmas, SiteEntity site);
    List<LemmaEntity> findAllByLemmaIn(List<String> lemma);
    Long countBySiteEntityId(SiteEntity siteEntity);
}
