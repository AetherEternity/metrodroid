/*
 * OVChipIndex.java
 *
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2012 Eric Butler <eric@codebutler.com>
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

package au.id.micolous.metrodroid.transit.ovc

import android.os.Parcelable

import au.id.micolous.metrodroid.util.Utils
import kotlinx.android.parcel.Parcelize

import java.util.Arrays

@Parcelize
data class OVChipIndex internal constructor(
        val recentTransactionSlot: Boolean,  // Most recent transaction slot (0xFB0 (false) or 0xFD0 (true))
        val recentInfoSlot: Boolean,  // Most recent card information index slot (0x5C0 (true) or 0x580(false))
        val recentSubscriptionSlot: Boolean,   // Most recent subscription index slot (0xF10 (false) or 0xF30(true))
        val recentTravelhistorySlot: Boolean, // Most recent travel history index slot (0xF50 (false) or 0xF70 (true))
        val recentCreditSlot: Boolean,         // Most recent credit index slot (0xF90(false) or 0xFA0(true))
        val subscriptionIndex: List<Int>
) : Parcelable {

    companion object {
        fun parse(data: ByteArray): OVChipIndex {
            val firstSlot = Arrays.copyOfRange(data, 0, data.size / 2)
            val secondSlot = Arrays.copyOfRange(data, data.size / 2, data.size)

            val iIDa3 = Utils.getBitsFromBuffer(firstSlot, 10, 16)
            val iIDb3 = Utils.getBitsFromBuffer(secondSlot, 10, 16)

            val buffer = if (iIDb3 > iIDa3) secondSlot else firstSlot

            val indexes = Utils.getBitsFromBuffer(buffer, 31 * 8, 3)

            val subscriptionIndex = (0..11).map { i -> Utils.getBitsFromBuffer(buffer, 108 + i * 4, 4) }

            return OVChipIndex(recentTransactionSlot = iIDb3 <= iIDa3,
                    recentSubscriptionSlot = indexes and 0x04 != 0x00,
                    recentTravelhistorySlot = indexes and 0x02 != 0x00,
                    recentCreditSlot = indexes and 0x01 != 0x00,
                    recentInfoSlot = buffer[3].toInt() shr 5 and 0x01 != 0,
                    subscriptionIndex = subscriptionIndex)
        }
    }
}
