package com.signaldekho.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

@SuppressLint("MissingPermission") // callers gate on ACCESS_FINE_LOCATION + READ_PHONE_STATE
class CellularRepo(private val context: Context) {

    fun read(): List<CellReading> {
        val subMgr = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val telMgr = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val subs = subMgr.activeSubscriptionInfoList ?: return emptyList()
        return subs.map { sub ->
            val tm = telMgr.createForSubscriptionId(sub.subscriptionId)
            val registered = tm.allCellInfo?.filter { it.isRegistered } ?: emptyList()
            val primary = registered.firstOrNull()
            CellReading(
                simSlot = sub.simSlotIndex + 1,
                operatorName = sub.carrierName?.toString() ?: "?",
                networkType = primary?.let { networkTypeOf(it) } ?: "?",
                dbm = primary?.let { dbmOf(it) },
                ageMillis = primary?.let {
                    @Suppress("DEPRECATION") // getTimeStamp() (nanos since boot) — getTimestampMillis needs API 30, minSdk is 26
                    (SystemClock.elapsedRealtimeNanos() - it.timeStamp) / 1_000_000
                } ?: 0L,
            )
        }
    }

    private fun networkTypeOf(info: CellInfo): String = when (info) {
        // CellInfoNr requires API 29 — it's only instantiated by the OS on API 29+ devices,
        // so referencing the class is safe at minSdk 26 (class-load only happens inside when on devices that return it)
        is CellInfoNr -> "5G"
        is CellInfoLte -> "4G"
        is CellInfoWcdma -> "3G"
        is CellInfoGsm -> "2G"
        else -> "?"
    }

    private fun dbmOf(info: CellInfo): Int? {
        val dbm = when (info) {
            is CellInfoNr -> info.cellSignalStrength.dbm
            is CellInfoLte -> info.cellSignalStrength.dbm
            is CellInfoWcdma -> info.cellSignalStrength.dbm
            is CellInfoGsm -> info.cellSignalStrength.dbm
            else -> return null
        }
        return if (dbm == CellInfo.UNAVAILABLE || dbm == Int.MAX_VALUE) null else dbm
    }
}
