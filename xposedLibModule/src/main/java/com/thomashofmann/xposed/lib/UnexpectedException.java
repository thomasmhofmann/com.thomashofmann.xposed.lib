package com.thomashofmann.xposed.lib;

public class UnexpectedException extends RuntimeException {
    public UnexpectedException(String message) {
        super(message);
        Logger.e(message);
    }

    public UnexpectedException(String message, Throwable cause) {
        super(message, cause);
        Logger.e(message, cause);
    }

}
