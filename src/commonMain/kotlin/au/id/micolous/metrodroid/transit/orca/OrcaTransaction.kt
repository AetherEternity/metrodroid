/*
 * OrcaTransaction.java
 *
 * Copyright 2011-2013 Eric Butler <eric@codebutler.com>
 * Copyright 2014 Kramer Campbell
 * Copyright 2015 Sean CyberKitsune McClenaghan
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.transit.orca

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.Epoch
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.Transaction
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.StationTableReader
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
class OrcaTransaction (private val mTimestamp: Long,
                       private val mCoachNum: Int,
                       private val mFare: Int,
                       private val mNewBalance: Int,
                       private val mAgency: Int,
                       private val mTransType: Int,
                       private val mIsTopup: Boolean): Transaction() {

    override val timestamp: TimestampFull?
        get() = if (mTimestamp == 0L) null else Epoch.utc(1970, MetroTimeZone.LOS_ANGELES).seconds(mTimestamp)

    override val isTapOff: Boolean
        get() = !mIsTopup && mTransType == TRANS_TYPE_TAP_OUT

    override val isCancel: Boolean
        get() = !mIsTopup && mTransType == TRANS_TYPE_CANCEL_TRIP

    // FIXME: Need to find bus route #s
    override val routeNames: List<String>
        get() = when {
            mIsTopup -> listOf(Localizer.localizeString(R.string.orca_topup))
            isLink -> listOf("Link Light Rail")
            isSounder -> listOf("Sounder Train")
            mAgency == OrcaTransitData.AGENCY_ST -> listOf("Express Bus")
            mAgency == OrcaTransitData.AGENCY_KCM -> listOf("Bus")
            else -> emptyList()
        }

    override val fare: TransitCurrency?
        get() = TransitCurrency.USD(if (mIsTopup) -mFare else mFare)

    override val station: Station?
        get() {
            if (mIsTopup)
                return null
            val s = getStation(mAgency, mCoachNum)
            if (s != null)
                return s
            if (isLink || isSounder || mAgency == OrcaTransitData.AGENCY_WSF) {
                return Station.unknown(mCoachNum)
            }

            return null
        }

    override val vehicleID: String?
        get() = when {
                mIsTopup -> mCoachNum.toString()
                isLink || isSounder || mAgency == OrcaTransitData.AGENCY_WSF -> null
                else -> mCoachNum.toString()
            }

    override val mode: Trip.Mode
        get() = when {
            mIsTopup -> Trip.Mode.TICKET_MACHINE
            isLink -> Trip.Mode.METRO
            isSounder -> Trip.Mode.TRAIN
            mAgency == OrcaTransitData.AGENCY_WSF -> Trip.Mode.FERRY
            else -> Trip.Mode.BUS
        }

    override val isTapOn: Boolean
        get() = !mIsTopup && mTransType == TRANS_TYPE_TAP_IN

    private val isLink: Boolean
        get() = mAgency == OrcaTransitData.AGENCY_ST && mCoachNum > 10000

    private val isSounder: Boolean
        get() = mAgency == OrcaTransitData.AGENCY_ST && mCoachNum < 20

    constructor(useData: ImmutableByteArray, isTopup: Boolean): this(
        mIsTopup = isTopup,
        mAgency = useData.getBitsFromBuffer(24, 4),
        mTimestamp = useData.getBitsFromBuffer(28, 32).toLong(),
        mCoachNum = useData.getBitsFromBuffer(76, 16),
        mFare = useData.getBitsFromBuffer(120, 15),
        mTransType = useData.getBitsFromBuffer(136, 8),
        mNewBalance = useData.getBitsFromBuffer(272, 16))

    override fun getAgencyName(isShort: Boolean): String? {
        return StationTableReader.getOperatorName(ORCA_STR, mAgency, isShort)
    }

    override fun isSameTrip(other: Transaction): Boolean {
        return other is OrcaTransaction && mAgency == other.mAgency
    }

    companion object {
        private const val ORCA_STR = "orca"

        private const val TRANS_TYPE_PURSE_USE = 0x0c
        private const val TRANS_TYPE_CANCEL_TRIP = 0x01
        private const val TRANS_TYPE_TAP_IN = 0x03
        private const val TRANS_TYPE_TAP_OUT = 0x07
        private const val TRANS_TYPE_PASS_USE = 0x60

        private fun getStation(agency: Int, stationId: Int): Station? {
            val id = (agency shl 16) or stationId
            return StationTableReader.getStationNoFallback(ORCA_STR, id,
                    NumberUtils.intToHex(id))
        }
    }
}
