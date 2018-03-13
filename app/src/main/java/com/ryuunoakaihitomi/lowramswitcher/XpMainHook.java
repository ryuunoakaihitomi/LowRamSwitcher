package com.ryuunoakaihitomi.lowramswitcher;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * Xposed Main Hook
 * Created by ZQY on 2018/3/13.
 */

public class XpMainHook implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    //buid.prop config
    private final String LOW_RAM_TAG = "ro.config.low_ram";
    //real value of getprop ro.config.low_ram
    private boolean isLowRAM;
    //arg:str key,(str def)
    private final XC_MethodHook SOLUTION_FILTER = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (LOW_RAM_TAG.equals(param.args[0]))
                param.setResult(!isLowRAM);
        }
    };
    //arg:null,only reverse
    private final XC_MethodReplacement SOLUTION_RETURN = XC_MethodReplacement.returnConstant(!isLowRAM);

    @Override
    public void initZygote(final StartupParam startupParam) throws Throwable {
        //init the real value
        isLowRAM = Boolean.valueOf(System.getProperty(LOW_RAM_TAG));
        //-> system
        Class<?> spClazz = findClass("android.os.SystemProperties", null);
        findAndHookMethod(spClazz, "getBoolean", String.class, boolean.class, SOLUTION_FILTER);
        //for Android 4.4
        findAndHookMethod(spClazz, "get", String.class, String.class, SOLUTION_FILTER);
    }

    //->app
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        hookAllMethods(findClass("java.lang.System", lpparam.classLoader), "getProperty", SOLUTION_FILTER);
        Class<?> amClazz = findClass("android.app.ActivityManager", lpparam.classLoader);
        findAndHookMethod(amClazz, "isLowRamDevice", SOLUTION_RETURN);
        //@hide,-> system,add condition:IS_DEBUGGABLE DEVELOPMENT_FORCE_LOW_RAM
        findAndHookMethod(amClazz, "isLowRamDeviceStatic", SOLUTION_RETURN);
        //I don't wanna hook v4-support library because of laziness,bye.
    }
}
