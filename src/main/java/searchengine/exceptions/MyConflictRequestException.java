package searchengine.exceptions;

public class MyConflictRequestException extends RuntimeException {
    public MyConflictRequestException(String message) {
        super(message);
    }
}
