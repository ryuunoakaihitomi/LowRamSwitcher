package com.ryuunoakaihitomi.lowramswitcher;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Xposed Main Hook
 * Created by ZQY on 2018/3/13.
 */

public class XpMainHook implements IXposedHookZygoteInit {

    @Override
    public void initZygote(final StartupParam startupParam) throws Throwable {
        XposedHelpers.findAndHookMethod(XposedHelpers.findClass("android.os.SystemProperties", null), "getBoolean", String.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ("ro.config.low_ram".equals(param.args[0]))
                    param.setResult(false);
            }
        });
    }
}
