package cz.mroczis.netmonster.core

import android.Manifest
import android.content.Context
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import cz.mroczis.netmonster.core.db.NetworkTypeTable
import cz.mroczis.netmonster.core.db.model.NetworkType
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.feature.config.PhysicalChannelConfigSource
import cz.mroczis.netmonster.core.feature.detect.*
import cz.mroczis.netmonster.core.feature.merge.CellMerger
import cz.mroczis.netmonster.core.feature.merge.CellSource
import cz.mroczis.netmonster.core.feature.postprocess.*
import cz.mroczis.netmonster.core.model.cell.ICell
import cz.mroczis.netmonster.core.model.config.PhysicalChannelConfig
import cz.mroczis.netmonster.core.subscription.ISubscriptionManagerCompat
import cz.mroczis.netmonster.core.telephony.ITelephonyManagerCompat
import cz.mroczis.netmonster.core.util.isDisplayOn

internal class NetMonster(
    private val context: Context,
    private val subscription: ISubscriptionManagerCompat
) : INetMonster {

    private val merger = CellMerger()
    private val physicalChannelSource by lazy { PhysicalChannelConfigSource() }

    /**
     * Postprocessors that try to fix / add behaviour to [ITelephonyManagerCompat.getAllCellInfo]
     */
    private val postprocessors = mutableListOf<ICellPostprocessor>().apply {
        add(MocnNetworkPostprocessor(subscription)) // fix PLMNs
        add(InvalidCellsPostprocessor()) // get rid of false-positive cells
        add(PrimaryCellPostprocessor()) // mark 1st cell as Primary if required
        add(PlmnPostprocessor()) // guess PLMNs when channels match
    }

    @WorkerThread
    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE]
    )
    override fun getCells(): List<ICell> = getCells(
        CellSource.CELL_LOCATION, CellSource.ALL_CELL_INFO, CellSource.NEIGHBOURING_CELLS
    )

    @WorkerThread
    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE]
    )
    override fun getCells(vararg sources: CellSource): List<ICell> {
        val subscriptions = subscription.getActiveSubscriptionIds()
        val oldApi = mutableListOf<ICell>().apply {
            if (sources.contains(CellSource.CELL_LOCATION)) {
                val serving = subscriptions.map { subId ->
                    NetMonsterFactory.getTelephony(context, subId).getCellLocation()
                }.flatten().toSet()

                addAll(serving)
            }

            if (sources.contains(CellSource.NEIGHBOURING_CELLS)) {
                val neighbouring = subscriptions.map { subId ->
                    NetMonsterFactory.getTelephony(context, subId).getNeighboringCellInfo()
                }.flatten().toSet()

                addAll(neighbouring)
            }
        }

        val newApi = if (sources.contains(CellSource.ALL_CELL_INFO)) {
            var allCells = subscriptions.map { subId ->
                NetMonsterFactory.getTelephony(context, subId).getAllCellInfo()
            }.flatten().toSet().toList()

            postprocessors.forEach { allCells = it.postprocess(allCells) }
            allCells
        } else emptyList()

        return merger.merge(oldApi, newApi, context.isDisplayOn())
    }

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE]
    )
    override fun getNetworkType(subId: Int): NetworkType = getNetworkType(
        subId,
        DetectorHspaDc(),
        DetectorLteAdvancedServiceState(),
        DetectorLteAdvancedPhysicalChannel(),
        DetectorLteAdvancedCellInfo(),
        DetectorAosp() // best to keep last when all other strategies fail
    ) ?: NetworkTypeTable.get(NetworkType.UNKNOWN)

    override fun getNetworkType(subId: Int, vararg detectors: INetworkDetector): NetworkType? {
        val telephony = getTelephony(subId)
        for (detector in detectors) {
            detector.detect(this, telephony)?.let {
                return it
            }
        }

        return null
    }

    override fun getPhysicalChannelConfiguration(subId : Int): List<PhysicalChannelConfig> =
        getTelephony(subId).getTelephonyManager()?.let {
            physicalChannelSource.get(it, subId)
        } ?: emptyList()

    private infix fun getTelephony(subId: Int) : ITelephonyManagerCompat {
        return NetMonsterFactory.getTelephony(context, subId)
    }

}