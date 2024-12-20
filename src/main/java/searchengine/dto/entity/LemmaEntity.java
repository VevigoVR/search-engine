package searchengine.dto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="lemma",  uniqueConstraints = @UniqueConstraint(columnNames={"site_id", "lemma"}))

public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // INT NOT NULL AUTO_INCREMENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity siteEntityId; // INT NOT NULL — ID веб-сайта из таблицы site;

    @Column(name = "lemma", nullable = false)
    private String lemma; // VARCHAR(255) NOT NULL — нормальная форма слова (лемма);

    @Column(name = "frequency", nullable = false)
    private Integer frequency; // INT NOT NULL — количество страниц, на которых слово встречается хотя бы один раз. Максимальное значение не может превышать общее количество слов на сайте.

    @OneToMany(mappedBy = "lemmaId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IndexEntity> indexList;
}
