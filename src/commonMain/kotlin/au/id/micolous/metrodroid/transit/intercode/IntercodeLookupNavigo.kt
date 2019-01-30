/*
 * IntercodeLookupNavigo.java
 *
 * Copyright 2009-2013 by 'L1L1'
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

package au.id.micolous.metrodroid.transit.intercode

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.Station
import au.id.micolous.metrodroid.transit.en1545.En1545Transaction
import au.id.micolous.metrodroid.util.StationTableReader

internal class IntercodeLookupNavigo : IntercodeLookupSTR(NAVIGO_STR) {

    override fun getStation(locationId: Int, agency: Int?, transport: Int?): Station? {
        if (locationId == 0)
            return null
        var mdstStationId = locationId or ((agency ?: 0) shl 16) or ((transport ?:0) shl 24)
        val sector_id = locationId shr 9
        val station_id = locationId shr 4 and 0x1F
        var humanReadableId = locationId.toString()
        var fallBackName = locationId.toString()
        if (transport == En1545Transaction.TRANSPORT_TRAIN && (agency == RATP || agency == SNCF)) {
            mdstStationId = mdstStationId and -0xff0010 or 0x30000
        }
        if ((agency == RATP || agency == SNCF) && (transport == En1545Transaction.TRANSPORT_METRO || transport == En1545Transaction.TRANSPORT_TRAM)) {
            mdstStationId = mdstStationId and 0x0000fff0 or 0x3020000
            // TODO: i18n
            fallBackName = if (SECTOR_NAMES[sector_id] != null)
                "sector " + SECTOR_NAMES[sector_id] + " station " + station_id
            else
                "sector $sector_id station $station_id"
            humanReadableId = sector_id.toString() + "/" + station_id
        }

        return StationTableReader.getStationNoFallback(NAVIGO_STR, mdstStationId, humanReadableId) ?: Station.unknown(fallBackName)
    }

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?): String? {
        if (contractTariff == null)
            return null
        when (contractTariff) {
            0 ->
                // TODO: i18n
                return "Forfait"
        }
        return Localizer.localizeString(R.string.unknown_format, contractTariff)
    }

    companion object {
        private val SECTOR_NAMES = mapOf(
                // TODO: Move this to MdSt
                1 to "Cité",
                2 to "Rennes",
                3 to "Villette",
                4 to "Montparnasse",
                5 to "Nation",
                6 to "Saint-Lazare",
                7 to "Auteuil",
                8 to "République",
                9 to "Austerlitz",
                10 to "Invalides",
                11 to "Sentier",
                12 to "Île Saint-Louis",
                13 to "Daumesnil",
                14 to "Italie",
                15 to "Denfert",
                16 to "Félix Faure",
                17 to "Passy",
                18 to "Étoile",
                19 to "Clichy - Saint Ouen",
                20 to "Montmartre",
                21 to "Lafayette",
                22 to "Buttes Chaumont",
                23 to "Belleville",
                24 to "Père Lachaise",
                25 to "Charenton",
                26 to "Ivry - Villejuif",
                27 to "Vanves",
                28 to "Issy",
                29 to "Levallois",
                30 to "Péreire",
                31 to "Pigalle"
        )
        private const val NAVIGO_STR = "navigo"

        private const val RATP = 3
        private const val SNCF = 2
    }
}
