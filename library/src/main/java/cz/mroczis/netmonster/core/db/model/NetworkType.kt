package cz.mroczis.netmonster.core.db.model

import android.os.Build
import android.telephony.TelephonyManager
import cz.mroczis.netmonster.core.model.annotation.SinceSdk

sealed class NetworkType {

    abstract val technology: Int

    data class Gsm internal constructor(@NetworkGsm override val technology: Int) : NetworkType()
    data class Cdma internal constructor(@NetworkCdma override val technology: Int) : NetworkType()
    data class Wcdma internal constructor(@NetworkWcdma override val technology: Int) : NetworkType()
    data class Lte internal constructor(@NetworkLte override val technology: Int) : NetworkType()
    data class Tdscdma internal constructor(@NetworkTdscdma override val technology: Int) : NetworkType()
    data class Nr internal constructor(@NetworkNr override val technology: Int) : NetworkType()
    data class Unknown internal constructor(@NetworkUnknown override val technology: Int) : NetworkType()

    /**
     * Copied constants from AOSP cause not all of them are accessible for all APIs.
     */
    companion object {
        const val UNKNOWN = TelephonyManager.NETWORK_TYPE_UNKNOWN
        const val GPRS = TelephonyManager.NETWORK_TYPE_GPRS
        const val EDGE = TelephonyManager.NETWORK_TYPE_EDGE
        const val UMTS = TelephonyManager.NETWORK_TYPE_UMTS
        const val CDMA = TelephonyManager.NETWORK_TYPE_CDMA
        const val EVDO_0 = TelephonyManager.NETWORK_TYPE_EVDO_0
        const val EVDO_A = TelephonyManager.NETWORK_TYPE_EVDO_A
        const val ONExRTT = TelephonyManager.NETWORK_TYPE_1xRTT
        const val HSDPA = TelephonyManager.NETWORK_TYPE_HSDPA
        const val HSUPA = TelephonyManager.NETWORK_TYPE_HSUPA
        const val HSPA = TelephonyManager.NETWORK_TYPE_HSPA
        const val IDEN = TelephonyManager.NETWORK_TYPE_IDEN
        const val EVDO_B = TelephonyManager.NETWORK_TYPE_EVDO_B
        const val LTE = TelephonyManager.NETWORK_TYPE_LTE
        const val EHRPD = TelephonyManager.NETWORK_TYPE_EHRPD
        const val HSPAP = TelephonyManager.NETWORK_TYPE_HSPAP
        @SinceSdk(Build.VERSION_CODES.N_MR1)
        const val GSM = 16
        @SinceSdk(Build.VERSION_CODES.N_MR1)
        const val TD_SCDMA = 17
        @SinceSdk(Build.VERSION_CODES.N_MR1)
        const val IWLAN = 18
        @SinceSdk(Build.VERSION_CODES.Q)
        const val NR = 20

        // Not in AOSP / not public in AOSP
        const val LTE_CA = 19
        const val HSPA_DC = Integer.MAX_VALUE - 1
    }
}

