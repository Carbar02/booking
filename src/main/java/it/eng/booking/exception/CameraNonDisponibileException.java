package it.eng.booking.exception;

public class CameraNonDisponibileException extends RuntimeException {
    public CameraNonDisponibileException(String message) {
        super(message);
    }
}