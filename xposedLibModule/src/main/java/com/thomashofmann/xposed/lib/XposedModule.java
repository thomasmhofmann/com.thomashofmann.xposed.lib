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
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class XposedModule implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {
    protected static final int MODULE_DRAWABLE_START_ID = 0x7F02FF00;
    protected static final int MODULE_DRAWABLE_NOTIFICATION_ICON = MODULE_DRAWABLE_START_ID + 1;

    protected Map<Integer, Context> applicationContextsByPid = new HashMap<Integer, Context>();
    protected Map<Integer, String> targetPackagesByPid = new HashMap<Integer, String>();
    protected Map<Integer, XC_LoadPackage.LoadPackageParam> loadPackageParamByPid = new HashMap<Integer, XC_LoadPackage.LoadPackageParam>();

    protected Settings settings = new Settings(getModulePackageName());
    protected int notificationId = 100000;
    private String modulePath;

    private BroadcastReceiver preferencesChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d("Received intent {0}", intent);
            try {
                if (intent.getAction().equals(getPreferencesChangedAction())) {
                    Logger.i("Reloading settings due to broadcast.");
                    settings.reload();
                    doHandlePreferenceChanges();
                }
            } catch (Throwable t) {
                Logger.e(t, "Exception during Intent processing.");
            }
        }
    };

    protected abstract List<String> getTargetPackageNames();

    protected abstract String getModuleDisplayName();

    protected abstract String getModulePackageName();

    protected abstract String getPreferencesChangedAction();

    protected void applicationTerminating(int pid) {
    }

    protected Procedure1<XC_MethodHook.MethodHookParam> hookApplicationContextOnCreateCode = new Procedure1<XC_MethodHook.MethodHookParam>() {
        @Override
        public void apply(XC_MethodHook.MethodHookParam methodHookParam) {
            Logger.v("Executing hooked method {0} on {1}. Capturing Application Context.", methodHookParam.method.getName(), methodHookParam.thisObject);
            Object applicationContextProvider = methodHookParam.thisObject;
            Context applicationContext;
            if (applicationContextProvider instanceof Context) {
                applicationContext = ((Context) applicationContextProvider).getApplicationContext();
                Logger.v("Application context has been captured.");
            } else {
                Logger.w("Could not capture ApplicationContext from {0}. It is not a Context.", applicationContextProvider);
                return;
            }
            Logger.v("ApplicationContext is {0}", applicationContext);
            setApplicationContext(applicationContext);
        }
    };

    protected Procedure1<XC_MethodHook.MethodHookParam> hookApplicationContextOnTerminateCode = new Procedure1<XC_MethodHook.MethodHookParam>() {
        @Override
        public void apply(XC_MethodHook.MethodHookParam methodHookParam) {
            Logger.d("Executing hooked method {0} on {1}. Unregistering broadcast receiver for preference changes and cleaning up.", methodHookParam.method.getName(), methodHookParam.thisObject);
            Object applicationContextProvider = methodHookParam.thisObject;
            Context applicationContext;
            if (applicationContextProvider instanceof Context) {
                applicationContext = ((Context) applicationContextProvider).getApplicationContext();
                applicationContext.unregisterReceiver(preferencesChangedReceiver);
                applicationContextsByPid.remove(Process.myPid());
                loadPackageParamByPid.remove(Process.myPid());
                targetPackagesByPid.remove(Process.myPid());
                applicationTerminating(Process.myPid());
            } else {
                Logger.w("Could not unregister broadcast receiver for preference changes from {0} and do some further cleanup. It is not a Context.", applicationContextProvider);
            }
        }
    };

    protected Procedure1<XC_MethodHook.MethodHookParam> hookSystemContextCode = new Procedure1<XC_MethodHook.MethodHookParam>() {
        @Override
        public void apply(XC_MethodHook.MethodHookParam methodHookParam) {
            Logger.v("Executing hooked method {0} on {1}. Capturing System Context.", methodHookParam.method.getName(), methodHookParam.thisObject);
            Context context = retrieveSystemContextFromActivityThread();
            setApplicationContext(context);
        }
    };

    protected Procedure1<XC_MethodHook.MethodHookParam> hookFirstContextImplCreationCode = new Procedure1<XC_MethodHook.MethodHookParam>() {
        @Override
        public void apply(XC_MethodHook.MethodHookParam methodHookParam) {
            Logger.v("Executing hooked method {0} on {1}. Capturing some Context. Stacktrace is {2}", methodHookParam.method.getName(), methodHookParam.thisObject, getCurrentStackTraceString());
            Context context = (Context) methodHookParam.thisObject;
            Logger.d("Captured Context " + context);
            if(context != null) {
                Logger.d("Captured Context for package " + context.getPackageName());
                Resources resources = context.getResources();
                Drawable drawable = null;
                if (resources != null) {
                    drawable = resources.getDrawable(getNotificationIconResourceId());
                    Logger.d("Drawable for notification icon from Context: " + drawable);
                }
                int pid = Process.myPid();
                Context existingContext = applicationContextsByPid.get(pid);
                if (existingContext == null || drawable != null) {
                    setApplicationContext(context);
                }
            }
        }
    };

    protected void setApplicationContext(Context context) {
        int pid = Process.myPid();
        if (applicationContextsByPid.containsKey(pid)) {
            Logger.v("Not setting ApplicationContext for PID {0}. It is already saved.", pid);
            return;
        }
        Logger.v("Setting ApplicationContext for PID {0}. Context is {1}", pid, context);
        applicationContextsByPid.put(pid, context);
        Logger.v("Registering BroadcastReceiver for action {0}", getPreferencesChangedAction());
        context.registerReceiver(preferencesChangedReceiver, new IntentFilter(getPreferencesChangedAction()));
        String targetPackage = targetPackagesByPid.get(pid);
        if (targetPackage == null) {
            Logger.e("No target package found for pid " + pid);
        }
        Logger.d("applicationContextsByPid size is {0}", applicationContextsByPid.size());
        applicationContextAvailable(context);
    }

    protected void applicationContextAvailable(Context context) {
    }

    protected Context getApplicationContext() {
        int pid = Process.myPid();
        Context context = applicationContextsByPid.get(pid);
        if (context == null) {
            throw new UnexpectedException("Application context is not available for PID {0}.",  pid);
        }
        return context;
    }

    protected boolean isApplicationContextAvailable() {
        return applicationContextsByPid.containsKey(Process.myPid());
    }

    protected String getTargetPackage() {
        int pid = Process.myPid();
        String targetPackage = targetPackagesByPid.get(pid);
        if(targetPackage == null) {
            throw new UnexpectedException("Target Package is not available for PID {0}.",  pid);
        }
        return targetPackage;
    }


    protected XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        int pid = Process.myPid();
        XC_LoadPackage.LoadPackageParam loadPackageParam = loadPackageParamByPid.get(pid);
        if(loadPackageParam == null) {
            throw new UnexpectedException("loadPackageParam is not available for PID {0}.",  pid);
        }
        return loadPackageParam;
    }

    private void captureApplicationContext(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.appInfo == null || loadPackageParam.appInfo.className == null) {
            Logger.w("appInfo in LoadPackageParam is null.");
            //Logger.w("appInfo in LoadPackageParam is null. Trying to hook first ContextImpl creation to retrieve a context.");
            //hookFirstContextCreation(loadPackageParam);
        } else {
            hookApplication(loadPackageParam);
        }
    }

    private void hookApplication(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        String className = loadPackageParam.appInfo.className;
        ClassLoader classLoader = loadPackageParam.classLoader;
        Logger.d("Setting up hook to capture application context called for class {0}", className);
        if (loadPackageParam.isFirstApplication) {
            Logger.v("{0} is main application for process {1}", className, loadPackageParam.processName);
            Class clazz = null;
            try {
                clazz = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                Logger.e(e, "Can't find class {0}", className);
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
                Logger.w("Don't know how to hook {0} to retrieve application context from.", className);
            }
        }
    }

    private void hookFirstContextCreation(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        ClassLoader classLoader = loadPackageParam.classLoader;
        Class contextImplClass = XposedHelpers.findClass("android.app.ContextImpl", classLoader);
        Logger.d("Setting up hook to capture first Context creation");
        XposedBridge.hookAllConstructors(contextImplClass, new AfterMethodHook(hookFirstContextImplCreationCode));
        //hookMethod("android.app.ContextImpl", classLoader, "createPackageContextAsUser", String.class, int.class, UserHandle.class, new AfterMethodHook(hookFirstContextImplCreationCode));
    }

    private void hookActivityThread(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        AfterMethodHook afterSystemMainHook = new AfterMethodHook(hookSystemContextCode);
        hookMethod("com.android.server.am.ActivityManagerService", loadPackageParam.classLoader, "systemReady", Runnable.class, afterSystemMainHook);
    }

    protected Context retrieveSystemContextFromActivityThread() {
        try {
            Class<?> activityThreadClass = Class.forName("and\n" +
                    "BUILD SUCCESSFULroid.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            Method getSystemContextMethod = activityThreadClass.getDeclaredMethod("getSystemContext");
            Object systemContext = getSystemContextMethod.invoke(activityThread);
            if (systemContext instanceof Context) {
                Logger.v("Got the system Context from the ActivityThread");
                return (Context) systemContext;
            } else {
                Logger.v("Result of getSystemContext method call on ActivityThread is {0}", systemContext);
            }
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Logger.w(e.toString());
        }
        return null;
    }

    protected void hookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        Logger.i("Hooking {0}#{1}", className, methodName);
        try {
            findAndHookMethodInClassHierarchy(className, classLoader, methodName, parameterTypesAndCallback);
        } catch (Exception e) {
            Logger.e(e, "Failed to hook method {0}#{1}", className, methodName);
        }
    }

    protected XC_MethodHook.Unhook findAndHookMethodInClassHierarchy(String className, ClassLoader classLoader,
                                                                     String methodName, Object... parameterTypesAndCallback) {
        Class clazz = XposedHelpers.findClass(className, classLoader);
        boolean notAtObject = clazz != Object.class;
        while (notAtObject) {
            try {
                return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
            } catch (NoSuchMethodError e) {
                Class superclass = clazz.getSuperclass();
                notAtObject = superclass != Object.class;
                Logger.d("Trying to hook method {0} for superclass {1}", methodName, superclass);
                return findAndHookMethodInClassHierarchy(superclass.getName(), classLoader, methodName,
                        parameterTypesAndCallback);
            }
        }
        throw new NoSuchMethodError(methodName);
    }

    public Settings getSettings() {
        return settings;
    }

    protected void doHandlePreferenceChanges() {
    }

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        modulePath = startupParam.modulePath;
        try {
            doInitZygote(startupParam);
        } catch (Exception e) {
            throw new UnexpectedException("Exception from doInitZygote", e);
        }
    }

    protected void doInitZygote(IXposedHookZygoteInit.StartupParam startupParam) {
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        Logger.d("handleInitPackageResources called for " + resparam.packageName);
        List<String> targets = getTargetPackageNames();
        if (targets == null || !targets.contains(resparam.packageName)) {
            return;
        }
        Logger.d("Creating moduleResources");
        XModuleResources moduleResources = XModuleResources.createInstance(modulePath, resparam.res);
        resparam.res.setReplacement(MODULE_DRAWABLE_NOTIFICATION_ICON, moduleResources.fwd(getNotificationIconResourceId()));
        Logger.d("Set resource replacement for " + MODULE_DRAWABLE_NOTIFICATION_ICON + " to " + getNotificationIconResourceId());
        try {
            doHandleInitPackageResources(resparam, moduleResources);
        } catch (Exception e) {
            throw new UnexpectedException("Exception from doHandleInitPackageResources", e);
        }
    }

    protected void doHandleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam, XModuleResources moduleResources) {
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        List<String> targets = getTargetPackageNames();
        if (targets == null || !targets.contains(loadPackageParam.packageName)) {
            return;
        }
        int pid = Process.myPid();
        Logger.i("Going to hook application {0} in PID {1}", loadPackageParam.packageName, pid);
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

    protected Notification.Builder createSimpleNotification(Context context, String contentTitle, String contentText, String subText) {
        Logger.i("createSimpleNotification with title {0}, text {1}, subText {2}", contentTitle, contentText, subText);
        Notification.Builder builder = new Notification.Builder(context);
        builder.setSmallIcon(MODULE_DRAWABLE_NOTIFICATION_ICON).
                setContentTitle(contentTitle).
                setContentText(contentText).
                setSubText(subText);
        return builder;
    }

    protected int createAndShowSimpleNotification(Context context, String contentTitle, String contentText, String subText) {
        Notification.Builder notification = createSimpleNotification(context, contentTitle, contentText, subText);
        return showNotification(context, notification);
    }

    protected Notification.Builder createBigTextStyleNotification(Context context, String contentTitle, String contentText, String subText,
                                                                  String bigTextTitle, String bigText, String bigTextSummary) {
        Logger.i("createBigTextStyleNotification with title {0}, text {1}, subText {2}, bigTextTitle {3}, bigText {4}, bigTextSummary {5}",
                contentTitle, contentText, subText, bigTextTitle, bigText, bigTextSummary);
        Notification.Builder builder = new Notification.Builder(context);
        builder.setSmallIcon(MODULE_DRAWABLE_NOTIFICATION_ICON).
                setContentTitle(contentTitle).
                setContentText(contentText).
                setSubText(subText);
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        if (bigTextTitle != null) {
            bigTextStyle.setBigContentTitle(bigTextTitle);
        }
        bigTextStyle.bigText(bigText);
        bigTextStyle.setSummaryText(bigTextSummary);
        builder.setStyle(bigTextStyle);
        return builder;
    }

    protected Notification.Builder createInboxStyleNotification(Context context, String contentTitle, String contentText, String subText,
                                                                String inboxSummary, String... inboxContents) {
        Logger.i("createBigTextStyleNotification with title {0}, text {1}, subText {2}, inboxSummary {3}",
                contentTitle, contentText, subText, inboxSummary);
        Notification.Builder builder = new Notification.Builder(context);
        builder.setSmallIcon(MODULE_DRAWABLE_NOTIFICATION_ICON).
                setContentTitle(contentTitle).
                setContentText(contentText).
                setSubText(subText);
        if (inboxContents != null) {
            Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
            inboxStyle.setSummaryText(inboxSummary);
            for (String line : inboxContents) {
                inboxStyle.addLine(line);
            }
            builder.setStyle(inboxStyle);
        }
        return builder;
    }

    protected int showNotification(Context context, Notification.Builder notificationBuilder) {
        return showNotification(context, notificationBuilder, -1);
    }

    protected int showNotification(Context context, Notification.Builder notificationBuilder, int notificationId) {
        try {
            NotificationManager notificationManager = getNotificationManager(context);
            if (notificationId == -1) {
                notificationId = this.notificationId;
            }
            notificationManager.notify(notificationId, notificationBuilder.build());
            this.notificationId = this.notificationId + 1;
            return notificationId;
        } catch(Throwable t) {
            Logger.e(t, "Unable to show notification");
            return -1;
        }
    }

    public int createAndShowNotification(Context context, String contentTitle, UnexpectedException e) {
        Logger.i("createAndShowNotification with title {0}, exception {1}",
                contentTitle, e);
        String stackTrace = getStackTraceString(e);
        Notification.Builder notification = createBigTextStyleNotification(context, contentTitle, e.getMessage(), "Expand for details", null, stackTrace, "Stacktrace");
        PendingIntent pendingIntent = buildActionSendPendingIntent(context, contentTitle, e.getMessage(), stackTrace);
        notification.setContentIntent(pendingIntent);
        return showNotification(context,notification);
    }

    protected String getCurrentStackTraceString() {
        return getStackTraceString(new RuntimeException("Determine Stacktrace"));
    }

    protected String getStackTraceString(Throwable t) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(byteStream));
        return byteStream.toString();
    }

    protected PendingIntent buildActionSendPendingIntent(Context context, String subject, String... details) {
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

    public int getNotificationIconResourceId() {
        return 0x7f020000;
    }

    protected NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void displayToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

}
