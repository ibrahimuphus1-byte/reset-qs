package com.example.qscolumns

import android.graphics.drawable.Icon
import android.widget.Toast
import de.robv.android.xposed.XposedHelpers

/**
 * Creates a QS tile that restarts SystemUI when clicked.
 * Uses reflection to construct a tile that extends com.android.systemui.qs.QSTile.
 */
object RestartSystemUITile {

    // The tile state class (QSTile.State is a common base)
    private val STATE_CLASS = "com.android.systemui.qs.QSTile.State"

    fun create(host: Any, classLoader: ClassLoader): Any? {
        return try {
            // Get QSTile class
            val qsTileClass = XposedHelpers.findClass(
                "com.android.systemui.qs.QSTile",
                classLoader
            )

            // We'll create an anonymous subclass of QSTile using reflection
            // Use XposedHelpers to call constructor and override methods
            val tileInstance = XposedHelpers.callConstructor(qsTileClass, host)

            // Override methods using Xposed's hooking on this instance
            // handleClick: restart SystemUI
            XposedHelpers.findAndHookMethod(
                qsTileClass,
                "handleClick",
                object : de.robv.android.xposed.XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: de.robv.android.xposed.XC_MethodHook.MethodHookParam?) {
                        try {
                            val context = XposedHelpers.callMethod(host, "getContext") as android.content.Context
                            // Execute restart command via shell
                            Runtime.getRuntime().exec("su -c am restart com.android.systemui")
                            Toast.makeText(context, "Restarting SystemUI...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            // fallback: try using pkill
                            try {
                                Runtime.getRuntime().exec("su -c pkill -f com.android.systemui")
                            } catch (_: Exception) {}
                        }
                    }
                }
            )

            // getTileLabel: return label
            XposedHelpers.findAndHookMethod(
                qsTileClass,
                "getTileLabel",
                object : de.robv.android.xposed.XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: de.robv.android.xposed.XC_MethodHook.MethodHookParam?): Any {
                        return "Restart SystemUI"
                    }
                }
            )

            // getState: return new State with icon and label
            XposedHelpers.findAndHookMethod(
                qsTileClass,
                "getState",
                object : de.robv.android.xposed.XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: de.robv.android.xposed.XC_MethodHook.MethodHookParam?): Any {
                        // Create a State object
                        val state = XposedHelpers.callConstructor(
                            XposedHelpers.findClass(STATE_CLASS, classLoader)
                        )
                        // Set icon - use built-in or custom drawable
                        val context = XposedHelpers.callMethod(host, "getContext") as android.content.Context
                        val icon = Icon.createWithResource(
                            context,
                            android.R.drawable.ic_menu_rotate // or use your own
                        )
                        XposedHelpers.setObjectField(state, "icon", icon)
                        XposedHelpers.setObjectField(state, "label", "Restart SystemUI")
                        // Set state to active? Not needed for instant action.
                        return state
                    }
                }
            )

            tileInstance
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
