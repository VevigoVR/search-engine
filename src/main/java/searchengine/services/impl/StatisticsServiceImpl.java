package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.entity.SiteEntity;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.services.LemmaService;
import searchengine.services.PageService;
import searchengine.services.SiteService;
import searchengine.services.StatisticsService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final PageService pageService;
    private final SiteService siteService;
    private final LemmaService lemmaService;

    @Override
    public StatisticsResponse getStatistics() {

        List<SiteEntity> siteEntities = siteService.findAll();

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteEntities.size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        for (int i = 0; i < siteEntities.size(); i++) {
            SiteEntity site = siteEntities.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            long pages = pageService.countBySiteEntityId(site);
            long lemmas = lemmaService.countBySiteEntityId(site);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getStatus().name());
            item.setError(site.getLastError() == null ? "" : site.getLastError());
            item.setStatusTime(site.getStatusTime().getTime());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
            log.info("Статус сайта: " + site.getStatus().name());
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
