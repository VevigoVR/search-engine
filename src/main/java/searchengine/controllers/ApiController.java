package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.response.SearchResponse;
import searchengine.dto.response.SuccessResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final SiteService siteService;
    private final SearchService searchService;
    private final PageService pageService;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @PostMapping("/indexPage")
    public SuccessResponse indexPage(@RequestParam String url) throws IOException {
        return siteService.indexPage(url);
    }

    @GetMapping("/startIndexing")
    public SuccessResponse startIndexing() throws InterruptedException {
        return siteService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public SuccessResponse stopIndexing() throws InterruptedException {
        return siteService.stopIndexing();
    }

    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit
    ) throws IOException {
        return searchService.search(query, site, offset, limit);
    }
}
