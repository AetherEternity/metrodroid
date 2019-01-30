/*
 * LaxTapTrip.java
 *
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.lax_tap

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip
import au.id.micolous.metrodroid.util.NumberUtils

import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.Trip
import au.id.micolous.metrodroid.transit.nextfare.NextfareTripCapsule

/**
 * Represents trip events on LAX TAP card.
 */
@Parcelize
class LaxTapTrip (override val capsule: NextfareTripCapsule): NextfareTrip() {

    override// Metro Bus uses the station_id for route numbers.
    // Normally not possible to guess what the route is.
    val routeName: String?
        get() {
            if (capsule.mModeInt == LaxTapData.AGENCY_METRO && capsule.mStartStation >= LaxTapData.METRO_BUS_START) {
                return LaxTapData.METRO_BUS_ROUTES[capsule.mStartStation] ?:
                        Localizer.localizeString(R.string.unknown_format, capsule.mStartStation)
            }
            return null
        }

    // Metro Bus uses the station_id for route numbers.
    // Normally not possible to guess what the route is.
    override val humanReadableRouteID: String?
        get() = if (capsule.mModeInt == LaxTapData.AGENCY_METRO && capsule.mStartStation >= LaxTapData.METRO_BUS_START) {
            NumberUtils.intToHex(capsule.mStartStation)
        } else null

    override val routeLanguage: String?
        get() = "en-US"

    override val currency: String
        get() = "USD"

    override val str: String?
        get() = LaxTapData.LAX_TAP_STR

    override fun getStation(stationId: Int): Station? {
        if (capsule.mModeInt == LaxTapData.AGENCY_SANTA_MONICA) {
            // Santa Monica Bus doesn't use this.
            return null
        }

        if (capsule.mModeInt == LaxTapData.AGENCY_METRO && stationId >= LaxTapData.METRO_BUS_START) {
            // Metro uses this for route names.
            return null
        }

        return super.getStation(stationId)
    }

    override fun lookupMode(): Trip.Mode {
        if (capsule.mModeInt == LaxTapData.AGENCY_METRO) {
            return if (capsule.mStartStation >= LaxTapData.METRO_BUS_START) {
                Trip.Mode.BUS
            } else if (capsule.mStartStation < LaxTapData.METRO_LR_START && capsule.mStartStation != 61) {
                Trip.Mode.METRO
            } else {
                Trip.Mode.TRAM
            }
        }
        return super.lookupMode()
    }
}
