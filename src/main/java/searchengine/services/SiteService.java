package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.CheckLink;
import searchengine.dto.StatusSite;
import searchengine.dto.entity.SiteEntity;
import searchengine.dto.response.SuccessResponse;

import java.io.IOException;
import java.util.List;

public interface SiteService {
    SuccessResponse startIndexing();
    SuccessResponse stopIndexing();
    SuccessResponse indexPage(String url) throws IOException;

    SiteEntity save(SiteEntity siteEntity);
    List<SiteEntity> saveAll(StatusSite statusSite);
    SiteEntity findByUrl(String url);
    SiteEntity findById(SiteEntity siteEntity);
    List<SiteEntity> findAll();
    boolean updateStatusTime(SiteEntity siteEntity);
    void deleteAll();

    CheckLink checkUrlAndSetLink(String url);
    String toStandart(String url);
    String toNeedProtocol(String url, String domain);
}
