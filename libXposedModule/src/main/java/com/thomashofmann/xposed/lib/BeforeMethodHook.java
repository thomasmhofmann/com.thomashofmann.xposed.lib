package com.thomashofmann.xposed.lib;

public class BeforeMethodHook extends MethodHook {

    public BeforeMethodHook(Procedure1<MethodHookParam> code) {
        super(code);
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
        Logger.d("Executing hook " + getMethodName(methodHookParam) + " called with values " + getMethodParameterString(methodHookParam));
        procedure1.apply(methodHookParam);
        Logger.d(getMethodName(methodHookParam) + " returns with " + methodHookParam.getResult());
    }

}