package searchengine.services.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.dto.ParseDTO;
import searchengine.dto.UrlDTO;
import searchengine.dto.entity.PageEntity;
import searchengine.dto.entity.SiteEntity;
import searchengine.config.DataSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Setter
@Getter
@AllArgsConstructor
public class PageFinder extends RecursiveAction {

    private FindService findService = new FindService();
    private volatile SiteEntity siteEntity = new SiteEntity();
    private volatile String url; // текущая ссылка внутри сайта
    private volatile String mainUrl; // главная страница сайта
    private CopyOnWriteArrayList<PageFinder> tasks = new CopyOnWriteArrayList<>();
    private ForkJoinPool forkJoinPool;

    public PageFinder(FindService findService, String url, SiteEntity siteEntity, ForkJoinPool forkJoinPool) {
        this.findService = findService;
        this.url = DataSet.getSiteService().toStandart(url);
        this.mainUrl = DataSet.getSiteService().toStandart(siteEntity.getUrl());
        this.siteEntity = siteEntity;
        this.forkJoinPool = forkJoinPool;
    }

    @Override
    synchronized
    protected void compute() {
        if (DataSet.isSitesStopping()) {
            return;
        }
        try {
            List<String> links = new ArrayList<>();
            ParseDTO parseDTO = parseHTML(url);
            Elements elements = parseDTO.getElements();
            if (elements == null) {
                return;
            }
            elements.forEach(element -> {
                links.add(element.attr("href"));
            });

            for (String link : links) {
                if (DataSet.isSitesStopping()) {
                    return;
                }
                UrlDTO urlDTO = constractUrlFromThisSite(link);
                if (!urlDTO.isEnabled()) { continue; }
                link = removeParamFromLink(urlDTO.getLink());
                if (!DataSet.getPageService().urlNotToImgOrSmth(link)) { continue; }
                if (!findService.getAllLinks().add(link)) { continue; }
                PageFinder newHTMLReader = new PageFinder(findService, urlDTO.getUrl(), siteEntity, forkJoinPool);

                PageEntity page = new PageEntity();
                page.setSiteEntityId(siteEntity);
                page.setPath(link);
                page.setContent(parseDTO.getDoc().toString());
                page.setCode(parseDTO.getCode());
                PageEntity pageFromDB = DataSet.getPageService().save(page);
                DataSet.getSiteService().updateStatusTime(siteEntity);

                if (pageFromDB != null) {
                    DataSet.getLemmaService().findAndAddLemmas(siteEntity, pageFromDB);
                }

                Thread.sleep(2000);

                newHTMLReader.fork();
                tasks.add(newHTMLReader);
            }
        } catch (InterruptedException interruptedException) {
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        if (tasks == null) { return; }
        for (PageFinder task : tasks) {
            if (DataSet.isSitesStopping()) {
                return;
            }
            task.join();
        }
    }

    private String removeParamFromLink(String link) {
        if (link.contains("#")) {
            link = link.substring(0, link.lastIndexOf("#"));
        } else if (link.contains("?")) {
            link = link.substring(0, link.lastIndexOf("?"));
        }
        return link;
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
        }

        if (doc != null) {
            urls = doc.select("a");
        }
        return new ParseDTO(urls, doc, code);
    }

    private UrlDTO constractUrlFromThisSite(String url) {
        String currentUrl = url;
        if (!currentUrl.endsWith("/") && !currentUrl.endsWith(".html") && !currentUrl.endsWith(".php")) {
            currentUrl += "/";
        }
        String mainDomain = mainUrl;
        String[] mainDomainParts = mainDomain.split("/");

        StringBuilder mainDomainBuild = new StringBuilder();
        StringBuilder otherPartOfMainDomain = new StringBuilder();

        for (int i = 0; i < mainDomainParts.length; i++) {
            if (i < 2) {
                mainDomainBuild.append(mainDomainParts[i]).append("/");
            }
            if (i == 2) {
                mainDomainBuild.append(mainDomainParts[i]);
            }
            if (i > 2) {
                otherPartOfMainDomain.append("/").append(mainDomainParts[i]);
            }
        }

        if (currentUrl.startsWith("/")) {
            if (otherPartOfMainDomain.isEmpty()) {
                return new UrlDTO(true, mainDomainBuild + currentUrl, currentUrl);
            } else if (currentUrl.startsWith(otherPartOfMainDomain.toString())) {
                currentUrl = currentUrl.substring(otherPartOfMainDomain.length());
                UrlDTO urlDTO = new UrlDTO(true, mainDomainBuild.append(otherPartOfMainDomain).append(currentUrl).toString(), currentUrl);
                return urlDTO;
            }
        } else {
            currentUrl = DataSet.getSiteService().toStandart(currentUrl);
            currentUrl = DataSet.getSiteService().toNeedProtocol(currentUrl, mainDomain);
            if (currentUrl.startsWith(mainDomain)) {
                UrlDTO urlDTO = new UrlDTO();
                urlDTO.setEnabled(true);
                urlDTO.setUrl(currentUrl);

                currentUrl = currentUrl.replace(mainDomain, "/");
                urlDTO.setLink(currentUrl);
                return urlDTO;
            }
        }
        return new UrlDTO(false, mainDomain, currentUrl);
    }
}
