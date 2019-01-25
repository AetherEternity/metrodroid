/*
 * EasyCardTransitData.kt
 *
 * Copyright 2017 Eric Butler <eric@codebutler.com>
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 *
 * Based on code from:
 * - http://www.fuzzysecurity.com/tutorials/rfid/4.html
 * - Farebot <https://codebutler.github.io/farebot/>
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
package au.id.micolous.metrodroid.transit.easycard

import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class EasyCardTransitData internal constructor(
        private val balanceRaw: Int,
        private val tripsRaw: List<Trip>,
        private val refill: EasyCardTopUp
) : TransitData() {
    constructor(card: ClassicCard) : this(
            parseBalance(card),
            EasyCardTransaction.parseTrips(card),
            EasyCardTopUp.parse(card)
    )

    override val balance get() = TransitCurrency.TWD(balanceRaw)

    override val cardName get() = NAME

    override val serialNumber get(): String? = null

    override val trips get() = tripsRaw + listOf(refill)

    companion object {
        private val TZ = TimeZone.getTimeZone("Asia/Taipei")

        internal const val NAME = "EasyCard"
        private val CARD_INFO = CardInfo.Builder()
                .setImageId(R.drawable.tpe_easy_card, R.drawable.iso7810_id1_alpha)
                .setName(NAME)
                .setLocation(R.string.location_taipei)
                .setCardType(CardType.MifareClassic)
                .setKeysRequired()
                .setPreview()
                .build()

        internal val MAGIC = ImmutableByteArray.fromHex("0e140001070208030904081000000000")

        internal const val EASYCARD_STR = "easycard"

        private fun parseBalance(card: ClassicCard): Int {
            return card[2, 0].data.byteArrayToIntReversed(0, 4)
        }

        internal fun parseTimestamp(ts: Long?): Calendar? {
            val g = GregorianCalendar(TZ)
            g.timeInMillis = (ts ?: return null) * 1000
            return g
        }

        val FACTORY = object : ClassicCardTransitFactory {
            override fun earlyCheck(sectors: List<ClassicSector>) = sectors[0][1].data?.let {
                it == MAGIC
            } ?: false

            override fun earlySectors() = 1

            override fun parseTransitIdentity(card: ClassicCard) = TransitIdentity(NAME, null)

            override fun parseTransitData(card: ClassicCard) = EasyCardTransitData(card)

            override fun getAllCards() = listOf(CARD_INFO)
        }
    }
}