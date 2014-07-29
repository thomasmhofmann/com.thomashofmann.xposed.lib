package xposed.lib;

class UnexpectedException extends RuntimeException {
    UnexpectedException(String message) {
        super(message);
        Logger.e(message);
    }

    UnexpectedException(String message, Throwable cause) {
        super(message, cause);
        Logger.e(message, cause);
    }

}
