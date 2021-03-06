package cz.mroczis.netmonster.core.feature.config

import android.Manifest
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import cz.mroczis.netmonster.core.feature.config.ServiceStateSource.ServiceStateListener
import cz.mroczis.netmonster.core.util.PhoneStateListenerPort
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * On Android N and older fetches [ServiceState] using [ServiceStateListener].
 * On Android O and newer takes [ServiceState] from [TelephonyManager.getServiceState].
 */
class ServiceStateSource {

    /**
     * Async executor so can await data from [ServiceStateListener]
     */
    private val asyncExecutor by lazy {
        val thread = HandlerThread(this.javaClass.simpleName).apply {
            start()
        }
        Handler(thread.looper)
    }

    /**
     * Registers [ServiceStateListener] and awaits for data. After 500 milliseconds time outs if
     * nothing is delivered.
     *
     * On Android O and newer directly grabs [ServiceState] from [TelephonyManager].
     */
    @RequiresPermission(allOf = [Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun get(telephonyManager: TelephonyManager, subId: Int): ServiceState? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            telephonyManager.serviceState
        } else {
            val asyncLock = CountDownLatch(1)
            var simState: ServiceState? = null

            asyncExecutor.post {
                // We'll receive callbacks on thread that created instance of [listener] by default.
                // Async processing is required otherwise deadlock would arise cause we block
                // original thread
                val listener = ServiceStateListener(subId) {
                    simState = it
                    asyncLock.countDown()
                    telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE)
                }

                telephonyManager.listen(listener, PhoneStateListener.LISTEN_SERVICE_STATE)
            }

            // And we also must block original thread
            // It'll get unblocked once we receive required data
            // This usually takes +/- 20 ms to complete
            try {
                asyncLock.await(500, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                // System was not able to deliver PhysicalChannelConfig in this time slot
            }

            simState
        }


    /**
     * Kotlin friendly PhoneStateListener that grabs [ServiceState]
     */
    private class ServiceStateListener(
        subId: Int?,
        private val simStateListener: ServiceStateListener.(state: ServiceState) -> Unit
    ) : PhoneStateListenerPort(subId) {

        override fun onServiceStateChanged(serviceState: ServiceState) {
            simStateListener.invoke(this, serviceState)
        }
    }
}