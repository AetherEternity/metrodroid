/*
 * LisboaVivaTrip.java
 *
 * Copyright 2018 Google
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

package au.id.micolous.metrodroid.transit.lisboaviva

import au.id.micolous.metrodroid.multi.Parcelize

import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.en1545.*
import au.id.micolous.metrodroid.util.ImmutableByteArray

@Parcelize
internal data class LisboaVivaTransaction (override val parsed: En1545Parsed): En1545Transaction() {

    override val isTapOn: Boolean
        get() {
            val transition = parsed.getIntOrZero(TRANSITION)
            return transition == 1
        }

    override val isTapOff: Boolean
        get() {
            val transition = parsed.getIntOrZero(TRANSITION)
            return transition == 4
        }

    override val routeNames: List<String>?
        get() {
            val routeNumber = parsed.getInt(En1545Transaction.EVENT_ROUTE_NUMBER) ?: return emptyList()
            return if (agency == LisboaVivaLookup.AGENCY_CP && routeNumber == LisboaVivaLookup.ROUTE_CASCAIS_SADO) {
                if ((stationId ?: 0) <= 54)
                    listOf("Cascais")
                else
                    listOf("Sado")
            } else super.routeNames

        }

    override val lookup: En1545Lookup
        get() = LisboaVivaLookup

    override fun getStation(station: Int?): Station? {
        return if (station == null) null else lookup.getStation(station, agency, parsed.getIntOrZero(En1545Transaction.EVENT_ROUTE_NUMBER))
    }

    constructor(data: ImmutableByteArray) : this(En1545Parser.parse(data, tripFields))

    companion object {
        private const val TRANSITION = "Transition"
        private val tripFields = En1545Container(
                En1545FixedInteger.dateTime(En1545Transaction.EVENT),
                En1545FixedHex(En1545Transaction.EVENT_UNKNOWN_A, 38),
                En1545FixedInteger("ContractsUsedBitmap", 4),
                En1545FixedHex(En1545Transaction.EVENT_UNKNOWN_B, 29),
                En1545FixedInteger(TRANSITION, 3),
                En1545FixedInteger(En1545Transaction.EVENT_SERVICE_PROVIDER, 5), // Curious
                En1545FixedHex(En1545Transaction.EVENT_UNKNOWN_C, 20),
                En1545FixedInteger(En1545Transaction.EVENT_DEVICE_ID, 16),
                En1545FixedInteger(En1545Transaction.EVENT_ROUTE_NUMBER, 16),
                En1545FixedInteger(En1545Transaction.EVENT_LOCATION_ID, 8),
                En1545FixedHex(En1545Transaction.EVENT_UNKNOWN_D, 63)
        )
    }
}
