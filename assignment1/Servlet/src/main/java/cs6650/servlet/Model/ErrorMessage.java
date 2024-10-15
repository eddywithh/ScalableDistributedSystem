package cs6650.servlet.Model;

import java.time.LocalDateTime;

public class ErrorMessage {
    private String message;
    private int statusCode;
    private String timestamp;

    public ErrorMessage(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
        this.timestamp = LocalDateTime.now().toString();
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
