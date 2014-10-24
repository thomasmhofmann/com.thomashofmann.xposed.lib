package com.thomashofmann.xposed.lib;


import de.robv.android.xposed.XC_MethodHook;

public class ReturnFixedValueForSpecificStringParameterHook  extends MethodHook{
    private Object fixedValue;
    private String parameter;
    private int paramPosition;

    public ReturnFixedValueForSpecificStringParameterHook(String parameter, int paramPosition, Object fixedValue) {
        super(null);
        this.parameter = parameter;
        this.paramPosition = paramPosition;
        this.fixedValue = fixedValue;
    }

    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
        if(methodHookParam.args.length > 0 ) {
            Object value = methodHookParam.args[paramPosition];
            if(value instanceof String && value == parameter) {
                methodHookParam.setResult(fixedValue);
            }
        }
    }
}
