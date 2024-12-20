package searchengine.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import searchengine.dto.entity.LemmaEntity;
import searchengine.dto.entity.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.services.IndexService;
import searchengine.services.SiteService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LemmaAdd implements CommandLineRunner {

    private final IndexService indexService;
    private final LemmaRepository lemmaRepository;
    private final SiteService siteService;

    @Autowired
    public LemmaAdd(IndexService indexService, LemmaRepository lemmaRepository, SiteService siteService) {
        this.indexService = indexService;
        this.lemmaRepository = lemmaRepository;
        this.siteService = siteService;
    }

    private List<String> lemmaList() {
        List<String> lemmas = new ArrayList<>();
        lemmas.add("постоянно");
        lemmas.add("шпион");
        lemmas.add("бпла");
        lemmas.add("изменяться");
        lemmas.add("занять");
        lemmas.add("ересь");
        return lemmas;
    }

    private SiteEntity getSiteEntity() {
        List<SiteEntity> siteEntities = siteService.findAll();
        for (SiteEntity siteEntity : siteEntities) {
            log.info("сайт: " + siteEntity.getUrl());
            return siteEntity;
        }
        return null;
    }

    private String showList(List<LemmaEntity> list) {
        String str = "";
        for (LemmaEntity lemma : list) {
            str += lemma.getLemma() + ", ";
        }
        return str;
    }

    void insertOrUpdateLemmaFromTable(List<String> lemmas, SiteEntity siteEntity) {
        List<LemmaEntity> lemmasFromDB = lemmaRepository.findAllByLemmaInAndSiteEntityId(lemmas, siteEntity);
        System.out.println("lemmasFromDB: " + showList(lemmasFromDB));
        List<LemmaEntity> lemmasFromPage = fromStringToObjects(lemmas, siteEntity);
        System.out.println("lemmasFromPage: " + showList(lemmasFromPage));
        if (lemmasFromDB.isEmpty()) {
            lemmaRepository.saveAll(lemmasFromPage);
            return;
        }
        List<LemmaEntity> lemmaToCreate = new ArrayList<>(lemmasFromPage);
        List<LemmaEntity> lemmaToUpdate = new ArrayList<>();

        for (LemmaEntity lemmaPage : lemmasFromPage) {
            for (LemmaEntity lemmaDb : lemmasFromDB) {
                if (lemmaPage.getLemma().equals(lemmaDb.getLemma())) {
                    lemmaToUpdate.add(lemmaPage);
                    break;
                }
            }
        }

        System.out.println("lemmaToUpdate: " + showList(lemmaToUpdate));

        for (LemmaEntity lemmaPage : lemmasFromPage) {
            for (LemmaEntity lemmaDb : lemmasFromDB) {
                if (lemmaPage.getLemma().equals(lemmaDb.getLemma())) {
                    lemmaToCreate.remove(lemmaPage);
                    break;
                }
            }
        }
        System.out.println("lemmaToCreate: " + showList(lemmaToCreate));
        //lemmaRepository.saveAll(lemmaToCreate);
        //updateLemmas(lemmaToUpdate, siteEntity);
    }

    List<LemmaEntity> fromStringToObjects(List<String> lemmas, SiteEntity siteEntity) {
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
    public void run(String... args) throws Exception {
        //insertOrUpdateLemmaFromTable(lemmaList(), getSiteEntity());
    }
}