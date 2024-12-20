package searchengine.dto;

import lombok.*;
import searchengine.dto.entity.SiteEntity;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CheckLink {
    private SiteEntity siteEntity;
    private String link;
    private String fullLink;
    private boolean result;
}
