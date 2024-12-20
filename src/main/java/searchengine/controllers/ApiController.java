package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.CheckLink;
import searchengine.dto.response.ErrorResponse;
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
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(@RequestParam String url) throws IOException {
        CheckLink checkUrl = siteService.checkUrlAndSetLink(url);
        if (!checkUrl.isResult()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(new ErrorResponse("Данная страница находится за пределами сайтов указанных в конфигурационном файле"));
        }
        CheckLink checkLink = pageService.checkLink(checkUrl);
        System.out.println("checkLink result: " + checkLink.isResult());
        if (!checkLink.isResult()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(new ErrorResponse("Ссылка не соответствует параметрам"));
        }
        pageService.indexPage(checkLink);
        return ResponseEntity.status(HttpStatus.OK).body(new SuccessResponse());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() throws InterruptedException {
        log.info("start of method startIndexing()");
        return siteService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() throws InterruptedException {
        log.info("start of method stopIndexing()");
        return siteService.stopIndexing();
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "5") Integer limit
    ) throws IOException {
        System.out.println("query: " + query);
        System.out.println("site: " + site);
        System.out.println("offset: " + offset);
        System.out.println("limit: " + limit);
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Задан пустой поисковый запрос"));
        }
        return searchService.search(query, site, offset, limit);
    }
}
