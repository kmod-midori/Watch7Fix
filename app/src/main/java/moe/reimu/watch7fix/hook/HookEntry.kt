package moe.reimu.watch7fix.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed
class HookEntry : IYukiHookXposedInit {

    override fun onInit() = configs {
        debugLog {
            tag = "Watch7Fix"
        }
    }

    override fun onHook() = encase {
        loadApp("com.samsung.wearable.watch7plugin") {
            "com.samsung.android.companionapps.reminder.utils.DeviceFeature".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "isSamsungFeatureSupported" }?.hook()?.replaceToTrue()
            }
            "com.samsung.android.companionservice.service.SettingsCompanion".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "isSupportBedtimeOnPhone" }?.hook()?.replaceToTrue()
                firstMethodOrNull { name = "isSupportRoutineMode" }?.hook()?.replaceToTrue()
            }
        }
    }
}