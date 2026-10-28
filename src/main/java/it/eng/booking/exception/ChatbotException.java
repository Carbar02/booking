package it.eng.booking.exception;

import org.springframework.http.HttpStatus;

public class ChatbotException extends RuntimeException {

    private final HttpStatus status;

    public ChatbotException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
