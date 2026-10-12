package com.thinh.shortener.exception;

public class UrlDisabledException extends RuntimeException {
    public UrlDisabledException(String message) {
        super(message);
    }
}
