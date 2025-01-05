package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ParseDTO {
    private Elements elements;
    private Document doc;
    private int code;
}
