/*
 * AdelaideLookup.java
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
package au.id.micolous.metrodroid.transit.adelaide

import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545LookupSTR
import au.id.micolous.metrodroid.util.NumberUtils

class AdelaideLookup private constructor() : En1545LookupSTR("adelaide") {

    override val timeZone: MetroTimeZone
        get() = MetroTimeZone.ADELAIDE

    override fun parseCurrency(price: Int): TransitCurrency {
        return TransitCurrency.AUD(price)
    }

    override fun getSubscriptionName(agency: Int?, contractTariff: Int?): String? {
        if (contractTariff == null)
            return null
        val tariff = TARIFFS[contractTariff] ?: return NumberUtils.intToHex(contractTariff)

        return Localizer.localizeString(tariff)
    }

    internal fun isPurseTariff(agency: Int?, contractTariff: Int?): Boolean {
        if (agency == null || agency != AGENCY_ADL_METRO || contractTariff == null) {
            return false
        }

        return contractTariff in TARIFFS
        // TODO: Exclude monthly tickets when implemented
    }

    override fun getRouteName(routeNumber: Int?, routeVariant: Int?, agency: Int?, transport: Int?): String? {
        if (routeNumber == 0)
            return null
        return super.getRouteName(routeNumber, routeVariant, agency, transport)
    }

    companion object {
        private const val AGENCY_ADL_METRO = 1

        private val TARIFFS = mapOf(
                0x804 to R.string.adelaide_ticket_type_regular,
                0x808 to R.string.adelaide_ticket_type_concession
                // TODO: handle other tickets

                // TODO: handle monthly subscriptions
        )

        val instance = AdelaideLookup()
    }
}
