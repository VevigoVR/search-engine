package searchengine.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UrlDTO {
    private boolean enabled;
    private String url;
    private String link;
}
