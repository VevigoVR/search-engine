package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.response.SearchResponse;

import java.io.IOException;

public interface SearchService {
    SearchResponse search(String query, String site, int offset, int limit)  throws IOException;
}
