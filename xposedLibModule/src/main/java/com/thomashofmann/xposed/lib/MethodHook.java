package com.thomashofmann.xposed.lib;

import de.robv.android.xposed.XC_MethodHook;

public class MethodHook extends XC_MethodHook {
    protected Procedure1<MethodHookParam> beforeCode;
    protected Procedure1<MethodHookParam> afterCode;

    public MethodHook(Procedure1<MethodHookParam> beforeCode, Procedure1<MethodHookParam> afterCode) {
        setBeforeCode(beforeCode);
        setAfterCode(afterCode);
    }

    void setBeforeCode(Procedure1<MethodHookParam> beforeCode) {
        this.beforeCode = beforeCode;
    }

    void setAfterCode(Procedure1<MethodHookParam> afterCode) {
        this.afterCode = afterCode;
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
        if(beforeCode == null) {
            return;
        }
        Logger.d("Executing hook " + getMethodName(methodHookParam) + " called with values " + getMethodParameterString(methodHookParam));
        try {
            beforeCode.apply(methodHookParam);
        } catch(Throwable t) {
            Logger.e(t,"Exception in before hook");
        }
        Logger.d(getMethodName(methodHookParam) + " returns with " + methodHookParam.getResult());
    }

    protected void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
        if(afterCode == null) {
            return;
        }
        Logger.d(getMethodName(methodHookParam) + " called with result " + methodHookParam.getResult());
        try {
            afterCode.apply(methodHookParam);
        } catch(Throwable t) {
            Logger.e(t,"Exception in after hook");
        }
        Logger.d(getMethodName(methodHookParam) + " returns with result " +methodHookParam.getResult());
    }

    protected String getMethodParameterString(XC_MethodHook.MethodHookParam param) {
        StringBuilder stringBuilder = new StringBuilder();
        if (param.args != null && param.args.length > 0) {
            for (Object object : param.args) {
                stringBuilder.append(object);
                stringBuilder.append(',');
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        } else {
            stringBuilder.append("<none>");
        }
        return stringBuilder.toString();
    }
    protected String getMethodName(XC_MethodHook.MethodHookParam param) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(param.method.getDeclaringClass().getSimpleName());
        stringBuilder.append('#');
        stringBuilder.append(param.method.getName());
        return stringBuilder.toString();
    }
}