package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.DataSet;
import searchengine.dto.entity.LemmaEntity;
import searchengine.dto.entity.PageEntity;
import searchengine.dto.entity.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.services.IndexService;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {
    private final IndexService indexService;
    private final LemmaRepository lemmaRepository;

    @Override
    public void save(LemmaEntity lemmaEntity) {
        if (DataSet.isSitesStopping()) {
            return;
        }
        lemmaRepository.save(lemmaEntity);
    }

    @Override
    public void saveAll(List<LemmaEntity> lemmas) {
        if (DataSet.isSitesStopping()) {
            return;
        }
        lemmaRepository.saveAll(lemmas);
    }

    @Override
    public void deleteAll(List<LemmaEntity> lemmas) {
        lemmaRepository.deleteAll(lemmas);
    }

    @Override
    public long countBySiteEntityId(SiteEntity siteEntity) {
        return lemmaRepository.countBySiteEntityId(siteEntity);
    }

    @Override
    public Map<String, Integer> getLemmasFromString(String page) throws IOException {
        page = Jsoup.parse(page).text();
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(page);
        return lemmas;
    }

    @Override
    public List<LemmaEntity> findAllByLemmaInAndSiteEntityId(List<String> lemmas, SiteEntity site) {
        if (site != null) {
            return lemmaRepository.findAllByLemmaInAndSiteEntityId(lemmas, site);
        } else {
            return lemmaRepository.findAllByLemmaIn(lemmas);
        }
    }

    // стартовый метод
    @Override
    synchronized
    public void findAndAddLemmas(SiteEntity siteEntity, PageEntity page) throws IOException {
        String doc = Jsoup.parse(page.getContent()).text();
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(doc);
        List<String> lemmaNames = new ArrayList<>(lemmas.keySet());
        insertOrUpdateLemmaFromTable(lemmaNames, siteEntity, page);
        indexService.saveToIndex(lemmas, page, siteEntity);
    }

    @Override
    public void insertOrUpdateLemmaFromTable(List<String> lemmas, SiteEntity siteEntity, PageEntity page) {

        List<LemmaEntity> lemmasFromDB = findAllByLemmaInAndSiteEntityId(lemmas, siteEntity);
        List<LemmaEntity> lemmasFromPage = getFromStringToObjects(lemmas, siteEntity);
        if (lemmasFromDB.isEmpty()) {
            saveAll(lemmasFromPage);
            return;
        }
        for (LemmaEntity lemma : lemmasFromDB) {
            lemma.setFrequency(lemma.getFrequency() + 1);
        }
        saveAll(lemmasFromDB);

        List<LemmaEntity> lemmasForSave = new ArrayList<>();

        for (LemmaEntity lemmaPG : lemmasFromPage) {
            int i = 0;
            for (LemmaEntity lemmaDB : lemmasFromDB) {
                if (lemmaPG.getLemma().equals(lemmaDB.getLemma())) {
                    i++;
                    break;
                }
            }
            if (i == 0) {
                lemmasForSave.add(lemmaPG);
            }
        }
        saveAll(lemmasForSave);
    }

    public List<LemmaEntity> getFromStringToObjects(List<String> lemmas, SiteEntity siteEntity) {
        List<LemmaEntity> lemmaList = new ArrayList<>();
        lemmas.forEach(lemma -> {
            LemmaEntity lemmaEntity = new LemmaEntity();
            lemmaEntity.setSiteEntityId(siteEntity);
            lemmaEntity.setLemma(lemma);
            lemmaEntity.setFrequency(1);
            lemmaList.add(lemmaEntity);
        });
        return lemmaList;
    }

    @Override
    public List<LemmaEntity> findAllBySiteEntityIdAndLemmaIn(SiteEntity siteEntity, List<String> lemmas) {
        return lemmaRepository.findAllByLemmaInAndSiteEntityId(lemmas, siteEntity);
    }

    @Override
    public List<LemmaEntity> findAllByLemmaIn(List<String> lemmas) {
        return lemmaRepository.findAllByLemmaIn(lemmas);
    }
}