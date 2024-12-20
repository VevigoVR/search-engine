package searchengine.services.impl;

import lombok.*;
import searchengine.config.DataSet;
import searchengine.config.Site;
import searchengine.dto.StatusSite;
import searchengine.dto.entity.SiteEntity;
import searchengine.services.SiteService;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

@Setter
@Getter
@NoArgsConstructor
public class FindService implements Runnable {

    private SiteEntity siteEntity;
    private SiteService siteService;
    private volatile Set<String> allLinks = new HashSet<>();

    public FindService(SiteEntity siteEntity, SiteService siteService) {
        this.siteEntity = siteEntity;
        this.siteService = siteService;
    }

    @Override
    public void run() {

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        PageFinder pageFinder = new PageFinder(this, siteEntity.getUrl(), siteEntity, forkJoinPool);
        forkJoinPool.invoke(pageFinder);

        for (Site site : DataSet.getSites()) {
            SiteEntity siteEntityFromDB = siteService.findByUrl(siteEntity.getUrl());
            // если останавливается пользователем (StatusSite.STOPPING)
            System.out.println("Остановка: " + site.getUrl() + " - " + siteEntity.getUrl());
            if(site.getUrl().equals(siteEntity.getUrl()) && site.getStatus().equals(StatusSite.STOPPING)) {
                List<Runnable> pool = forkJoinPool.shutdownNow();
                pool.forEach(t -> System.out.println("потоки для закрытия: " + t.toString()));
                pool.clear();
                site.setStatus(StatusSite.FAILED);
                siteEntityFromDB.setStatus(StatusSite.FAILED);
                siteEntityFromDB.setLastError("Остановлено пользователем");
                siteEntityFromDB.setStatusTime(new Date());
                siteService.save(siteEntityFromDB);
                return;
            } else if (site.getUrl().equals(siteEntity.getUrl())
                    && !site.getStatus().equals(StatusSite.STOPPING)) {
                site.setStatus(StatusSite.INDEXED);
                siteEntityFromDB.setStatus(StatusSite.INDEXED);
                siteEntityFromDB.setLastError("");
                siteEntityFromDB.setStatusTime(new Date());
                siteService.save(siteEntityFromDB);
            }
        }
    }
}
