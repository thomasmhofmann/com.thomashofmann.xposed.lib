package com.thomashofmann.xposed.lib;


import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;

import de.robv.android.xposed.XposedBridge;

public class Logger {
    static Settings settings;
    static String tag;

    static public void init(String tag, Settings settings) {
        XposedBridge.log("init Logger with tag " + tag);
        Logger.tag = tag;
        Logger.settings = settings;
    }


    static public void v(String message) {
        Logger.v(null, message, (Object[])null);
    }

    static public void v(String message, Object... params) {
        Logger.v(null, message, params);
    }

    static public void v(Throwable throwable, String message, Object... params) {
        if(params != null) {
            message = MessageFormat.format(message, params);
        }

        if(shouldLogToXposedLogVerbose()) {
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

        } else {
            if (throwable == null) {
                Log.v(tag, message);
            } else {
                Log.v(tag, message + '\n' + getStackTraceString(throwable));
            }
        }
    }

    static public void d(String message) {
        Logger.d(null, message, (Object[])null);
    }

    static public void d(String message, Object... params) {
        Logger.d(null, message, params);
    }

    static public void d(Throwable throwable, String message, Object... params) {
        if(params != null) {
            message = MessageFormat.format(message, params);
        }

        if(shouldLogToXposedLogDebug()) {
            Logger.log(message, throwable, new Procedure2<String, Throwable>() {
                @Override
                public void apply(String message, Throwable throwable) {
                    if (throwable == null) {
                        Log.d(tag, message);
                    } else {
                        Log.d(tag, message + '\n' + getStackTraceString(throwable));
                    }
                }
            });

        } else {
            if (throwable == null) {
                Log.d(tag, message);
            } else {
                Log.d(tag, message + '\n' + getStackTraceString(throwable));
            }
        }
    }

    static public void i(String message) {
        Logger.i(null, message, (Object[])null);
    }

    static public void i(String message, Object... params) {
        Logger.i(null, message, params);
    }

    static public void i(Throwable throwable, String message, Object... params) {
        if(params != null) {
            message = MessageFormat.format(message, params);
        }

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
        Logger.w(null, message, (Object[])null);
    }

    static public void w(String message, Object... params) {
        Logger.w(null, message, params);
    }

    static public void w(Throwable throwable, String message, Object... params) {
        if(params != null) {
            message = MessageFormat.format(message, params);
        }

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
        Logger.e(null, message, (Object[])null);
    }

    static public void e(String message, Object... params) {
        Logger.e(null, message, params);
    }

    static public void e(Throwable throwable, String message, Object... params) {
        if(params != null) {
            message = MessageFormat.format(message, params);
        }

        Logger.log(message, throwable, true, new Procedure2<String, Throwable>() {
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
        log(message,t, false, code);
    }

    static void log(String message, Throwable t, boolean forceLogging, Procedure2<String, Throwable> code) {
        if (shouldLog()|| forceLogging) {
            code.apply(message, t);
        }
        if (shouldLogToXposedLog()|| forceLogging) {
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
        try {
            XposedBridge.log(tag + ": " + message);
        } catch(Throwable t) {
            Log.e("xposed.lib", "Unable to log to Xposed log due to: " + t + "\nThis is only supported from within a module.");
        }
    }

    private static boolean shouldLog() {
        return Logger.settings != null && Logger.settings.getPreferences().getBoolean("pref_logging_enabled_state", false);
    }

    private static boolean shouldLogToXposedLog() {
        return Logger.settings != null && Logger.settings.getPreferences().getBoolean("pref_log_to_xposed_log_state", false);
    }

    private static boolean shouldLogToXposedLogDebug() {
        return Logger.settings != null && Logger.settings.getPreferences().getBoolean("pref_log_to_xposed_log_debug_state", false);
    }

    private static boolean shouldLogToXposedLogVerbose() {
        return Logger.settings != null && Logger.settings.getPreferences().getBoolean("pref_log_to_xposed_log_verbose_state", false);
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
