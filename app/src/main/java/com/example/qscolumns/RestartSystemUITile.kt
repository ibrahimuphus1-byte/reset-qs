package com.example.qscolumns

import android.graphics.drawable.Icon
import android.widget.Toast
import io.github.libxposed.api.XposedHelpers

object RestartSystemUITile {
    private val STATE_CLASS = "com.android.systemui.qs.QSTile.State"

    fun create(host: Any, classLoader: ClassLoader): Any? {
        return try {
            val qsTileClass = XposedHelpers.findClass("com.android.systemui.qs.QSTile", classLoader)
            val tileInstance = XposedHelpers.callConstructor(qsTileClass, host)

            // Override handleClick
            XposedHelpers.findAndHookMethod(
                qsTileClass,
                "handleClick",
                XposedModuleInterface.MethodHooker { _ ->
                    try {
                        val context = XposedHelpers.callMethod(host, "getContext") as android.content.Context
                        Runtime.getRuntime().exec("su -c am restart com.android.systemui")
                        Toast.makeText(context, "Restarting SystemUI...", Toast.LENGTH_SHORT).show()
                    } catch (_: Throwable) {
                        try {
                            Runtime.getRuntime().exec("su -c pkill -f com.android.systemui")
                        } catch (_: Throwable) { }
                    }
                }
            )

            // Override getTileLabel
            XposedHelpers.findAndHookMethod(
                qsTileClass,
                "getTileLabel",
                XposedModuleInterface.MethodHooker { paramHook ->
                    paramHook.result = "Restart SystemUI"
                }
            )

            // Override getState
            XposedHelpers.findAndHookMethod(
                qsTileClass,
                "getState",
                XposedModuleInterface.MethodHooker { paramHook ->
                    val state = XposedHelpers.callConstructor(
                        XposedHelpers.findClass(STATE_CLASS, classLoader)
                    )
                    val context = XposedHelpers.callMethod(host, "getContext") as android.content.Context
                    val icon = Icon.createWithResource(context, android.R.drawable.ic_menu_rotate)
                    XposedHelpers.setObjectField(state, "icon", icon)
                    XposedHelpers.setObjectField(state, "label", "Restart SystemUI")
                    paramHook.result = state
                }
            )

            tileInstance
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
