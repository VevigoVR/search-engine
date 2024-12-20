package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.DataSet;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.CheckLink;
import searchengine.dto.StatusSite;
import searchengine.dto.entity.SiteEntity;
import searchengine.dto.response.ErrorResponse;
import searchengine.dto.response.SuccessResponse;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private final SitesList sites;
    private final SiteRepository siteRepository;

    @Override
    public ResponseEntity startIndexing() {
        // Если хоть один сайт из списка имеет статус STOPPING
        if (DataSet.isSitesStopping()) {
            ErrorResponse errorResponse =  new ErrorResponse("Индексация запущена, но уже останавливается");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        // Если хоть один сайт из базы данных имеет статус INDEXING
        if (isSitesIndexing()) {
            ErrorResponse errorResponse =  new ErrorResponse("Индексация уже запущена");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        deleteAll();
        List<SiteEntity> siteEntities = saveAll(StatusSite.INDEXING);

        for (SiteEntity site : siteEntities) {
            System.out.println(site);
            new Thread(new FindService(site, this)).start();
        }

        return ResponseEntity.status(HttpStatus.OK).body(new SuccessResponse());
    }

    @Override
    public ResponseEntity stopIndexing() {

        // Если хоть один сайт из списка имеет статус STOPPING
        if (DataSet.isSitesStopping()) {
            ErrorResponse errorResponse =  new ErrorResponse("Индексация уже останавливается");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        // Если ни один сайт из базы данных не имеет статус INDEXING
        if (!isSitesIndexing()) {
            ErrorResponse errorResponse =  new ErrorResponse("Индексация не запущена");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        DataSet.setSitesStatus(StatusSite.STOPPING, StatusSite.INDEXED);

        return ResponseEntity.status(HttpStatus.OK).body(new SuccessResponse());
    }

    @Override
    public SiteEntity save(SiteEntity siteEntity) {
        return siteRepository.save(siteEntity);
    }

    @Override
    public List<SiteEntity> saveAll(StatusSite statusSite) {
        List<SiteEntity> siteEntities = new ArrayList<>();
        for (Site site : sites.getSites()) {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setName(site.getName());
            siteEntity.setUrl(toStandart(site.getUrl()));
            siteEntity.setStatus(statusSite);
            if (statusSite.equals(StatusSite.STOP)) {
                siteEntity.setLastError("сайт ещё не индексировался");
            }
            siteEntity.setStatusTime(new Date());
            siteEntity.setPages(null);
            siteEntities.add(siteEntity);
        }
        return siteRepository.saveAll(siteEntities);
    }

    @Override
    public CheckLink checkUrlAndSetLink(String url) {
        List<SiteEntity> sites = siteRepository.findAll();
        CheckLink checkUrl = new CheckLink();
        checkUrl.setResult(false);
        url = toStandart(url);
        for (SiteEntity site : sites) {
            if (url.startsWith(site.getUrl())) {
                String urlShort = url.replace(site.getUrl(), "/");
                checkUrl.setLink(urlShort);
                checkUrl.setFullLink(url);
                checkUrl.setSiteEntity(site);
                checkUrl.setResult(true);
                break;
            }
        }
        return checkUrl;
    }

    private boolean isSitesIndexing() {
        List<SiteEntity> siteEntities = siteRepository.findAll();
        if (siteEntities.isEmpty()) { return false; }
        for (SiteEntity siteEntity : siteEntities) {
            if (siteEntity.getStatus().equals(StatusSite.INDEXING)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void deleteAll() {
        siteRepository.deleteAll();
    }

    @Override
    public List<SiteEntity> findAll() {
        return siteRepository.findAll();
    }

    @Override
    public SiteEntity findByUrl(String url) {
        return siteRepository.findByUrl(url);
    }

    @Override
    public boolean updateStatusTime(SiteEntity siteEntity) {
        SiteEntity siteEntityOne = siteRepository.findByUrl(siteEntity.getUrl());
        if (siteEntityOne.getStatus().equals(StatusSite.FAILED)) {
            return false;
        }
        log.info("update");
        siteEntityOne.setStatusTime(new Date());
        siteRepository.save(siteEntityOne);
        return true;
    }

    @Override
    public SiteEntity findById(SiteEntity siteEntity) {
        Optional<SiteEntity> site = siteRepository.findById(siteEntity.getId());
        return site.orElse(null);
    }

    @Override
    public String toStandart(String url) {
        if (!url.endsWith("/") && !url.endsWith(".html") && !url.endsWith(".php")) {
            url += "/";
        }

        if (url.startsWith("www.")) {
            url = url.substring(4);
            url = "http://" + url;
        }

        if (url.startsWith("http://www.")) {
            url = url.replaceFirst("http://www.", "http://");
        }

        if (url.startsWith("https://www.")) {
            url = url.replaceFirst("https://www.", "https://");
        }

        if (!url.startsWith("http://")
                && !url.startsWith("https://")
                && !url.startsWith("www.")) {
            url = "http://" + url;
        }
        return url;
    }

    @Override
    public String toNeedProtocol(String url, String domain) {
        if (url.startsWith(domain)) {
            return url;
        }
        if (domain.startsWith("https")) {
            url = url.replaceFirst("http://", "https://");
        }
        return url;
    }
}