package xposed.lib;


import de.robv.android.xposed.XC_MethodHook;

class AfterMethodHook extends MethodHook {

    public AfterMethodHook(Procedure1<XC_MethodHook.MethodHookParam> procedure1) {
        super(procedure1);
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
        Logger.d(getMethodName(methodHookParam) + " called with result " + methodHookParam.getResult());
        procedure1.apply(methodHookParam);
        Logger.d(getMethodName(methodHookParam) + " returns with result " +methodHookParam.getResult());
    }

}