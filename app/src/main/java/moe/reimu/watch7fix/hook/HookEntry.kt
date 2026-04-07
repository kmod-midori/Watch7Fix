package moe.reimu.watch7fix.hook

import android.telephony.TelephonyManager
import android.util.ArrayMap
import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed
class HookEntry : IYukiHookXposedInit {
    override fun onInit() = configs {
        debugLog {
            tag = "Watch7Fix"
        }
    }

    override fun onHook() = encase {
        loadSystem {
            "com.android.server.SystemConfig".toClassOrNull()?.resolve()?.method {
                name = "getAvailableFeatures"
            }?.hookAll {
                after {
                    val currentResult = result<ArrayMap<String, Any>>()
                    currentResult?.remove("cn.google.services")
                    currentResult?.remove("com.google.android.feature.services_updater")
                }
            }
        }
        loadApp("com.samsung.wearable.watch7plugin") {
            "com.samsung.android.companionapps.reminder.utils.DeviceFeature".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "isSamsungFeatureSupported" }?.hook()?.replaceToTrue()
            }
            "com.samsung.android.companionservice.service.SettingsCompanion".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "isSupportBedtimeOnPhone" }?.hook()?.replaceToTrue()
                firstMethodOrNull { name = "isSupportRoutineMode" }?.hook()?.replaceToTrue()
            }
            "com.samsung.android.companionservice.capability.CapabilityExchangeMessage".toClassOrNull()?.resolve()?.apply {
                constructor {
                    // All
                }.hookAll().after {
                    val data = firstFieldOrNull { name = "data" }?.of(instance)?.get<MutableMap<String, String>>()
                    data?.set("vender", "samsung")
                }
            }
            "com.samsung.android.companionservice.service.SelfDiagnosticsCompanion".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull { name = "isCapabilityExchangeRequestRequired" }?.hook()?.replaceToTrue()
            }
        }
        loadApp("com.sec.android.app.shealth", "com.samsung.android.app.watchmanager", "com.samsung.wearable.watch7plugin", "com.samsung.android.shealthmonitor") {
            hookSim()
        }
        loadApp("com.samsung.android.shealthmonitor") {
            "android.os.Build".toClassOrNull()?.resolve()?.apply {
                firstField { name = "MANUFACTURER" }.set("samsung")
            }
            "android.os.SystemProperties".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "get"
                    parameters(String::class)
                }?.hook()?.before {
                    if (args(0).string() == "ro.csc.countryiso_code") {
                        result = "US"
                    }
                }?.onFailureThrowToApp()
            }
        }
        loadApp("com.miui.powerkeeper") {
            "com.miui.powerkeeper.provider.PowerKeeperConfigureManager".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "pkgHasIcon"
                    parameters(String::class)
                }?.hook()?.before {
                    if (args(0).string() == "com.samsung.wearable.watch7plugin") {
                        result = true
                    }
                }
            }
        }
    }

    private fun PackageParam.hookSim() {
        try {
            val tmClass = "android.telephony.TelephonyManager".toClass().resolve()
            tmClass.method { name = "getSimState" }.hookAll {
                replaceTo(TelephonyManager.SIM_STATE_READY)
            }

            tmClass.method { name = "getSimCountryIso" }.hookAll {
                replaceTo("us")
            }
            tmClass.method { name = "getSimOperatorName" }.hookAll {
                replaceTo("T-Mobile")
            }
            tmClass.method { name = "getSimOperator" }.hookAll {
                replaceTo("310200")
            }

            tmClass.method { name = "getNetworkCountryIso" }.hookAll {
                replaceTo("us")
            }
            tmClass.method { name = "getNetworkOperator" }.hookAll {
                replaceTo("310200")
            }
            tmClass.method { name = "getNetworkOperatorName" }.hookAll {
                replaceTo("T-Mobile")
            }
        } catch (e: Exception) {
            YLog.error("Failed to hook SIM", e)
        }
    }
}