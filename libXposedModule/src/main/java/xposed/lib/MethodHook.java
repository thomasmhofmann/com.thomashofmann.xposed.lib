package xposed.lib;

import de.robv.android.xposed.XC_MethodHook;

class MethodHook extends XC_MethodHook {
    protected Procedure1<MethodHookParam> procedure1;

    public MethodHook(Procedure1<MethodHookParam> procedure1) {
        setCode(procedure1);
    }

    void setCode(Procedure1<MethodHookParam> procedure1) {
        this.procedure1 = procedure1;
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