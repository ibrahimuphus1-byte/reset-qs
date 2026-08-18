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
                XposedModuleInterface.MethodHooker { paramHook ->
                    paramHook.result = 5
                }
            )
        } catch (_: Throwable) { /* fallback to TileLayout */ }

        try {
            val tileLayoutClass = XposedHelpers.findClass("com.android.systemui.qs.TileLayout", classLoader)
            XposedHelpers.findAndHookMethod(
                tileLayoutClass,
                "getNumColumns",
                XposedModuleInterface.MethodHooker { paramHook ->
                    paramHook.result = 5
                }
            )
        } catch (_: Throwable) { }

        // 2. Add Restart SystemUI tile
        try {
            val hostClass = XposedHelpers.findClass("com.android.systemui.qs.QSTileHost", classLoader)

            // Hook getTiles() to insert custom tile spec
            XposedHelpers.findAndHookMethod(
                hostClass,
                "getTiles",
                XposedModuleInterface.MethodHooker { paramHook ->
                    val original = paramHook.invokeOriginal() as? MutableList<*> ?: return@MethodHooker
                    val tileSpecs = original.map { it.toString() }.toMutableList()
                    if (!tileSpecs.contains("restart_systemui")) {
                        tileSpecs.add("restart_systemui")
                    }
                    paramHook.result = tileSpecs
                }
            )

            // Hook createTile(String) to return our custom tile
            XposedHelpers.findAndHookMethod(
                hostClass,
                "createTile",
                String::class.java,
                XposedModuleInterface.MethodHooker { paramHook ->
                    val spec = paramHook.args[0] as String
                    if (spec == "restart_systemui") {
                        val host = paramHook.thisObject
                        paramHook.result = RestartSystemUITile.create(host, classLoader)
                    } else {
                        paramHook.invokeOriginal()
                    }
                }
            )
        } catch (_: Throwable) { }
    }
}
