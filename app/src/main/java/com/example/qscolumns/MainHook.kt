package com.example.qscolumns

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.XposedHook
import io.github.libxposed.api.XposedHelpers

@XposedHook
class MainHook : XposedModule() {

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "com.android.systemui") return

        val classLoader = param.classLoader

        // 1. Set QS columns to 5
        try {
            val qsPanelClass = XposedHelpers.findClass("com.android.systemui.qs.QSPanel", classLoader)
            XposedHelpers.findAndHookMethod(
                qsPanelClass,
                "getNumColumns",
                object : XposedModuleInterface.MethodHooker {
                    override fun beforeHookedMethod(param: XposedModuleInterface.MethodHookParam) {
                        // Do nothing
                    }
                    override fun afterHookedMethod(param: XposedModuleInterface.MethodHookParam) {
                        param.result = 5
                    }
                }
            )
        } catch (_: Throwable) {
            // fallback to TileLayout
        }

        try {
            val tileLayoutClass = XposedHelpers.findClass("com.android.systemui.qs.TileLayout", classLoader)
            XposedHelpers.findAndHookMethod(
                tileLayoutClass,
                "getNumColumns",
                object : XposedModuleInterface.MethodHooker {
                    override fun beforeHookedMethod(param: XposedModuleInterface.MethodHookParam) {}
                    override fun afterHookedMethod(param: XposedModuleInterface.MethodHookParam) {
                        param.result = 5
                    }
                }
            )
        } catch (_: Throwable) { }

        // 2. Add Restart SystemUI tile
        try {
            val hostClass = XposedHelpers.findClass("com.android.systemui.qs.QSTileHost", classLoader)

            XposedHelpers.findAndHookMethod(
                hostClass,
                "getTiles",
                object : XposedModuleInterface.MethodHooker {
                    override fun beforeHookedMethod(param: XposedModuleInterface.MethodHookParam) {}
                    override fun afterHookedMethod(param: XposedModuleInterface.MethodHookParam) {
                        val original = param.result as? MutableList<*> ?: return
                        val tileSpecs = original.map { it.toString() }.toMutableList()
                        if (!tileSpecs.contains("restart_systemui")) {
                            tileSpecs.add("restart_systemui")
                        }
                        param.result = tileSpecs
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                hostClass,
                "createTile",
                String::class.java,
                object : XposedModuleInterface.MethodHooker {
                    override fun beforeHookedMethod(param: XposedModuleInterface.MethodHookParam) {}
                    override fun afterHookedMethod(param: XposedModuleInterface.MethodHookParam) {
                        val spec = param.args[0] as String
                        if (spec == "restart_systemui") {
                            val host = param.thisObject
                            val tile = RestartSystemUITile.create(host, classLoader)
                            if (tile != null) {
                                param.result = tile
                            }
                        }
                    }
                }
            )
        } catch (_: Throwable) { }
    }
}
