package searchengine.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import searchengine.dto.entity.PageEntity;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RankDTO {
    private Integer pageId;
    private PageEntity pageEntity;
    private double absRelevance = 0.0;
    private double relativeRelevance = 0.0;
    private double maxLemmaRank = 0;
}
