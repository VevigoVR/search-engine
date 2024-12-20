package searchengine.services;

import searchengine.dto.entity.IndexEntity;
import searchengine.dto.entity.LemmaEntity;
import searchengine.dto.entity.PageEntity;
import searchengine.dto.entity.SiteEntity;

import java.util.List;
import java.util.Map;

public interface IndexService {
    void saveToIndex(Map<String, Integer> lemmaMap, PageEntity page, SiteEntity site);
    List<IndexEntity> findAllByLemmaId(LemmaEntity lemma);
    List<IndexEntity> findIndexesByLemma(LemmaEntity lemmaEntity);
    List<IndexEntity> findAllByPageId(PageEntity pageEntity);
    void deleteAll(List<IndexEntity> indexes);
}