package org.restler.client;

public class RestlerException extends RuntimeException {

    public RestlerException(Throwable cause) {
        super(cause);
    }

    public RestlerException(String message) {
        super(message);
    }

    public RestlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
