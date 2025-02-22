package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.services.impl.LemmaFinder;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchDTO {
    private String sentence;
    private List<String> simpleLemmasFromSearch;
    private LemmaFinder lemmaFinder;
    private StringBuilder textFromElement;
}
