package cz.mroczis.netmonster.core.feature.merge

import android.os.Build
import cz.mroczis.netmonster.core.model.annotation.SinceSdk
import cz.mroczis.netmonster.core.model.annotation.TillSdk
import cz.mroczis.netmonster.core.model.cell.ICell
import cz.mroczis.netmonster.core.telephony.ITelephonyManagerCompat

/**
 * Represents unique identifier of methods that might be used
 * to obtain any instance of [ICell] in this library
 */
enum class CellSource {
    /**
     * Corresponds to [ITelephonyManagerCompat.getCellLocation].
     *
     * Deprecated since SDK 26 / [Build.VERSION_CODES.O]
     */
    CELL_LOCATION,

    /**
     * Corresponds to [ITelephonyManagerCompat.getNeighbouringCells]
     *
     * Deprecated since SDK 23 / [Build.VERSION_CODES.M]
     * Removed since SDK 29 / [Build.VERSION_CODES.Q]
     */
    @TillSdk(
        sdkInt = Build.VERSION_CODES.Q,
        fallbackBehaviour = "On Q+ this source does not work, replaced by ALL_CELL_INFO"
    )
    NEIGHBOURING_CELLS,

    /**
     * Corresponds to [ITelephonyManagerCompat.getAllCellInfo]
     */
    @SinceSdk(Build.VERSION_CODES.JELLY_BEAN_MR1)
    ALL_CELL_INFO;

}