package com.ryuunoakaihitomi.lowramswitcher;

import android.annotation.SuppressLint;
import android.app.ActivityManager;

import java.lang.reflect.Method;

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
    //arg:str key,(str def);ret:str
    private final XC_MethodHook SOLUTION_FILTER_STRING_RET = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (LOW_RAM_TAG.equals(param.args[0]))
                param.setResult(String.valueOf(!isLowRAM));
        }
    };
    //arg:str key,(bol def);ret:bol
    private final XC_MethodHook SOLUTION_FILTER_BOOL_RET = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (LOW_RAM_TAG.equals(param.args[0]))
                param.setResult(!isLowRAM);
        }
    };
    //arg:null,ret:bol
    private final XC_MethodReplacement SOLUTION_RETURN = XC_MethodReplacement.returnConstant(!isLowRAM);

    @Override
    public void initZygote(final StartupParam startupParam) throws Throwable {
        final String SYSTEM_PROPERTIES_CLASS_NAME = "android.os.SystemProperties";
        //init the real value
        try {
            @SuppressLint("PrivateApi") Class<?> clazz = Class.forName(SYSTEM_PROPERTIES_CLASS_NAME);
            Method method = clazz.getMethod("get", String.class);
            isLowRAM = "true".equals(method.invoke(null, "ro.config.low_ram"));
        } catch (Exception ignored) {
        }
        //-> system
        Class<?> spClazz = findClass(SYSTEM_PROPERTIES_CLASS_NAME, null);
        //@hide,-> app(inflection) | system
        hookAllMethods(spClazz, "getBoolean", SOLUTION_FILTER_BOOL_RET);
        //for Android 7.0 or lower
        hookAllMethods(spClazz, "get", SOLUTION_FILTER_STRING_RET);
    }

    //->app
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        //-> app
        Class<?> amClazz = findClass("android.app.ActivityManager", lpparam.classLoader);
        final String LOW_RAM_SHOWED_API_METHOD_NAME = "isLowRamDevice";
        findAndHookMethod(amClazz, LOW_RAM_SHOWED_API_METHOD_NAME, SOLUTION_RETURN);
        //@hide,-> app(inflection) | system;add condition:IS_DEBUGGABLE DEVELOPMENT_FORCE_LOW_RAM
        findAndHookMethod(amClazz, "isLowRamDeviceStatic", SOLUTION_RETURN);
        //v4-support library(unnecessary hook in kitkat or +)
        try {
            findAndHookMethod(findClass("android.support.v4.app.ActivityManagerCompat", lpparam.classLoader), LOW_RAM_SHOWED_API_METHOD_NAME, ActivityManager.class, SOLUTION_RETURN);
        } catch (Throwable t) { //may throw ClassNotFoundException
            System.out.println(lpparam.packageName + ":android.support.v4.app.ActivityManagerCompat.isLowRamDevice() not found!");
        }
    }
}
