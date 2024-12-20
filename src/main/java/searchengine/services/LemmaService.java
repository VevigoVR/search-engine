package searchengine.services;

import searchengine.dto.entity.LemmaEntity;
import searchengine.dto.entity.PageEntity;
import searchengine.dto.entity.SiteEntity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface LemmaService {
    void save(LemmaEntity lemmaEntity);
    void saveAll(List<LemmaEntity> lemmas);
    void deleteAll(List<LemmaEntity> lemmas);
    long countBySiteEntityId(SiteEntity siteEntity);
    Map<String, Integer> getLemmasFromString(String page) throws IOException;
    List<LemmaEntity> findAllByLemmaInAndSiteEntityId(List<String> lemmas, SiteEntity site);
    void findAndAddLemmas(SiteEntity siteEntity, PageEntity page) throws IOException;
    void insertOrUpdateLemmaFromTable(List<String> lemmas, SiteEntity siteEntity);
    List<LemmaEntity> fromStringToObjects(List<String> lemmas, SiteEntity siteEntity);
    List<LemmaEntity> findAllBySiteEntityIdAndLemmaIn(SiteEntity siteEntity, List<String> lemmas);
    List<LemmaEntity> findAllByLemmaIn(List<String> lemmas);
}
