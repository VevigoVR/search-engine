package searchengine.dto.response;

import lombok.Data;

@Data
public class ErrorResponse {
    private final boolean result = false;
    private final String error;
}
