package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.DataSet;
import searchengine.dto.CheckLink;
import searchengine.dto.ParseDTO;
import searchengine.dto.entity.IndexEntity;
import searchengine.dto.entity.LemmaEntity;
import searchengine.dto.entity.PageEntity;
import searchengine.dto.entity.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.services.PageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PageServiceImpl implements PageService {
    private final PageRepository pageRepository;

    @Override
    public long countBySiteEntityId(SiteEntity siteEntity) {
        return pageRepository.countBySiteEntityId(siteEntity);
    }

    @Override
    public PageEntity save(PageEntity pageEntity) {
        return pageRepository.save(pageEntity);
    }

    @Override
    public long count() {
        return pageRepository.count();
    }

    @Override
    public PageEntity findById(PageEntity pageEntity) {
        Optional<PageEntity> page = pageRepository.findById(pageEntity.getId());
        return page.orElse(null);
    }

    @Override
    public void indexPage(CheckLink checkLink) throws IOException {
        Optional<PageEntity> pageEntity = pageRepository.findByPathAndSiteEntityId(checkLink.getLink(), checkLink.getSiteEntity());

        PageEntity newPage = new PageEntity();
        newPage.setSiteEntityId(checkLink.getSiteEntity());
        newPage.setPath(checkLink.getLink());
        ParseDTO parseDTO = parseHTML(checkLink.getFullLink());
        newPage.setContent(parseDTO.getDoc().toString());
        newPage.setCode(parseDTO.getCode());

        if (pageEntity.isPresent()) {
            removeOldPageData(pageEntity.get(), checkLink.getSiteEntity());
            pageRepository.delete(pageEntity.get());
        }

        PageEntity pageFromDB = save(newPage);
        DataSet.getSiteService().updateStatusTime(checkLink.getSiteEntity());
        DataSet.getLemmaService().findAndAddLemmas(checkLink.getSiteEntity(), pageFromDB);
    }

    @Override
    public CheckLink checkLink(CheckLink checkLink) {
        if (urlNotToImgOrSmthForOnePage(checkLink.getFullLink(), checkLink.getSiteEntity().getUrl())) {
            checkLink.setResult(true);
        } else {
            checkLink.setResult(false);
        }
        return checkLink;
    }

    @Override
    public boolean urlNotToImgOrSmth(String link) {
        String[] currentLink = link.split("\\.");
        if (currentLink.length == 1) {
            return true;
        }

        if (currentLink[currentLink.length - 1].equals("html")
                || currentLink[currentLink.length - 1].equals("php")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean urlNotToImgOrSmthForOnePage(String link, String siteUrl) {
        String[] currentLink = link.split("\\.");
        String[] currentUrl = siteUrl.split("\\.");
        if (currentLink.length == currentUrl.length) {
            return true;
        }
        if (currentLink[currentLink.length - 1].equals("html")
                || currentLink[currentLink.length - 1].equals("php")) {
            return true;
        }
        return false;
    }

    private ParseDTO parseHTML(String url) throws IOException {
        Elements urls = null;
        Document doc = null;
        int code = 0;
        try {
            Connection.Response connection = Jsoup.connect(url)
                    .userAgent("SearchBot")
                    .timeout(20000)
                    .execute();
            code = connection.statusCode();
            doc = connection.parse();
        } catch (HttpStatusException exception) {
            System.out.println("http exception: " + exception.getUrl());
        }

        if (doc != null) {
            urls = doc.select("a");
        }
        return new ParseDTO(urls, doc, code);
    }

    @Override
    public void delete(PageEntity pageEntity) {
        pageRepository.delete(pageEntity);
    }

    private void removeOldPageData(PageEntity pageEntity, SiteEntity siteEntity) throws IOException {
        List<IndexEntity> indexes = DataSet.getIndexService().findAllByPageId(pageEntity);
        DataSet.getIndexService().deleteAll(indexes);
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        Document doc = Jsoup.parse(pageEntity.getContent());
        Map<String, Integer> lemmasMap = lemmaFinder.collectLemmas(doc.text());
        List<LemmaEntity> lemmas = DataSet
                .getLemmaService()
                .findAllBySiteEntityIdAndLemmaIn(siteEntity, lemmasMap.keySet().stream().toList());
        List<LemmaEntity> lemmasToDelete = new ArrayList<>();
        for (LemmaEntity lemmaEntity : lemmas) {
            if (lemmaEntity.getFrequency() > 1) {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() - 1);
            } else {
                lemmasToDelete.add(lemmaEntity);
            }
        }
        DataSet.getLemmaService().saveAll(lemmas);
        if (!lemmasToDelete.isEmpty()) {
            DataSet.getLemmaService().deleteAll(lemmasToDelete);
        }
    }
}
