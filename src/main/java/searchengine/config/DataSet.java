package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import searchengine.dto.StatusSite;
import searchengine.dto.response.SearchDataResponse;
import searchengine.services.IndexService;
import searchengine.services.LemmaService;
import searchengine.services.PageService;
import searchengine.services.SiteService;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Component
public class DataSet {
    @Getter
    private static SiteService siteService;
    @Getter
    private static LemmaService lemmaService;
    @Getter
    private static PageService pageService;
    @Getter
    private static IndexService indexService;
    @Getter
    private volatile static List<Site> sites = new ArrayList<>();
    @Getter
    @Setter
    private static List<SearchDataResponse> response = new ArrayList<>();

    public DataSet(SiteService siteService, LemmaService lemmaService, PageService pageService, IndexService indexService) {
        DataSet.siteService = siteService;
        DataSet.lemmaService = lemmaService;
        DataSet.pageService = pageService;
        DataSet.indexService = indexService;
    }

    public static void setDataSet(SitesList sitesList) {
        if (sites.isEmpty()) {
            sites.addAll(sitesList.getSites());
            for (Site site : sites) {
                site.setUrl(siteService.toStandart(site.getUrl()));
            }
        }
    }

    public static boolean isSitesStopping() {
        for (Site site : sites) {
            if (site.getStatus().equals(StatusSite.STOPPING)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSitesStop() {
        for (Site site : sites) {
            if (!site.getStatus().equals(StatusSite.INDEXING)) {
                return true;
            }
        }
        return false;
    }

    public static void setSitesStatus(StatusSite status, StatusSite noChangeStatus) {
        for (Site site : sites) {
            if (!site.getStatus().equals(noChangeStatus)) {
                site.setStatus(status);
            }
        }
    }
}
