package searchengine.services.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import searchengine.config.DataSet;
import searchengine.config.SitesList;
import searchengine.dto.StatusSite;
import searchengine.dto.response.SearchDataResponse;
import searchengine.services.LemmaService;
import searchengine.services.PageService;
import searchengine.services.SiteService;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Getter
public class DataSetService implements CommandLineRunner {
    private final SitesList sites;
    private final SiteService siteService;
    private final LemmaService lemmaService;
    private final PageService pageService;

    @Override
    public void run(String... args) {
        //siteService.deleteAll();
        //siteService.saveAll(StatusSite.STOP);
        DataSet.setDataSet(sites);
    }
}
