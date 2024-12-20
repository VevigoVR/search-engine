package searchengine.dto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="indexes",  uniqueConstraints = @UniqueConstraint(columnNames={"page_id", "lemma_id"}))
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // NOT NULL AUTO_INCREMENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private PageEntity pageId; // INT NOT NULL — идентификатор страницы;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaEntity lemmaId; // INT NOT NULL — идентификатор леммы;

    @Column(name = "ranks", nullable = false)
    private Float rank; // FLOAT NOT NULL — количество данной леммы для данной страницы.

    public int getPageIdInt() {
        return pageId.getId();
    }
}
