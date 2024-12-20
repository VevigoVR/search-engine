package searchengine.services;

import searchengine.dto.CheckLink;
import searchengine.dto.entity.PageEntity;
import searchengine.dto.entity.SiteEntity;

import java.io.IOException;

public interface PageService {
    long count();
    long countBySiteEntityId(SiteEntity siteEntity);
    PageEntity save(PageEntity pageEntity);
    PageEntity findById(PageEntity pageEntity);
    void delete(PageEntity pageEntity);

    boolean urlNotToImgOrSmth(String link);
    boolean urlNotToImgOrSmthForOnePage(String link, String siteUrl);
    CheckLink checkLink(CheckLink checkLink);
    void indexPage(CheckLink checkLink) throws IOException;
}
