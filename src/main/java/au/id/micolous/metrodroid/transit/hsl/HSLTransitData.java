/*
 * HSLTransitData.java
 *
 * Copyright 2013 Lauri Andler <lauri.andler@gmail.com>
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

package au.id.micolous.metrodroid.transit.hsl;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.Card;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.desfire.DesfireCard;
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory;
import au.id.micolous.metrodroid.card.desfire.files.DesfireFile;
import au.id.micolous.metrodroid.card.desfire.files.RecordDesfireFile;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitCurrency;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.transit.Trip;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

/**
 * Implements a reader for HSL transit cards.
 *
 * Documentation and sample libraries for this are available at:
 * http://dev.hsl.fi/#travel-card
 *
 * The documentation (in Finnish) is available at:
 * http://dev.hsl.fi/hsl-card-java/HSL-matkakortin-kuvaus.pdf
 *
 * Machine translation to English:
 * https://translate.google.com/translate?sl=auto&tl=en&js=y&prev=_t&hl=en&ie=UTF-8&u=http%3A%2F%2Fdev.hsl.fi%2Fhsl-card-java%2FHSL-matkakortin-kuvaus.pdf&edit-text=&act=url
 */
public class HSLTransitData extends TransitData implements Parcelable {

    private static final TimeZone TZ = TimeZone.getTimeZone("Europe/Helsinki");
    private static final long EPOCH;

    static {
        GregorianCalendar epoch = new GregorianCalendar(TZ);
        epoch.set(1997, Calendar.JANUARY,1, 0, 0, 0);

        EPOCH = epoch.getTimeInMillis();
    }

    private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setImageId(R.drawable.hsl_card)
            .setName("HSL")
            .setLocation(R.string.location_helsinki_finland)
            .setCardType(CardType.MifareDesfire)
            .build();

    private static final int APP_ID = 0x1120ef;
    private final String mSerialNumber;
    private final int mBalance;
    private final List<HSLTrip> mTrips;
    private HSLRefill mLastRefill;
    private final long mArvoExit;
    private final int mArvoPax;
    private final int mArvoPurchasePrice;
    private final long mArvoXfer;
    private final long mArvoDiscoGroup;
    private final long mArvoMystery1;
    private final long mArvoDuration;
    private final long mArvoRegional;
    private final long mArvoJOREExt;
    private final int mArvoVehicleNumber;
    private final long mArvoUnknown;
    private final long mArvoLineJORE;
    private final int mKausiVehicleNumber;
    private final long mKausiUnknown;
    private final long mKausiLineJORE;
    private final long mKausiJOREExt;
    private final long mArvoDirection;
    private final long mKausiDirection;
    /*
    private static final String[] regionNames = {
        "N/A", "Helsinki", "Espoo", "Vantaa", "Koko alue", "Seutu", "", "", "", "",  // 0-9
        "", "", "", "", "", "", "", "", "", "", // 10-19
        "", "", "", "", "", "", "", "", "", "", // 20-29
        "", "", "", "", "", "", "", "", "", ""}; // 30-39
        */
/*    private static final Map<Long,String> vehicleNames =  Collections.unmodifiableMap(new HashMap<Long, String>() {{
        put(1L, "Metro");
        put(18L, "Bus");
        put(16L, "Tram");
    }});*/

    private HSLTransitData(Parcel parcel) {
        mSerialNumber = parcel.readString();
        mBalance = parcel.readInt();
        mArvoMystery1 = parcel.readLong();
        mArvoDuration = parcel.readLong();
        mArvoRegional = parcel.readLong();
        mArvoExit = parcel.readLong();
        mArvoPurchasePrice = parcel.readInt();
        mArvoDiscoGroup = parcel.readLong();
        mArvoPax = parcel.readInt();
        mArvoXfer = parcel.readLong();
        mArvoVehicleNumber = parcel.readInt();
        mArvoUnknown = parcel.readLong();
        mArvoLineJORE = parcel.readLong();
        mArvoJOREExt = parcel.readLong();
        mArvoDirection = parcel.readLong();
        mKausiVehicleNumber = parcel.readInt();
        mKausiUnknown = parcel.readLong();
        mKausiLineJORE = parcel.readLong();
        mKausiJOREExt = parcel.readLong();
        mKausiDirection = parcel.readLong();

        mTrips = new ArrayList<>();
        parcel.readTypedList(mTrips, HSLTrip.CREATOR);
    }

