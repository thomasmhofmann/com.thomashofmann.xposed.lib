package com.thomashofmann.xposed.lib;


public class ReturnFixedValueHook extends MethodHook {
    private Object fixedValue;

    public ReturnFixedValueHook(Object fixedValue) {
        super(null, null);
        this.fixedValue = fixedValue;
    }

    protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
        try {
            Logger.v("Executing hooked method " + methodHookParam.method.getName() + ". Returning fixed value " + fixedValue);
            methodHookParam.setResult(fixedValue);
        } catch (Throwable t) {
            methodHookParam.setThrowable(t);
        }
    }
}
