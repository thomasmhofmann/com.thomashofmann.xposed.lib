package com.thomashofmann.xposed.lib;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Process;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class XposedModule extends BroadcastReceiver implements IXposedHookLoadPackage {

    protected Map<Integer, Context> applicationContextsByPid = new HashMap<Integer, Context>();
    protected Map<Integer, String> targetPackagesByPid = new HashMap<Integer, String>();
    protected Map<Integer, XC_LoadPackage.LoadPackageParam> loadPackageParamByPid = new HashMap<Integer, XC_LoadPackage.LoadPackageParam>();

    protected Settings settings = new Settings(getModulePackageName());
    protected int notificationId = 100000;


    protected abstract List<String> getTargetPackageNames();

    protected abstract String getModuleDisplayName();

    protected abstract String getModulePackageName();

    protected abstract String getPreferencesChangedAction();

    protected void applicationTerminated(int pid) {
    }

    protected Procedure1<XC_MethodHook.MethodHookParam> hookApplicationContextOnCreateCode = new Procedure1<XC_MethodHook.MethodHookParam>() {
        @Override
        public void apply(XC_MethodHook.MethodHookParam methodHookParam) {
            Logger.v("Executing hooked method " + methodHookParam.method.getName() + " on " + methodHookParam.thisObject + ". Capturing Application Context.");
            Object applicationContextProvider = methodHookParam.thisObject;
            Context applicationContext;
            if (applicationContextProvider instanceof Context) {
                applicationContext = ((Context) applicationContextProvider).getApplicationContext();
                Logger.v("Application context has been captured.");
            } else {
                Logger.w("Could not capture ApplicationContext from «applicationContextProvider». It is not a Context.");
                return;
            }
            Logger.v("ApplicationContext is " + applicationContext);
            setApplicationContext(applicationContext);
        }
    };

    protected Procedure1<XC_MethodHook.MethodHookParam> hookApplicationContextOnTerminateCode = new Procedure1<XC_MethodHook.MethodHookParam>() {
        @Override
        public void apply(XC_MethodHook.MethodHookParam methodHookParam) {
            Logger.d("Executing hooked method " + methodHookParam.method.getName() + " on " + methodHookParam.thisObject + ". Unregistering broadcast receiver for preference changes and cleaning up.");
            Object applicationContextProvider = methodHookParam.thisObject;
            Context applicationContext;
            if (applicationContextProvider instanceof Context) {
                applicationContext = ((Context) applicationContextProvider).getApplicationContext();
                applicationContext.unregisterReceiver(XposedModule.this);
                applicationContextsByPid.remove(Process.myPid());
                loadPackageParamByPid.remove(Process.myPid());
                targetPackagesByPid.remove(Process.myPid());
                applicationTerminated(Process.myPid());
            } else {
                Logger.w("Could not unregister broadcast receiver for preference changes from «applicationContextProvider» and do some further cleanup. It is not a Context.");
            }
        }
    };

    protected Procedure1<XC_MethodHook.MethodHookParam> hookSystemContextCode = new Procedure1<XC_MethodHook.MethodHookParam>() {
        @Override
        public void apply(XC_MethodHook.MethodHookParam methodHookParam) {
            Logger.v("Executing hooked method " + methodHookParam.method.getName() + " on " + methodHookParam.thisObject + ". Capturing System Context.");
            Context context = retrieveContextFromActivityThread();
            setApplicationContext(context);
        }
    };

    protected void setApplicationContext(Context context) {
        int pid = Process.myPid();
        if(applicationContextsByPid.containsKey(pid)) {
            Logger.v("Not setting ApplicationContext for PID " + pid + ". It is already saved.");
            return;
        }
        Logger.v("Setting ApplicationContext for PID " + pid + ". Context is " + context + ".");
        applicationContextsByPid.put(pid, context);
        Logger.v("Registering BroadcastReceiver for action: " + getPreferencesChangedAction());
        context.registerReceiver(XposedModule.this, new IntentFilter(getPreferencesChangedAction()));
        String targetPackage = targetPackagesByPid.get(pid);
        Logger.v("target package for pid " + pid + " is " + targetPackage);
        if (targetPackage == null) {
            createNotification("XposedMod Problem", "No target package found for pid " + pid);
        }
        Logger.d("applicationContextsByPid size is " + applicationContextsByPid.size() + ".");
        applicationContextAvailable(context);
    }

    protected void applicationContextAvailable(Context context) {
    }

    protected Context getApplicationContext() {
        int pid = Process.myPid();
        Context context = applicationContextsByPid.get(pid);
        if (context != null) {
            Logger.d("Request for ApplicationContext for PID " + pid + ". Returning " + context + ".");
        } else {
            Logger.e("Request for ApplicationContext for PID " + pid + ". Context not available. Returning null.");
        }
        return context;
    }

    protected String getTargetPackage() {
        int pid = Process.myPid();
        String targetPackage = targetPackagesByPid.get(pid);
        Logger.d("Request for target package for PID " + pid + ". Returning " + targetPackage + ".");
        return targetPackage;
    }

    protected XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        int pid = Process.myPid();
        XC_LoadPackage.LoadPackageParam loadPackageParam = loadPackageParamByPid.get(pid);
        Logger.d("Request for loadPackageParam for PID " + pid + ". Returning " + loadPackageParam + ".");
        return loadPackageParam;
    }

    protected void captureApplicationContext(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.appInfo == null || loadPackageParam.appInfo.className == null) {
            Logger.w("appInfo in LoadPackageParam is null. Trying to hook ActivityThread to retrieve a system context.");
            hookActivityThread(loadPackageParam);
        } else {
            hookApplication(loadPackageParam);
        }
    }

    private void hookApplication(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        String className = loadPackageParam.appInfo.className;
        ClassLoader classLoader = loadPackageParam.classLoader;
        Logger.d("Setting up hook to capture application context called for class " + className);
        if (loadPackageParam.isFirstApplication) {
            Logger.v(className + " is main application for process " + loadPackageParam.processName);
            Class clazz = null;
            try {
                clazz = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                Logger.e("Can't find class " + className, e);
            }
            BeforeMethodHook onCreateHook = new BeforeMethodHook(this.hookApplicationContextOnCreateCode);
            BeforeMethodHook onTerminateHook = new BeforeMethodHook(this.hookApplicationContextOnTerminateCode);
            if (Activity.class.isAssignableFrom(clazz)) {
                hookMethod(className, loadPackageParam.classLoader, "onCreate", Bundle.class, onCreateHook);
                hookMethod(className, loadPackageParam.classLoader, "onTerminate", onTerminateHook);
            } else if (Service.class.isAssignableFrom(clazz)) {
                hookMethod(className, loadPackageParam.classLoader, "onCreate", onCreateHook);
                hookMethod(className, loadPackageParam.classLoader, "onTerminate", onTerminateHook);
            } else if (Application.class.isAssignableFrom(clazz)) {
                hookMethod(className, loadPackageParam.classLoader, "onCreate", onCreateHook);
                hookMethod(className, loadPackageParam.classLoader, "onTerminate", onTerminateHook);
            } else {
                Logger.w("Don't know how to hook " + className + " to retrieve application context from.");
            }
        }
    }

    private void hookActivityThread(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        AfterMethodHook afterSystemMainHook = new AfterMethodHook(hookSystemContextCode);
        hookMethod("com.android.server.am.ActivityManagerService", loadPackageParam.classLoader, "systemReady", Runnable.class, afterSystemMainHook);
    }

    private Context retrieveContextFromActivityThread() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            Method getSystemContextMethod = activityThreadClass.getDeclaredMethod("getSystemContext");
            Object systemContext = getSystemContextMethod.invoke(activityThread);
            if (systemContext instanceof Context) {
                Logger.v("Got the system Context from the ActivityThread");
                return (Context) systemContext;
            } else {
                Logger.v("Result of getSystemContext method call on ActivityThread is :" + systemContext);
            }
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Logger.w(e.toString());
        }
        return null;
    }

