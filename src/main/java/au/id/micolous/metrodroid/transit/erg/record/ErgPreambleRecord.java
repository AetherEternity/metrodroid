/*
 * ErgPreambleRecord.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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

package au.id.micolous.metrodroid.transit.erg.record;

import au.id.micolous.metrodroid.transit.erg.ErgTransitData;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

import java.util.Arrays;
import java.util.Locale;

/**
 * Represents a preamble record.
 *
 * https://github.com/micolous/metrodroid/wiki/ERG-MFC#preamble-record
 */
public class ErgPreambleRecord extends ErgRecord {
    private static final byte[] OLD_CARD_ID = {0x00, 0x00, 0x00};
    private String mCardSerial;

    private ErgPreambleRecord() {
    }

    public static ErgPreambleRecord recordFromBytes(ImmutableByteArray input) {
        ErgPreambleRecord record = new ErgPreambleRecord();

        // Check that the record is valid for a preamble
        if (!input.copyOfRange(0, ErgTransitData.SIGNATURE.length).contentEquals(ErgTransitData.SIGNATURE)) {
            throw new IllegalArgumentException("Preamble signature does not match");
        }

        // This is not set on 2012-era cards
        if (input.copyOfRange(10, 13).contentEquals(OLD_CARD_ID)) {
            record.mCardSerial = null;
        } else {
            record.mCardSerial = input.getHexString(10, 4);
        }
        return record;
    }

    /**
     * Returns the card serial number. Returns null on old cards.
     */
    public String getCardSerial() {
        return mCardSerial;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "[%s: serial=%s]",
                getClass().getSimpleName(),
                getCardSerial() == null ? "null" : getCardSerial());
    }
}
