package xposed.lib;


import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.robv.android.xposed.XposedBridge;

public class Logger {
    static Settings settings;
    static String tag;

    static public void init(String tag, Settings settings) {
        XposedBridge.log("init Logger with tag " + tag);
        Logger.tag = tag;
        Logger.settings = settings;
    }

    static public void d(String message) {
        Logger.d(message, null);
    }

    static public void d(String message, Throwable throwable) {
        if (throwable == null) {
            Log.d(tag, message);
        } else {
            Log.d(tag, message + '\n' + getStackTraceString(throwable));
        }
//        Logger.log(message, throwable, new Procedure2<String, Throwable>() {
//            @Override
//            public void apply(String message, Throwable throwable) {
//                if (throwable == null) {
//                    Log.d(tag, message);
//                } else {
//                    Log.d(tag, message + '\n' + getStackTraceString(throwable));
//                }
//            }
//        });
    }

    static public void v(String message) {
        Logger.v(message, null);
    }

    static public void v(String message, Throwable throwable) {
        Logger.log(message, throwable, new Procedure2<String, Throwable>() {
            @Override
            public void apply(String message, Throwable throwable) {
                if (throwable == null) {
                    Log.v(tag, message);
                } else {
                    Log.v(tag, message + '\n' + getStackTraceString(throwable));
                }
            }
        });
    }

    static public void i(String message) {
        Logger.i(message, null);
    }

    static public void i(String message, Throwable throwable) {
        Logger.log(message, throwable, new Procedure2<String, Throwable>() {
            @Override
            public void apply(String message, Throwable throwable) {
                if (throwable == null) {
                    Log.i(tag, message);
                } else {
                    Log.i(tag, message + '\n' + getStackTraceString(throwable));
                }
            }
        });

    }

    static public void w(String message) {
        Logger.w(message, null);
    }

    static public void w(String message, Throwable throwable) {
        Logger.log(message, throwable, new Procedure2<String, Throwable>() {
            @Override
            public void apply(String message, Throwable throwable) {
                if (throwable == null) {
                    Log.w(tag, message);
                } else {
                    Log.w(tag, message + '\n' + getStackTraceString(throwable));
                }
            }
        });
    }

    static public void e(String message) {
        Logger.e(message, null);
    }

    static public void e(String message, Throwable throwable) {
        Logger.log(message, throwable, new Procedure2<String, Throwable>() {
            @Override
            public void apply(String message, Throwable throwable) {
                if (throwable == null) {
                    Log.e(tag, message);
                } else {
                    Log.e(tag, message + '\n' + getStackTraceString(throwable));
                }
            }
        });
    }

    static void log(String message, Throwable t, Procedure2<String, Throwable> code) {
        if (shouldLog()) {
            code.apply(message, t);
        }
        if (shouldLogToXposedLog()) {
            logToXposedLog(message, t);
        }
    }

    static void logToXposedLog(String message, Throwable t) {
        if (t == null) {
            appendToLog(tag, message);
        } else {
            appendToLog(tag, message + "\n" + getStackTraceString(t));
        }
    }

    static void appendToLog(String tag, String message) {
        XposedBridge.log(tag + ": " + message);
    }

    private static boolean shouldLog() {
        return Logger.settings != null && Logger.settings.getPreferences().getBoolean("pref_logging_enabled_state", false);
    }

    private static boolean shouldLogToXposedLog() {
        return Logger.settings != null && Logger.settings.getPreferences().getBoolean("pref_log_to_xposed_log_state", true);
    }

    private static String getStackTraceString(Throwable t) {
        if (t == null) {
            return "";
        }
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        return stringWriter.toString();
    }
}
