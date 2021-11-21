package com.grin.ioc.exceptions;

public class PostConstructException extends ServiceInstantiationException {
    public PostConstructException(String message) {
        super(message);
    }

    public PostConstructException(String message, Throwable cause) {
        super(message, cause);
    }
}