//    protected void hookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
//        hookMethod(className, classLoader, methodName, parameterTypesAndCallback);
//    }

    protected void hookMethod(String className, ClassLoader classLoader, String methodName, Object[] parameterTypesAndCallback) {
        Logger.i("Hooking " + className + "#" + methodName);
        try {
            findAndHookMethodInClassHierarchy(className, classLoader, methodName, parameterTypesAndCallback);
        } catch (Exception e) {
            Logger.e("Failed to hook method " + className + "#" + methodName, e);
        }
    }

//    protected XC_MethodHook.Unhook findAndHookMethodInClassHierarchy(String className, ClassLoader classLoader,
//                                                                     String methodName, Object... parameterTypesAndCallback) {
//            findAndHookMethodInClassHierarchy(className,classLoader,methodName,parameterTypesAndCallback);
//    }

    protected XC_MethodHook.Unhook findAndHookMethodInClassHierarchy(String className, ClassLoader classLoader,
                                                                     String methodName, Object[] parameterTypesAndCallback) {
        Class clazz = XposedHelpers.findClass(className, classLoader);
        boolean notAtObject = clazz != Object.class;
        while (notAtObject) {
            try {
                return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            } catch (NoSuchMethodError e) {
                Class superclass = clazz.getSuperclass();
                notAtObject = superclass != Object.class;
                Logger.d("Trying to hook method " + methodName + " for superclass: " + superclass);
                return findAndHookMethodInClassHierarchy(superclass.getName(), classLoader, methodName,
                        parameterTypesAndCallback);
            }
        }
        throw new NoSuchMethodError(methodName);
    }

    protected Settings getSettings() {
        return settings;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d("Received intent " + intent);
        try {

            if (intent.getAction().equals(getPreferencesChangedAction())) {
                Logger.i("Reloading settings due to broadcast.");
                settings.reload();
                doHandlePreferenceChanges();
            }
        } catch (Throwable t) {
            Logger.e("Exception during Intent processing.", t);
        }
    }

    protected void doHandlePreferenceChanges() {
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        List<String> targets = getTargetPackageNames();
        if (targets == null || !targets.contains(loadPackageParam.packageName)) {
            return;
        }
        int pid = Process.myPid();
        Logger.i("Going to hook application: " + loadPackageParam.packageName + " in PID " + pid);
        targetPackagesByPid.put(pid, loadPackageParam.packageName);
        loadPackageParamByPid.put(pid, loadPackageParam);
        captureApplicationContext(loadPackageParam);
        try {
            doHandleLoadPackage();
        } catch (Exception e) {
            throw new UnexpectedException("Exception from doHandleLoadPackage", e);
        }
    }

    protected abstract void doHandleLoadPackage();

    protected void createNotification(String title, String... details) {
        createNotification(getApplicationContext(), title, details);
    }

    protected int createNotification(Context context, String title, String... details) {
        if (context == null) {
            Logger.w("Cannot create notification without context.");
            return -1;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String detail : details) {
            stringBuilder.append(detail).append(", ");
        }
        Logger.i("Creating notification for '" + title + "-" + stringBuilder.toString() + "'");

        Notification.Builder notificationBuilder = new Notification.Builder(context);
        notificationBuilder.setSmallIcon(getNotificationIconResourceId());
        notificationBuilder.setContentTitle(title);
        if (details.length > 0) {
            String detailsText = details[0].substring(0, Math.min(details.length, 50));
            notificationBuilder.setContentText(detailsText);
            if (details.length > 1) {
                Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
                inboxStyle.setBigContentTitle(title);
                for (String detail : details) {
                    inboxStyle.addLine(detail);
                }
                notificationBuilder.setStyle(inboxStyle);
            }
            PendingIntent pendingIntent = buildIntentForNotification(context, title, details);
            notificationBuilder.setContentIntent(pendingIntent);
        }


        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationBuilder.build());
        notificationId = notificationId + 1;
        return notificationId;
    }

    protected int createNotification(String title, PendingIntent pendingIntent) {
        return createNotification(getApplicationContext(), title, pendingIntent);
    }

    protected int createNotification(Context context, String title, PendingIntent pendingIntent) {
        if (context == null) {
            Logger.w("Cannot create notification without context.");
            return -1;
        }

        Logger.i("Creating notification for " + title + " - with PendingIntent " + pendingIntent);

        Notification.Builder notificationBuilder = new Notification.Builder(context);
        notificationBuilder.setSmallIcon(getNotificationIconResourceId());
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationBuilder.build());
        notificationId = notificationId + 1;
        return notificationId;
    }

    protected int createNotification(UnexpectedException e) {
        return createNotification(getApplicationContext(), e);
    }

    protected int createNotification(Context context, UnexpectedException e, String... details) {
        if (context == null) {
            Logger.w("Cannot create notification without context.");
            return -1;
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(byteStream));
        return createNotification(context, e.toString(), byteStream.toString());
    }

    protected PendingIntent buildIntentForNotification(Context context, String subject, String... details) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        if (details != null) {
            if (details.length > 1) {
                StringBuilder stringBuilder = new StringBuilder();
                for (String detail : details) {
                    stringBuilder.append(detail).append("\n\n");
                }
                intent.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString());
            } else {
                intent.putExtra(Intent.EXTRA_TEXT, details[0]);
            }
        }
        intent.setType("text/plain");
        return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    protected int getNotificationIconResourceId() {
        return 0x7f020000;
    }

    protected void displayToast(String text) {
        displayToast(getApplicationContext(), text);
    }

    protected void displayToast(Context context, String text) {
        if (context == null) {
            Logger.w("Cannot create toast without context.");
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

}
