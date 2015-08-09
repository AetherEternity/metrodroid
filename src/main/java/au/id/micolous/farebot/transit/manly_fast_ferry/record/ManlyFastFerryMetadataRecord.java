package au.id.micolous.farebot.transit.manly_fast_ferry.record;

import android.os.Parcel;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import au.id.micolous.farebot.transit.manly_fast_ferry.record.ManlyFastFerryRegularRecord;
import au.id.micolous.farebot.util.Utils;

/**
 * Represents a "preamble" type record.
 */
public class ManlyFastFerryMetadataRecord extends ManlyFastFerryRegularRecord {
    private String mCardSerial;
    private GregorianCalendar mEpochDate;

    private static GregorianCalendar MANLY_BASE_EPOCH = new GregorianCalendar(2000, Calendar.JANUARY, 1);

    public static ManlyFastFerryMetadataRecord recordFromBytes(byte[] input) {
        assert input[0] == 0x02;
        assert input[1] == 0x03;

        ManlyFastFerryMetadataRecord record = new ManlyFastFerryMetadataRecord();

        int epochDays = Utils.byteArrayToInt(input, 5, 2);

        record.mCardSerial = Utils.getHexString(Arrays.copyOfRange(input, 7, 11));

        record.mEpochDate = new GregorianCalendar();
        record.mEpochDate.setTimeInMillis(MANLY_BASE_EPOCH.getTimeInMillis());
        record.mEpochDate.add(Calendar.DATE, epochDays);

        return record;
    }

    protected ManlyFastFerryMetadataRecord() {}

    public String getCardSerial() { return mCardSerial; }
    public GregorianCalendar getEpochDate() { return mEpochDate; }
}