    private HSLTransitData(DesfireCard desfireCard) {
        ImmutableByteArray data;

        try {
            data = desfireCard.getApplication(APP_ID).getFile(0x08).getData();
            mSerialNumber = data.toHexString().substring(2, 20);  //data.byteArrayToInt(1, 9);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL serial", ex);
        }

        try {
            data = desfireCard.getApplication(APP_ID).getFile(0x02).getData();
            mBalance = data.getBitsFromBuffer(0, 20);
            mLastRefill = new HSLRefill(data);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL refills", ex);
        }

        try {
            mTrips = parseTrips(desfireCard);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL trips", ex);
        }

        int balanceIndex = -1;

        for (int i = 0; i < mTrips.size(); ++i) {
            if (mTrips.get(i).mArvo == 1) {
                balanceIndex = i;
                break;
            }
        }

        try {
            data = desfireCard.getApplication(APP_ID).getFile(0x03).getData();
            mArvoMystery1 = data.getBitsFromBuffer(0, 9);
            mArvoDiscoGroup = data.getBitsFromBuffer(9, 5);
            mArvoDuration = data.getBitsFromBuffer(14, 13);
            mArvoRegional = data.getBitsFromBuffer(27, 5);

            mArvoExit = cardDateToTimestamp(
                    data.getBitsFromBuffer(32, 14),
                    data.getBitsFromBuffer(46, 11));

            //68 price, 82 zone?
            mArvoPurchasePrice = data.getBitsFromBuffer(68, 14);
            //mArvoDiscoGroup = data.getBitsFromBuffer(82, 6);
            Calendar mArvoPurchase = cardDateToCalendar(
                    data.getBitsFromBuffer(88, 14),
                    data.getBitsFromBuffer(102, 11));

            Calendar mArvoExpire = cardDateToCalendar(
                    data.getBitsFromBuffer(113, 14),
                    data.getBitsFromBuffer(127, 11));

            mArvoPax = data.getBitsFromBuffer(138, 6);

            mArvoXfer = cardDateToTimestamp(
                    data.getBitsFromBuffer(144, 14),
                    data.getBitsFromBuffer(158, 11));

            mArvoVehicleNumber = data.getBitsFromBuffer(169, 14);

            mArvoUnknown = data.getBitsFromBuffer(183, 2);

            mArvoLineJORE = data.getBitsFromBuffer(185, 14);
            mArvoJOREExt = data.getBitsFromBuffer(199, 4);
            mArvoDirection = data.getBitsFromBuffer(203, 1);

            if (balanceIndex > -1) {
                mTrips.get(balanceIndex).mLine = Long.toString(mArvoLineJORE);
                mTrips.get(balanceIndex).mVehicleNumber = mArvoVehicleNumber;
            } else if (mArvoPurchase.getTimeInMillis() > 2) {
                HSLTrip t = new HSLTrip();
                t.mArvo = 1;
                t.mExpireTimestamp = mArvoExpire;
                t.mFare = mArvoPurchasePrice;
                t.mPax = mArvoPax;
                t.mTimestamp = mArvoPurchase;
                t.mVehicleNumber = mArvoVehicleNumber;
                t.mLine = Long.toString(mArvoLineJORE);
                mTrips.add(t);
                Collections.sort(mTrips, new Trip.Comparator());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL value data", ex);
        }

        int seasonIndex = -1;
        for (int i = 0; i < mTrips.size(); ++i) {
            if (mTrips.get(i).mArvo == 0) {
                seasonIndex = i;
                break;
            }
        }

        try {
            data = desfireCard.getApplication(APP_ID).getFile(0x01).getData();

            if (data.getBitsFromBuffer(19, 14) == 0 && data.getBitsFromBuffer(67, 14) == 0) {
                boolean mKausiNoData = true;
            }

            long mKausiStart = cardDateToTimestamp(data.getBitsFromBuffer(19, 14), 0);
            long mKausiEnd = cardDateToTimestamp(data.getBitsFromBuffer(33, 14), 0);
            long mKausiPrevStart = cardDateToTimestamp(data.getBitsFromBuffer(67, 14), 0);
            long mKausiPrevEnd = cardDateToTimestamp(data.getBitsFromBuffer(81, 14), 0);
            if (mKausiPrevStart > mKausiStart) {
                long temp = mKausiStart;
                long temp2 = mKausiEnd;
                mKausiStart = mKausiPrevStart;
                mKausiEnd = mKausiPrevEnd;
                mKausiPrevStart = temp;
                mKausiPrevEnd = temp2;
            }
            Calendar mKausiPurchase = cardDateToCalendar(
                    data.getBitsFromBuffer(110, 14),
                    data.getBitsFromBuffer(124, 11));
            int mKausiPurchasePrice = data.getBitsFromBuffer(149, 15);
            long mKausiLastUse = cardDateToTimestamp(
                    data.getBitsFromBuffer(192, 14),
                    data.getBitsFromBuffer(206, 11));
            mKausiVehicleNumber = data.getBitsFromBuffer(217, 14);
            //mTrips[0].mVehicleNumber = mArvoVehicleNumber;

            mKausiUnknown = data.getBitsFromBuffer(231, 2);

            mKausiLineJORE = data.getBitsFromBuffer(233, 14);
            //mTrips[0].mLine = Long.toString(mArvoLineJORE).substring(1);

            mKausiJOREExt = data.getBitsFromBuffer(247, 4);
            mKausiDirection = data.getBitsFromBuffer(241, 1);
            if (seasonIndex > -1) {
                mTrips.get(seasonIndex).mVehicleNumber = mKausiVehicleNumber;
                mTrips.get(seasonIndex).mLine = Long.toString(mKausiLineJORE);
            } else if (mKausiVehicleNumber > 0) {
                HSLTrip t = new HSLTrip();
                t.mArvo = 0;
                t.mExpireTimestamp = mKausiPurchase;
                t.mFare = mKausiPurchasePrice;
                t.mPax = 1;
                t.mTimestamp = mKausiPurchase;
                t.mVehicleNumber = mKausiVehicleNumber;
                t.mLine = Long.toString(mKausiLineJORE);
                mTrips.add(t);
                Collections.sort(mTrips, new Trip.Comparator());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing HSL kausi data", ex);
        }
    }

    public static final Creator<HSLTransitData> CREATOR = new Creator<HSLTransitData>() {
        @Override
        public HSLTransitData createFromParcel(Parcel in) {
            return new HSLTransitData(in);
        }

        @Override
        public HSLTransitData[] newArray(int size) {
            return new HSLTransitData[size];
        }
    };

    public final static DesfireCardTransitFactory FACTORY = new DesfireCardTransitFactory() {
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
            return new HSLTransitData(desfireCard);
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull DesfireCard card) {
            try {
                ImmutableByteArray data = card.getApplication(APP_ID).getFile(0x08).getData();
                return new TransitIdentity("HSL", data.toHexString().substring(2, 20));
            } catch (Exception ex) {
                throw new RuntimeException("Error parsing HSL serial", ex);
            }
        }
    };

    static Calendar cardDateToCalendar(long day, long minute) {
        GregorianCalendar c = new GregorianCalendar(TZ);
        c.setTimeInMillis(EPOCH);
        c.add(Calendar.DAY_OF_YEAR, (int)day);
        c.add(Calendar.MINUTE, (int)minute);
        return c;
    }

    private static long cardDateToTimestamp(long day, long minute) {
        return (EPOCH) + day * (60 * 60 * 24) + minute * 60;
    }

    @Override
    public String getCardName() {
        return "HSL";
    }

    /*
    public String getCustomString() {
        DateFormat shortDateTimeFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        DateFormat shortDateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT);

        StringBuilder ret = new StringBuilder();
        if (!mKausiNoData) {
            ret.append(GR(R.string.hsl_season_ticket)).append(":\n");
            ret.append(GR(R.string.hsl_value_ticket_vehicle_number)).append(": ")
            .append(mKausiVehicleNumber).append("\n");
            ret.append(GR(R.string.hsl_value_ticket_line_number)).append(": ")
            .append(Long.toString(mKausiLineJORE).substring(1)).append("\n");
            ret.append("JORE extension").append(": ").append(mKausiJOREExt).append("\n");
            ret.append("Direction").append(": ").append(mKausiDirection).append("\n");

            ret.append(GR(R.string.hsl_season_ticket_starts)).append(": ")
            .append(shortDateFormat.format(mKausiStart * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_season_ticket_ends)).append(": ")
            .append(shortDateFormat.format(mKausiEnd * 1000.0));
            ret.append("\n\n");
            ret.append(GR(R.string.hsl_season_ticket_bought_on)).append(": ")
            .append(shortDateTimeFormat.format(mKausiPurchase * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_season_ticket_price_was)).append(": ")
            .append(NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mKausiPurchasePrice / 100.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_you_last_used_this_ticket)).append(": ")
            .append(shortDateTimeFormat.format(mKausiLastUse * 1000.0));
            ret.append("\n");
            ret.append(GR(R.string.hsl_previous_season_ticket)).append(": ")
            .append(shortDateFormat.format(mKausiPrevStart * 1000.0));
            ret.append(" - ").append(shortDateFormat.format(mKausiPrevEnd * 1000.0));
            ret.append("\n\n");
        }

        ret.append(GR(R.string.hsl_value_ticket)).append(":\n");
        ret.append(GR(R.string.hsl_value_ticket_bought_on)).append(": ")
        .append(shortDateTimeFormat.format(mArvoPurchase * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_expires_on)).append(": ")
        .append(shortDateTimeFormat.format(mArvoExpire * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_last_transfer)).append(": ")
        .append(shortDateTimeFormat.format(mArvoXfer * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_last_sign)).append(": ")
        .append(shortDateTimeFormat.format(mArvoExit * 1000.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_price)).append(": ")
        .append(NumberFormat.getCurrencyInstance(Locale.GERMANY).format(mArvoPurchasePrice / 100.0)).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_disco_group)).append(": ").append(mArvoDiscoGroup).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_pax)).append(": ").append(mArvoPax).append("\n");
        ret.append("Mystery1").append(": ").append(mArvoMystery1).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_duration)).append(": ").append(mArvoDuration).append(" min\n");
        ret.append(GR(R.string.hsl_value_ticket_vehicle_number)).append(": ").append(mArvoVehicleNumber).append("\n");
        ret.append("Region").append(": ").append(regionNames[(int) mArvoRegional]).append("\n");
        ret.append(GR(R.string.hsl_value_ticket_line_number)).append(": ")
        .append(Long.toString(mArvoLineJORE).substring(1)).append("\n");
        ret.append("JORE extension").append(": ").append(mArvoJOREExt).append("\n");
        ret.append("Direction").append(": ").append(mArvoDirection).append("\n");

        return ret.toString();
    }
    */

    @Nullable
    @Override
    public TransitCurrency getBalance() {
        return TransitCurrency.EUR(mBalance);
    }

    // TODO: push these into Subscriptions
    /*
        if (mHasKausi)
            ret += "\n" + app.getString(R.string.hsl_pass_is_valid);
        if (mArvoExpire * 1000.0 > System.currentTimeMillis())
            ret += "\n" + app.getString(R.string.hsl_value_ticket_is_valid) + "!";
    */

    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public List<Trip> getTrips() {
        List<Trip> trips = new ArrayList<>(mTrips);
        trips.add(mLastRefill);
        Collections.sort(trips, new Trip.Comparator());

        return trips;
    }

    private List<HSLTrip> parseTrips(DesfireCard card) {
        DesfireFile file = card.getApplication(APP_ID).getFile(0x04);

        if (file instanceof RecordDesfireFile) {
            RecordDesfireFile recordFile = (RecordDesfireFile) file;

            List<HSLTrip> useLog = new ArrayList<>();
            for (int i = 0; i < recordFile.getRecords().size(); i++) {
                useLog.add(new HSLTrip(recordFile.getRecords().get(i)));
            }
            Collections.sort(useLog, new Trip.Comparator());
            return useLog;
        }
        return new ArrayList<>();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mSerialNumber);
        parcel.writeInt(mBalance);

        parcel.writeLong(mArvoMystery1);
        parcel.writeLong(mArvoDuration);
        parcel.writeLong(mArvoRegional);

        parcel.writeLong(mArvoExit);
        parcel.writeInt(mArvoPurchasePrice);
        parcel.writeLong(mArvoDiscoGroup);
        //parcel.writeLong(mArvoPurchase);
        //parcel.writeLong(mArvoExpire);
        parcel.writeInt(mArvoPax);
        parcel.writeLong(mArvoXfer);
        parcel.writeInt(mArvoVehicleNumber);
        parcel.writeLong(mArvoUnknown);
        parcel.writeLong(mArvoLineJORE);
        parcel.writeLong(mArvoJOREExt);
        parcel.writeLong(mArvoDirection);
        parcel.writeInt(mKausiVehicleNumber);
        parcel.writeLong(mKausiUnknown);
        parcel.writeLong(mKausiLineJORE);
        parcel.writeLong(mKausiJOREExt);
        parcel.writeLong(mKausiDirection);
        if (mTrips != null) {
            parcel.writeTypedList(mTrips);
        } else {
            parcel.writeInt(0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
