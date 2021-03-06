package cz.mroczis.netmonster.core.feature.postprocess

import android.Manifest
import androidx.annotation.RequiresPermission
import cz.mroczis.netmonster.core.model.Network
import cz.mroczis.netmonster.core.model.cell.*
import cz.mroczis.netmonster.core.subscription.ISubscriptionManagerCompat

/**
 * The problem - when current network is MOCN (Multi-Operator Core Network) some Android phones
 * pick first entry from offered PLMN list ignoring the flag for currently active one.
 *
 * This issue is only present in entries returned from [android.telephony.TelephonyManager.getAllCellInfo]
 * but correct PLMNs is stored in [android.telephony.ServiceState] or [android.telephony.SubscriptionInfo].
 * This class aims to fix the issues by modifying PLMN assigned to cells.
 *
 * Postprocessor does not work if phone is in "Emergency calls only" state since those two sources
 * do not return valid data -> incorrect PLMN will stay incorrect
 *
 * References: [AOSP bug tracker](https://issuetracker.google.com/issues/73130708)
 */
class MocnNetworkPostprocessor(
    private val subscription: ISubscriptionManagerCompat
) : ICellPostprocessor {

    @RequiresPermission(allOf = [Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun postprocess(list: List<ICell>): List<ICell> {
        val subscriptions = subscription.getActiveSubscriptions().associateBy {
            it.network
        }
        return list.toMutableList().map { cell ->
            val suggestedSub = subscriptions[cell.network]
            if (suggestedSub?.network != null && suggestedSub.network != cell.network) {
                cell.let(PlmnSwitcher(suggestedSub.network))
            } else {
                cell
            }
        }
    }

    /**
     * Changes PLMN of given cells to [plmn].
     */
    private class PlmnSwitcher(
        private val plmn: Network
    ) : ICellProcessor<ICell> {

        override fun processCdma(cell: CellCdma): ICell = cell.copy(network = plmn)
        override fun processGsm(cell: CellGsm): ICell = cell.copy(network = plmn)
        override fun processLte(cell: CellLte): ICell = cell.copy(network = plmn)
        override fun processNr(cell: CellNr): ICell = cell.copy(network = plmn)
        override fun processTdscdma(cell: CellTdscdma): ICell = cell.copy(network = plmn)
        override fun processWcdma(cell: CellWcdma): ICell = cell.copy(network = plmn)
    }

}