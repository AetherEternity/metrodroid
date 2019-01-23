/*
 * SeqGoTransitData.java
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.seq_go;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.classic.ClassicCard;
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory;
import au.id.micolous.metrodroid.card.classic.ClassicSector;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTransitData;
import au.id.micolous.metrodroid.transit.nextfare.NextfareTrip;
import au.id.micolous.metrodroid.transit.nextfare.record.NextfareTopupRecord;
import au.id.micolous.metrodroid.util.StationTableReader;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.ImmutableByteArray;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * Transit data type for Go card (Brisbane / South-East Queensland, AU), used by Translink.
 * <p>
 * Documentation of format: https://github.com/micolous/metrodroid/wiki/Go-%28SEQ%29
 *
 * @author Michael Farrell
 */
public class SeqGoTransitData extends NextfareTransitData {

    private static final String NAME = "Go card";
    public static final Creator<SeqGoTransitData> CREATOR = new Creator<SeqGoTransitData>() {
        public SeqGoTransitData createFromParcel(Parcel parcel) {
            return new SeqGoTransitData(parcel);
        }

        public SeqGoTransitData[] newArray(int size) {
            return new SeqGoTransitData[size];
        }
    };

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.seqgo_card, R.drawable.seqgo_card_alpha)
            .setName(SeqGoTransitData.NAME)
            .setLocation(R.string.location_brisbane_seq_australia)
            .setCardType(CardType.MifareClassic)
            .setKeysRequired()
            .setExtraNote(R.string.card_note_seqgo)
            .build();

    @VisibleForTesting
    public static final ImmutableByteArray SYSTEM_CODE1 = ImmutableByteArray.Companion.fromHex(
            "5A5B20212223"
    );

    @VisibleForTesting
    public static final ImmutableByteArray SYSTEM_CODE2 = ImmutableByteArray.Companion.fromHex(
            "202122230101"
    );

    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Australia/Brisbane");

    private static final String TAG = "SeqGoTransitData";
    private final SeqGoTicketType mTicketType;

    private SeqGoTransitData(Parcel parcel) {
        super(parcel, "AUD");
        mTicketType = (SeqGoTicketType) parcel.readSerializable();
    }

    private SeqGoTransitData(ClassicCard card) {
        super(card, "AUD");
        if (mConfig != null) {
            mTicketType = SeqGoData.TICKET_TYPE_MAP.get(mConfig.getTicketType(), SeqGoTicketType.UNKNOWN);
        } else
            mTicketType = SeqGoTicketType.UNKNOWN;
    }

    public static final ClassicCardTransitFactory FACTORY = new NextFareTransitFactory() {
        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ClassicCard card) {
            return super.parseTransitIdentity(card, NAME);
        }

        @Override
        public boolean earlyCheck(@NonNull List<ClassicSector> sectors) {
            ClassicSector sector0 = sectors.get(0);
            ImmutableByteArray blockData = sector0.getBlock(1).getData();
            if (!blockData.copyOfRange(1, 9).contentEquals(MANUFACTURER)) {
                return false;
            }

            ImmutableByteArray systemCode = blockData.copyOfRange(9, 15);
            //Log.d(TAG, "SystemCode = " + Utils.getHexString(systemCode));
            return systemCode.contentEquals(SYSTEM_CODE1) || systemCode.contentEquals(SYSTEM_CODE2);
        }

        @Override
        public TransitData parseTransitData(@NonNull ClassicCard classicCard) {
            return new SeqGoTransitData(classicCard);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeSerializable(mTicketType);
    }

    @Override
    protected NextfareTrip newTrip() {
        return new SeqGoTrip();
    }

    @Override
    protected NextfareTrip newRefill(NextfareTopupRecord record) {
        return new SeqGoRefill(record);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public String getMoreInfoPage() {
        return "https://micolous.github.io/metrodroid/seqgo";
    }

    @Override
    public String getOnlineServicesPage() {
        return "https://gocard.translink.com.au/";
    }

    /**
     * The base implementation of hasUnknownStations from Nextfare always returns false, but we can
     * return the correct value for Go card.
     *
     * @return true if there are unknown station IDs on the card.
     */
    @Override
    public boolean hasUnknownStations() {
        return mHasUnknownStations;
    }

    @Override
    public String getTicketClass() {
        return Localizer.INSTANCE.localizeString(mTicketType.getDescription());
    }

    @Override
    protected TimeZone getTimezone() {
        return TIME_ZONE;
    }

    @Nullable
    public static String getNotice() {
        return StationTableReader.getNotice(SeqGoData.SEQ_GO_STR);
    }
}
