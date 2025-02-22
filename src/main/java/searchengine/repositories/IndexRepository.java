package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.dto.entity.IndexEntity;
import searchengine.dto.entity.LemmaEntity;
import searchengine.dto.entity.PageEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    List<IndexEntity> findAllByLemmaId(LemmaEntity lemma);
    List<IndexEntity> findAllByPageId(PageEntity pageEntity);

    List<IndexEntity> findAllByLemmaIdIn(List<LemmaEntity> lemmas);
}
