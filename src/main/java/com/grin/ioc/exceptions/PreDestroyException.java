package com.grin.ioc.exceptions;

public class PreDestroyException extends ServiceInstantiationException {
    public PreDestroyException(String message) {
        super(message);
    }

    public PreDestroyException(String message, Throwable cause) {
        super(message, cause);
    }
}