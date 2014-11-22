package com.thomashofmann.xposed.lib;

import java.text.MessageFormat;

public class UnexpectedException extends RuntimeException {
    public UnexpectedException(String message, Object... params) {
        this(null, message, params);
        Logger.e(message);
    }

    public UnexpectedException(Throwable cause, String message, Object... params) {
        super(MessageFormat.format(message, params), cause);
        Logger.e(getMessage(), cause);
    }

}
