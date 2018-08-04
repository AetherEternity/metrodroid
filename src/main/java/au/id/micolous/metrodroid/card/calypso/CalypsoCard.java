/*
 * CalypsoCard.java
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.card.calypso;

import android.nfc.TagLostException;
import android.util.Log;

import org.simpleframework.xml.Root;

import java.io.IOException;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Application;
import au.id.micolous.metrodroid.card.iso7816.ISO7816File;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Protocol;
import au.id.micolous.metrodroid.card.iso7816.ISO7816Selector;
import au.id.micolous.metrodroid.util.Utils;

/**
 * Implements communication with Calypso cards.
 * <p>
 * This builds on top of the ISO7816 implementation, and pokes at certain file paths on the card.
 * <p>
 * References:
 * - https://github.com/L1L1/cardpeek/tree/master/dot_cardpeek_dir/scripts/calypso
 * - https://github.com/zoobab/mobib-extractor
 * - http://demo.calypsostandard.net/
 * - https://github.com/nfc-tools/libnfc/blob/master/examples/pn53x-tamashell-scripts/ReadMobib.sh
 * - https://github.com/nfc-tools/libnfc/blob/master/examples/pn53x-tamashell-scripts/ReadNavigo.sh
 */
public class CalypsoCard extends ISO7816Application {
    public static final byte[] CALYPSO_FILENAME = Utils.stringToByteArray("1TIC.ICA");
    private static final String TAG = CalypsoCard.class.getName();
    public static final String TYPE = "calypso";

    private CalypsoCard(ISO7816Application.ISO7816Info appData, boolean partialRead) {
        super(appData);
    }

    private CalypsoCard() {
        super(); /* For XML Serializer */
    }

    public static CalypsoCard dumpTag(ISO7816Protocol protocol, ISO7816Application.ISO7816Info appData,
                                      TagReaderFeedbackInterface feedbackInterface) throws IOException {
        // At this point, the connection is already open, we just need to dump the right things...

        feedbackInterface.updateStatusText(Utils.localizeString(R.string.calypso_reading));
        feedbackInterface.updateProgressBar(0, File.getAll().length);
        int counter = 0;
        boolean partialRead = false;

            for (File f : File.getAll()) {
                feedbackInterface.updateProgressBar(counter++, File.getAll().length);
                try {
                    appData.dumpFile(protocol, f.getSelector(), 0x1d);
                } catch (TagLostException e) {
                    Log.w(TAG, "tag lost", e);
                    partialRead = true;
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "couldn't select file", e);
                }
            }

        return new CalypsoCard(appData, partialRead);
    }

    public ISO7816File getFile(File f) {
        return getFile(f.getSelector());
    }

    public enum File {
        AID(0x3F04),
        ICC(0x0002),
        ID(0x0003),
        HOLDER_EXTENDED(0x3F1C),
        DISPLAY(0x2F10),

        TICKETING_ENVIRONMENT(0x2000, 0x2001),
        TICKETING_HOLDER(0x2000, 0x2002),
        TICKETING_AID(0x2000, 0x2004),
        TICKETING_LOG(0x2000, 0x2010),
        TICKETING_CONTRACTS_1(0x2000, 0x2020),
        TICKETING_CONTRACTS_2(0x2000, 0x2030),
        TICKETING_COUNTERS_1(0x2000, 0x202A),
        TICKETING_COUNTERS_2(0x2000, 0x202B),
        TICKETING_COUNTERS_3(0x2000, 0x202C),
        TICKETING_COUNTERS_4(0x2000, 0x202D),
        TICKETING_COUNTERS_5(0x2000, 0x202E),
        TICKETING_COUNTERS_6(0x2000, 0x202F),
        TICKETING_SPECIAL_EVENTS(0x2000, 0x2040),
        TICKETING_CONTRACT_LIST(0x2000, 0x2050),
        TICKETING_COUNTERS_7(0x2000, 0x2060),
        TICKETING_COUNTERS_8(0x2000, 0x2062),
        TICKETING_COUNTERS_9(0x2000, 0x2069),
        TICKETING_COUNTERS_10(0x2000, 0x206A),
        TICKETING_FREE(0x2000, 0x20F0),

        // Parking application (MPP)
        MPP_PUBLIC_PARAMETERS(0x3100, 0x3102),
        MPP_AID(0x3100, 0x3104),
        MPP_LOG(0x3100, 0x3115),
        MPP_CONTRACTS(0x3100, 0x3120),
        MPP_COUNTERS_1(0x3100, 0x3113),
        MPP_COUNTERS_2(0x3100, 0x3123),
        MPP_COUNTERS_3(0x3100, 0x3133),
        MPP_MISCELLANEOUS(0x3100, 0x3150),
        MPP_COUNTERS_4(0x3100, 0x3169),
        MPP_FREE(0x3100, 0x31F0),

        // Transport application (RT)
        RT2_ENVIRONMENT(0x2100, 0x2101),
        RT2_AID(0x2100, 0x2104),
        RT2_LOG(0x2100, 0x2110),
        RT2_CONTRACTS(0x2100, 0x2120),
        RT2_SPECIAL_EVENTS(0x2100, 0x2140),
        RT2_CONTRACT_LIST(0x2100, 0x2150),
        RT2_COUNTERS(0x2100, 0x2169),
        RT2_FREE(0x2100, 0x21F0),

        EP_AID(0x1000, 0x1004),
        EP_LOAD_LOG(0x1000, 0x1014),
        EP_PURCHASE_LOG(0x1000, 0x1015),

        ETICKET(0x8000, 0x8004),
        ETICKET_EVENT_LOGS(0x8000, 0x8010),
        ETICKET_PRESELECTION(0x8000, 0x8030);

        private int mFolder;
        private int mFile;

        File(int file) {
            this(0, file);
        }

        File(int folder, int file) {
            mFolder = folder;
            mFile = file;
        }

        public static File[] getAll() {
            return File.class.getEnumConstants();
        }

        public int getFile() {
            return mFile;
        }

        public int getFolder() {
            return mFolder;
        }

        public ISO7816Selector getSelector() {
            if (mFolder == 0)
                return ISO7816Selector.makeSelector(mFile);
            return ISO7816Selector.makeSelector(mFolder, mFile);
        }
    }
}
