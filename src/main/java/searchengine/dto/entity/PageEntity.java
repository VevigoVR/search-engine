package searchengine.dto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "page", uniqueConstraints = @UniqueConstraint(columnNames = {"path", "site_id"}))
@Getter
@Setter
@NoArgsConstructor
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // INT NOT NULL AUTO_INCREMENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteEntityId; // INT NOT NULL — ID веб-сайта из таблицы site;

    @Column(name = "path", nullable = false, length = 512)
    private String path; // TEXT NOT NULL — адрес страницы от корня сайта (должен начинаться со слэша, например: /news/372189/);

    @Column(name = "code", nullable = false)
    private Integer code; // INT NOT NULL — код HTTP-ответа, полученный при запросе страницы (например, 200, 404, 500 или другие);

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content; // MEDIUMTEXT NOT NULL — контент страницы (HTML-код).

    @OneToMany(mappedBy = "pageId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IndexEntity> indexList;
}
