package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.dto.entity.PageEntity;
import searchengine.dto.entity.SiteEntity;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    Boolean existsByPath(String path);
    Long countBySiteEntityId(SiteEntity siteEntity);
    Optional<PageEntity> findByPath(String path);
    Optional<PageEntity> findByPathAndSiteEntityId(String path, SiteEntity siteEntity);
}
