/*
 * AdelaideMetrocardTransitData.java
 *
 * Copyright 2015 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.transit.adelaide;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import au.id.micolous.metrodroid.time.Daystamp;
import au.id.micolous.metrodroid.time.Timestamp;
import au.id.micolous.metrodroid.time.TimestampFormatter;
import au.id.micolous.metrodroid.time.TimestampFull;
import au.id.micolous.metrodroid.transit.*;
import au.id.micolous.metrodroid.util.NumberUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireApplication;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup;
import au.id.micolous.metrodroid.transit.en1545.En1545TransitData;
import au.id.micolous.metrodroid.transit.intercode.IntercodeTransitData;
import au.id.micolous.metrodroid.ui.ListItem;
import au.id.micolous.metrodroid.util.TripObfuscator;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

public class AdelaideMetrocardTransitData extends En1545TransitData {
    public static final Parcelable.Creator<AdelaideMetrocardTransitData> CREATOR = new Parcelable.Creator<AdelaideMetrocardTransitData>() {
        public AdelaideMetrocardTransitData createFromParcel(Parcel parcel) {
            return new AdelaideMetrocardTransitData(parcel);
        }

        public AdelaideMetrocardTransitData[] newArray(int size) {
            return new AdelaideMetrocardTransitData[size];
        }
    };

    private static final int APP_ID = 0xb006f2;
    // Matches capitalisation used by agency (and on the card).
    private static final String NAME = "metroCARD";
    private final List<TransactionTripAbstract> mTrips;
    private final List<AdelaideSubscription> mSubs;
    private final AdelaideSubscription mPurse;

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(NAME)
            .setLocation(R.string.location_adelaide)
            .setCardType(CardType.MifareDesfire)
            .setExtraNote(R.string.card_note_adelaide)
            .setPreview()
            .build();

    private final long mSerial;

    private AdelaideMetrocardTransitData(DesfireCard card) {
        mSerial = getSerial(card.getTagId());
        DesfireApplication app = card.getApplication(APP_ID);

        // This is basically mapped from Intercode
        // 0 = TICKETING_ENVIRONMENT
        mTicketEnvParsed.append(app.getFile(0).getData(), IntercodeTransitData.TICKET_ENV_FIELDS);

        // 1 is 0-record file on all cards we've seen so far

        // 2 = TICKETING_CONTRACT_LIST, not really useful to use

        List<Transaction> transactionList = new ArrayList<>();

        // 3-6: TICKETING_LOG
        // 7: rotating pointer for log
        // 8 is "HID ADELAIDE" or "NoteAB ADELAIDE"
        // 9-0xb: TICKETING_SPECIAL_EVENTS
        for (int fileId : new int[]{3,4,5,6, 9, 0xa, 0xb}) {
            ImmutableByteArray data = app.getFile(fileId).getData();
            if (data.getBitsFromBuffer(0, 14) == 0)
                continue;
            transactionList.add(new AdelaideTransaction(data));
        }

        // c-f: locked counters
        mSubs = new ArrayList<>();
        AdelaideSubscription purse = null;
        // 10-13: contracts
        for (int fileId : new int[]{0x10, 0x11, 0x12, 0x13}) {
            ImmutableByteArray data = app.getFile(fileId).getData();
            if (data.getBitsFromBuffer(0, 7) == 0)
                continue;
            AdelaideSubscription sub = new AdelaideSubscription(data);
            if (sub.isPurse())
                purse = sub;
            else
                mSubs.add(sub);
        }

        mPurse = purse;

        // 14-17: zero-filled
        // 1b-1c: locked
        // 1d: empty
        // 1e: const

        mTrips = TransactionTrip.Companion.merge(transactionList);
    }

    @SuppressWarnings("unchecked")
    private AdelaideMetrocardTransitData(Parcel parcel) {
        super(parcel);
        mSerial = parcel.readLong();
        mTrips = parcel.readArrayList(AdelaideTransaction.class.getClassLoader());
        mSubs = parcel.readArrayList(AdelaideTransaction.class.getClassLoader());
        if (parcel.readInt() != 0)
            mPurse = new AdelaideSubscription(parcel);
        else
            mPurse = null;
    }

    @Override
    protected AdelaideLookup getLookup() {
        return AdelaideLookup.getInstance();
    }

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
        @Override
        public TransitIdentity parseTransitIdentity(@NonNull DesfireCard card) {
            return new TransitIdentity(NAME, formatSerial(getSerial(card.getTagId())));
        }

        @Override
        public boolean earlyCheck(int[] appIds) {
            return ArrayUtils.contains(appIds, APP_ID);
        }


        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }

        @Override
        public TransitData parseTransitData(@NonNull DesfireCard desfireCard) {
            return new AdelaideMetrocardTransitData(desfireCard);
        }
    };

    @NonNls
    private static String formatSerial(long serial) {
        return "01-" + NumberUtils.INSTANCE.formatNumber(serial, " ", 3, 4, 4, 4);
    }

    private static long getSerial(ImmutableByteArray tagId) {
        return tagId.byteArrayToLongReversed(1, 6);
    }

    @Override
    public String getSerialNumber() {
        return formatSerial(mSerial);
    }

    @Override
    public String getCardName() {
        return NAME;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeLong(mSerial);
        parcel.writeList(mTrips);
        parcel.writeList(mSubs);
        parcel.writeInt(mPurse != null ? 1 : 0);
        if (mPurse != null)
            mPurse.writeToParcel(parcel, i);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public List<TransactionTripAbstract> getTrips() {
        return mTrips;
    }

    @Override
    public List<AdelaideSubscription> getSubscriptions() {
        if (mSubs.isEmpty())
            return null;
        return mSubs;
    }

    @Override
    public List<ListItem> getInfo() {
        List<ListItem> items = super.getInfo();
        if (mPurse != null) {
            items.add(new ListItem(R.string.ticket_type, mPurse.getSubscriptionName()));

            if (mPurse.getMachineId() != null) {
                items.add(new ListItem(R.string.machine_id,
                        Integer.toString(mPurse.getMachineId())));
            }

            Timestamp purchaseTS = mPurse.getPurchaseTimestamp();
            if (purchaseTS != null) {
                if (purchaseTS instanceof TimestampFull)
                    purchaseTS = TripObfuscator.INSTANCE.maybeObfuscateTS((TimestampFull) purchaseTS);
                else
                    purchaseTS = TripObfuscator.INSTANCE.maybeObfuscateTS((Daystamp) purchaseTS);

                items.add(new ListItem(R.string.issue_date, TimestampFormatter.INSTANCE.dateFormat(purchaseTS)));
            }

            Integer purseId = mPurse.getId();
            if (purseId != null)
                items.add(new ListItem(R.string.purse_serial_number, Integer.toHexString(purseId)));
        }
        return items;
    }
}
