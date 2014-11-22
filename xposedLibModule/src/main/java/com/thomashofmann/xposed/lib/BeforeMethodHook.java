package com.thomashofmann.xposed.lib;

public class BeforeMethodHook extends MethodHook {

    public BeforeMethodHook(Procedure1<MethodHookParam> beforeCode) {
        super(beforeCode,null);
    }

}