package searchengine.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.DataSet;
import searchengine.dto.entity.IndexEntity;
import searchengine.dto.entity.LemmaEntity;
import searchengine.dto.entity.PageEntity;
import searchengine.dto.entity.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.services.IndexService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IndexServiceImpl implements IndexService {
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;

    public IndexServiceImpl(IndexRepository indexRepository, LemmaRepository lemmaRepository) {
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public void saveToIndex(Map<String, Integer> lemmaMap, PageEntity page, SiteEntity site) {
        if (DataSet.isSitesStopping()) {
            return;
        }
        SiteEntity siteEntity = DataSet.getSiteService().findById(site);
        List<LemmaEntity> lemmasFromDB = lemmaRepository.findAllByLemmaInAndSiteEntityId(lemmaMap.keySet().stream().toList(), siteEntity);
        PageEntity pageEntity = DataSet.getPageService().findById(page);
        List<IndexEntity> indexList = new ArrayList<>();
        if (lemmasFromDB.isEmpty()) { return; }
        for (LemmaEntity lemma : lemmasFromDB) {
            for (String lemmaName : lemmaMap.keySet()) {
                if (lemma.getLemma().equals(lemmaName)) {
                    IndexEntity index = new IndexEntity();
                    index.setLemmaId(lemma);
                    index.setRank((float) lemmaMap.get(lemmaName));
                    index.setPageId(pageEntity);
                    indexList.add(index);
                    break;
                }
            }
        }
        saveAll(indexList);
    }

    private void saveAll(List<IndexEntity> indexes) {
        if (DataSet.isSitesStopping()) {
            return;
        }
        indexRepository.saveAll(indexes);
    }

    @Override
    public List<IndexEntity> findAllByLemmaId(LemmaEntity lemma) {
        if (DataSet.isSitesStopping()) {
            return null;
        }
        return indexRepository.findAllByLemmaId(lemma);
    }

    @Override
    public List<IndexEntity> findIndexesByLemma(LemmaEntity lemmaEntity) {
        return indexRepository.findAllByLemmaId(lemmaEntity);
    }

    @Override
    public List<IndexEntity> findAllByPageId(PageEntity pageEntity) {
        return indexRepository.findAllByPageId(pageEntity);
    }

    @Override
    public void deleteAll(List<IndexEntity> indexes) {
        indexRepository.deleteAll(indexes);
    }
}