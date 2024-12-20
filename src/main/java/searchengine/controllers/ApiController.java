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
        if (!checkLink.isResult()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(new ErrorResponse("Ссылка не соответствует параметрам"));
        }
        pageService.indexPage(checkLink);
        return ResponseEntity.status(HttpStatus.OK).body(new SuccessResponse());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() throws InterruptedException {
        return siteService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() throws InterruptedException {
        return siteService.stopIndexing();
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String site,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) throws IOException {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Задан пустой поисковый запрос"));
        }
        return searchService.search(query, site, offset, limit);
    }
}
