package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.DataSet;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.CheckLink;
import searchengine.dto.StatusSite;
import searchengine.dto.entity.SiteEntity;
import searchengine.dto.response.SuccessResponse;
import searchengine.exceptions.MyBadRequestException;
import searchengine.exceptions.MyConflictRequestException;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteService;

import java.io.IOException;
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
    public SuccessResponse startIndexing() {
        if (DataSet.isSitesStopping()) {
            throw new MyConflictRequestException("Индексация запущена, но уже останавливается");
        }
        if (isSitesIndexing()) {
            throw new MyConflictRequestException("Индексация уже запущена");
        }
        deleteAll();
        List<SiteEntity> siteEntities = saveAll(StatusSite.INDEXING);
        DataSet.setSitesStatus(StatusSite.INDEXING, StatusSite.INDEXING);
        for (SiteEntity site : siteEntities) {
            new Thread(new FindService(site, this)).start();
        }
        return new SuccessResponse();
    }

    @Override
    public SuccessResponse stopIndexing() {
        if (DataSet.isSitesStopping()) {
            throw new MyConflictRequestException("Индексация уже останавливается");
        }
        if (!isSitesIndexing()) {
            throw new MyConflictRequestException("Индексация не запущена");
        }
        DataSet.setSitesStatus(StatusSite.STOPPING, StatusSite.INDEXED);
        return new SuccessResponse();
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
                siteEntity.setLastError("сайт не индексировался");
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

    @Override
    public SuccessResponse indexPage(String url) {
        CheckLink checkUrl = checkUrlAndSetLink(url);
        if (!checkUrl.isResult()) {
            throw new MyBadRequestException("Данная страница находится за пределами сайтов указанных в конфигурационном файле");
        }
        CheckLink checkLink = DataSet.getPageService().checkLink(checkUrl);
        if (!checkLink.isResult()) {
            throw new MyBadRequestException("Ссылка не соответствует параметрам");
        }
        try {
            DataSet.getPageService().indexPage(checkLink);
        } catch (IOException e) {
            log.info("Ошибка индексирования страницы");
        }
        return new SuccessResponse();
    }
}