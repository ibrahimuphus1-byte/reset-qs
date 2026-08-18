package com.example.qscolumns

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        // 1. Set QS columns to 5
        try {
            val qsPanelClass = XposedHelpers.findClass(
                "com.android.systemui.qs.QSPanel",
                lpparam.classLoader
            )
            XposedHelpers.findAndHookMethod(
                qsPanelClass,
                "getNumColumns",
                XC_MethodReplacement.returnConstant(5)
            )
        } catch (_: Throwable) { }

        try {
            val tileLayoutClass = XposedHelpers.findClass(
                "com.android.systemui.qs.TileLayout",
                lpparam.classLoader
            )
            XposedHelpers.findAndHookMethod(
                tileLayoutClass,
                "getNumColumns",
                XC_MethodReplacement.returnConstant(5)
            )
        } catch (_: Throwable) { }

        // 2. Add Restart SystemUI tile
        try {
            val hostClass = XposedHelpers.findClass(
                "com.android.systemui.qs.QSTileHost",
                lpparam.classLoader
            )
            // Hook getTiles() to insert our tile spec if not present
            XposedHelpers.findAndHookMethod(
                hostClass,
                "getTiles",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        val original = param.invokeOriginalMethod() as? MutableList<*>
                            ?: return null
                        val tileSpecs = original.map { it.toString() }.toMutableList()
                        if (!tileSpecs.contains("restart_systemui")) {
                            tileSpecs.add("restart_systemui")
                        }
                        return tileSpecs
                    }
                }
            )

            // Also hook into QSTileHost constructor to create our tile instance
            XposedHelpers.findAndHookMethod(
                hostClass,
                "createTile",
                String::class.java,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        val spec = param.args[0] as String
                        if (spec == "restart_systemui") {
                            // Create our custom tile instance
                            return RestartSystemUITile.create(
                                param.thisObject,
                                param.thisObject.javaClass.classLoader
                            )
                        }
                        return param.invokeOriginalMethod()
                    }
                }
            )
        } catch (_: Throwable) {
            // Fallback: if createTile hook fails, tile won't be added
        }
    }
}
