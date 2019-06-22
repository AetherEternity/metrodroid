/*
 * UmarshTransitData.kt
 *
 * Copyright 2019 Google
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
package au.id.micolous.metrodroid.transit.umarsh

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.multi.*
import au.id.micolous.metrodroid.time.Daystamp
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.ui.HeaderListItem
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils

internal enum class UmarshDenomination {
    UNLIMITED,
    TRIPS,
    RUB
}

internal data class UmarshTariff(
        val name: StringResource,
        val cardName: StringResource? = null,
        val denomination: UmarshDenomination? = null
)

internal data class UmarshSystem(
        val cardInfo: CardInfo,
        val tariffs: Map<Int, UmarshTariff> = emptyMap()
)

// This implements reader for umarsh format: https://umarsh.com
// Reference: https://github.com/micolous/metrodroid/wiki/Umarsh

private val systemsMap = mapOf(
        // This seems to be in reverse order of region numbers
        Pair(0x1cda, 66) to UmarshSystem(
                CardInfo(
                        name = R.string.card_name_ekarta,
                        cardType = CardType.MifareClassic,
                        locationId = R.string.location_ekaterinburg,
                        imageId = R.drawable.ekarta,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        keysRequired = true,
                        preview = true),
                tariffs = mapOf(
                        0x42640f to UmarshTariff(
                                name = R.string.monthly_subscription,
                                denomination = UmarshDenomination.UNLIMITED
                        )
                )
        ),
        Pair(0x2b9f, 91) to UmarshSystem(
                CardInfo(
                        name = R.string.card_name_crimea_trolleybus,
                        cardType = CardType.MifareClassic,
                        locationId = R.string.location_crimea,
                        imageId = R.drawable.crimea_trolley,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        keysRequired = true,
                        preview = true),
                tariffs = mapOf(
                        0x3d7f00 to UmarshTariff(
                                name = R.string.card_name_crimea_parus_school,
                                cardName = R.string.card_name_crimea_parus_school,
                                denomination = UmarshDenomination.UNLIMITED
                        ),
                        0x467f00 to UmarshTariff(
                                name = R.string.card_name_crimea_trolleybus,
                                cardName = R.string.card_name_crimea_trolleybus,
                                denomination = UmarshDenomination.UNLIMITED
                        )
                )
        ),
        Pair(0x2b9f, 12) to UmarshSystem(
                CardInfo(
                        name = R.string.card_name_yoshkar_ola,
                        cardType = CardType.MifareClassic,
                        locationId = R.string.location_yoshkar_ola,
                        imageId = R.drawable.yoshkar_ola,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        keysRequired = true,
                        preview = true),
                tariffs = mapOf(
                        0x287f00 to UmarshTariff(
                                name = R.string.card_name_yoshkar_ola
                        )
                )
        ),
        Pair(0x399f, 58) to UmarshSystem(
                CardInfo(
                        name = R.string.card_name_penza,
                        cardType = CardType.MifareClassic,
                        locationId = R.string.location_penza,
                        imageId = R.drawable.penza,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        keysRequired = true,
                        preview = true),
                tariffs = mapOf(
                        0x1400ff to UmarshTariff(R.string.umarsh_adult)
                )
        ),
        Pair(0x3d9f, 52) to UmarshSystem(
                CardInfo(
                        name = R.string.card_name_siticard,
                        cardType = CardType.MifareClassic,
                        locationId = R.string.location_nizhniy_novgorod,
                        imageId = R.drawable.siticard,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        keysRequired = true,
                        preview = true),
                tariffs = mapOf(
                        0x0a7f00 to UmarshTariff(
                                name = R.string.siticard_adult_60min_xfer_purse,
                                denomination = UmarshDenomination.RUB
                        ),
                        0x0a007f to UmarshTariff(
                                name = R.string.siticard_adult_60min_xfer_purse,
                                denomination = UmarshDenomination.RUB
                        ),
                        0x2564ff to UmarshTariff(
                                name = R.string.siticard_edinyj_3_days,
                                denomination = UmarshDenomination.UNLIMITED
                        ),
                        0x31002f to UmarshTariff(
                                name = R.string.siticard_adult_90min_xfer_purse,
                                denomination = UmarshDenomination.RUB
                        ),
                        0x33690f to UmarshTariff(
                                name = R.string.siticard_edinyj_16_trips,
                                denomination = UmarshDenomination.TRIPS
                        ),
                        0x34690f to UmarshTariff(
                                name = R.string.siticard_edinyj_30_trips,
                                denomination = UmarshDenomination.TRIPS
                        ),
                        0x3c7f00 to UmarshTariff(
                                name = R.string.siticard_aerial_tramway
                        )
                )
        ),
        Pair(0x4881, 43) to UmarshSystem(
                CardInfo(
                        name = R.string.card_name_kirov,
                        cardType = CardType.MifareClassic,
                        imageId = R.drawable.kirov,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        locationId = R.string.location_kirov,
                        keysRequired = true,
                        preview = true),
                tariffs = mapOf(
                        0x5000ff to UmarshTariff(R.string.umarsh_adult)
                )
        ),
        Pair(0x659f, 18) to UmarshSystem(
                CardInfo(
                        name = R.string.card_name_strizh,
                        cardType = CardType.MifareClassic,
                        locationId = R.string.location_izhevsk,
                        imageId = R.drawable.strizh,
                        imageAlphaId = R.drawable.iso7810_id1_alpha,
                        keysRequired = true,
                        preview = true),
                tariffs = mapOf(
                        0x0a7f00 to UmarshTariff(
                                name = R.string.umarsh_adult
                        ),
                        0x1e7f00 to UmarshTariff(
                                name = R.string.umarsh_student
                        ),
                        0x247f00 to UmarshTariff(
                                name = R.string.umarsh_school
                        ),
                        0x587f00 to UmarshTariff(
                                name = R.string.umarsh_adult
                        )
                )
        )
)

private fun formatSerial(sn: Int) = NumberUtils.formatNumber(sn.toLong(), " ", 3, 3, 3)

private fun parseDate(raw: ImmutableByteArray, off: Int) = if (raw.getBitsFromBuffer(off, 16) == 0) null else Daystamp(
        year = raw.getBitsFromBuffer(off, 7) + 2000,
        month = raw.getBitsFromBuffer(off + 7, 4) - 1,
        day = raw.getBitsFromBuffer(off + 11, 5)
)


@Parcelize
data class UmarshSector(val counter: Int, val serialNumber: Int,
                      val balanceRaw: Int, val systemNum: Int,
                      val total: Int,
                      val tariffRaw: Int,
                      val lastRefill: Daystamp?,
                      override val validTo: Daystamp?,
                      val refillCounter: Int,
                      val hash: ImmutableByteArray,
                      val mA: ImmutableByteArray,
                      val region: Int,
                      val mB: Int,
                      override val machineId: Int,
                      val mC: Int,
                      val mD: Int,
                      val mE: Int,
                      val secno: Int) : Subscription() {
    val hasExtraSector: Boolean get() = systemNum == 0x3d9f

    val header
        get() = when {
            secno == 8 -> null
            else -> HeaderListItem(R.string.siticard_aerial_tramway)
        }

    override val info
        get() = listOf(
                ListItem(R.string.purchase_date, lastRefill?.format()),
                ListItem(R.string.machine_id, machineId.toString())
        )

    val genericInfo
        get() = listOfNotNull(header) + listOf(
                ListItem(R.string.refill_counter, refillCounter.toString()),
                ListItem(R.string.zolotaya_korona_region, region.toString())
        ) + if (denomination == UmarshDenomination.RUB)
            listOf(
                    ListItem(FormattedString("Last refill"), lastRefill?.format()),
                    ListItem(R.string.machine_id, machineId.toString())
            )
        else
            emptyList()

    private val system get() = systemsMap.getValue(Pair(systemNum, region))

    val cardName get() = tariff?.cardName?.let { Localizer.localizeString(it) } ?: system.cardInfo.name

    internal val denomination
        get() = tariff?.denomination ?: if (total == 0) UmarshDenomination.RUB else UmarshDenomination.TRIPS

    private val tariff get() = system.tariffs[tariffRaw]

    override val subscriptionName: String?
        get() = tariff?.name?.let { Localizer.localizeString(it) }
                ?: Localizer.localizeString(R.string.unknown_format, NumberUtils.intToHex(tariffRaw))

    override val remainingTripCount: Int?
        get() = if (denomination == UmarshDenomination.TRIPS) balanceRaw else null

    override val totalTripCount: Int?
        get() = if (denomination == UmarshDenomination.TRIPS) total else null

    val balance: TransitBalance?
        get() = if (denomination == UmarshDenomination.RUB) TransitBalanceStored(
                TransitCurrency.RUB(balanceRaw * 100),
                subscriptionName,
                null,
                validTo
        )
        else
            null

    override fun getRawFields(level: TransitData.RawLevel): List<ListItem> = listOf(
            HeaderListItem("sector $secno")) + (
            if (level == TransitData.RawLevel.ALL)
                listOf(
                        ListItem(FormattedString("Ticket from"), validFrom?.format()),
                        ListItem(FormattedString("Ticket to"), validTo?.format()),
                        ListItem("Tariff", NumberUtils.intToHex(tariffRaw)),
                        ListItem("Total", total.toString()),
                        ListItem(FormattedString("Hash"), hash.toHexDump()))
            else
                emptyList()) + listOf(
            ListItem("counter", NumberUtils.intToHex(counter)),
            ListItem(FormattedString("A"), mA.toHexDump()),
            ListItem("B", NumberUtils.intToHex(mB)),
            ListItem("C", NumberUtils.intToHex(mC)),
            ListItem("D", NumberUtils.intToHex(mD)),
            ListItem("E", NumberUtils.intToHex(mE))
    )

    companion object {
        fun getRegion(sector: ClassicSector) = (sector[1].data.getBitsFromBuffer(100, 4)
                or (sector[1].data.getBitsFromBuffer(64, 3) shl 4))

        fun parse(sector: ClassicSector, secno: Int): UmarshSector =
                UmarshSector(counter = 0x7fffffff - sector[0].data.byteArrayToIntReversed(0, 4),
                        systemNum = sector[1].data.byteArrayToInt(0, 3),
                        mA = sector[1].data.sliceOffLen(3, 2),
                        total = sector[1].data.byteArrayToInt(5, 2),
                        refillCounter = sector[1].data.byteArrayToInt(7, 1),
                        region = getRegion(sector),
                        serialNumber = sector[1].data.getBitsFromBuffer(67, 29),
                        tariffRaw = sector[1].data.byteArrayToInt(13, 3),
                        mB = sector[1].data.getBitsFromBuffer(96, 4),
                        validTo = parseDate(sector[2].data, 0),
                        mC = sector[2].data.byteArrayToInt(2, 1),
                        machineId = sector[2].data.byteArrayToInt(3, 3),
                        lastRefill = parseDate(sector[2].data, 48),
                        mD = sector[2].data.getBitsFromBuffer(64, 1),
                        balanceRaw = sector[2].data.getBitsFromBuffer(65, 15),
                        mE = sector[2].data.byteArrayToInt(10, 1), // Looks like always 0x80
                        hash = sector[2].data.sliceOffLen(11, 5),
                        secno = secno)

        fun check(sector: ClassicSector): Boolean = Pair(sector[1].data.byteArrayToInt(0, 3), getRegion(sector)) in systemsMap
        internal fun system(sector: ClassicSector) = systemsMap[Pair(sector[1].data.byteArrayToInt(0, 3), getRegion(sector))]
    }
}

@Parcelize
data class UmarshTransitData(val sectors: List<UmarshSector>) : TransitData() {

    override val serialNumber get() = formatSerial(sectors.first().serialNumber)

    override val cardName get() = sectors.first().cardName

    override val balances: List<TransitBalance>?
        get() = sectors.mapNotNull { it.balance }

    override val subscriptions: List<Subscription>?
        get() = sectors.filter { it.denomination != UmarshDenomination.RUB }

    override val info: List<ListItem>?
        get() = sectors.flatMap { it.genericInfo }

    override fun getRawFields(level: RawLevel): List<ListItem>? = sectors.flatMap { it.getRawFields(level) }
}

object UmarshTransitFactory : ClassicCardTransitFactory {
    override val allCards = systemsMap.values.map { it.cardInfo } + listOf(
            CardInfo(
                    name = R.string.card_name_crimea_parus_school,
                    cardType = CardType.MifareClassic,
                    locationId = R.string.location_crimea,
                    imageId = R.drawable.parus_school,
                    imageAlphaId = R.drawable.iso7810_id1_alpha,
                    keysRequired = true,
                    preview = true)
    )

    override fun earlyCheck(sectors: List<ClassicSector>) = UmarshSector.check(sectors[8])

    override fun earlyCardInfo(sectors: List<ClassicSector>): CardInfo? = UmarshSector.system(sectors[8])?.cardInfo

    override val earlySectors get() = 9

    override fun parseTransitIdentity(card: ClassicCard): TransitIdentity {
        val sec = UmarshSector.parse(card[8], 8)
        return TransitIdentity(sec.cardName, formatSerial(sec.serialNumber))
    }

    override fun parseTransitData(card: ClassicCard): TransitData {
        val sec8 = UmarshSector.parse(card[8], 8)
        val secs = if (!sec8.hasExtraSector)
            listOf(sec8)
        else
            listOf(sec8, UmarshSector.parse(card[7], 7))
        return UmarshTransitData(secs)
    }
}