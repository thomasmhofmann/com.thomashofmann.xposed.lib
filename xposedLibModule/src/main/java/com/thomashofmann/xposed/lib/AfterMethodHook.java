package com.thomashofmann.xposed.lib;


import de.robv.android.xposed.XC_MethodHook;

public class AfterMethodHook extends MethodHook {

    public AfterMethodHook(Procedure1<XC_MethodHook.MethodHookParam> afterCode) {
        super(null,afterCode);
    }

}