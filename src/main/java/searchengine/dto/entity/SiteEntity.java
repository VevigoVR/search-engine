package searchengine.dto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.dto.StatusSite;

import java.util.Date;
import java.util.List;

@Entity
@Table(name="site")
@Getter
@Setter
@NoArgsConstructor
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusSite status;

    @Column(name = "status_time", nullable = false)
    private Date statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", nullable = false, length = 255, unique = true)
    private String url;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @OneToMany(mappedBy = "siteEntityId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PageEntity> pages;

    @OneToMany(mappedBy = "siteEntityId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LemmaEntity> lemmas;
}
