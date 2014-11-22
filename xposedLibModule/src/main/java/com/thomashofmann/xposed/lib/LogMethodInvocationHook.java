package com.thomashofmann.xposed.lib;

import de.robv.android.xposed.XC_MethodHook;

public class LogMethodInvocationHook extends MethodHook {
    public LogMethodInvocationHook() {
        super(null, null);
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
        Logger.d("Executing hook " + getMethodName(methodHookParam) + " called with values " + getMethodParameterString(methodHookParam));
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
        Logger.d(getMethodName(methodHookParam) + " returns with " + methodHookParam.getResult());
    }

}

