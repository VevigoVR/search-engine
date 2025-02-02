package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import searchengine.dto.StatusSite;

@Setter
@Getter
@ToString
public class Site {
    private String url;
    private String name;
    private volatile StatusSite status = StatusSite.STOP;
}