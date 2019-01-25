/*
 * NewShenzhenTransitData.java
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

package au.id.micolous.metrodroid.transit.china;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.id.micolous.farebot.R;
import au.id.micolous.metrodroid.card.CardType;
import au.id.micolous.metrodroid.card.china.ChinaCard;
import au.id.micolous.metrodroid.card.china.ChinaCardTransitFactory;
import au.id.micolous.metrodroid.multi.Localizer;
import au.id.micolous.metrodroid.transit.CardInfo;
import au.id.micolous.metrodroid.transit.TransitData;
import au.id.micolous.metrodroid.transit.TransitIdentity;
import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

// Reference: https://github.com/sinpolib/nfcard/blob/master/src/com/sinpo/xnfc/nfc/reader/pboc/BeijingMunicipal.java
public class BeijingTransitData extends ChinaTransitData {
    private static final int FILE_INFO = 0x4;
    private final String mSerial;

   private static final CardInfo CARD_INFO = new CardInfo.Builder()
            .setName(Localizer.INSTANCE.localizeString(R.string.card_name_beijing))
            .setLocation(R.string.location_beijing)
            .setCardType(CardType.ISO7816)
            .setPreview()
            .build();

    public static final Creator<BeijingTransitData> CREATOR = new Creator<BeijingTransitData>() {
        public BeijingTransitData createFromParcel(Parcel parcel) {
            return new BeijingTransitData(parcel);
        }

        public BeijingTransitData[] newArray(int size) {
            return new BeijingTransitData[size];
        }
    };

    private BeijingTransitData(ChinaCard card) {
        super(card);
        mSerial = parseSerial(card);
        ImmutableByteArray info = getFile(card, FILE_INFO).getBinaryData();

        mValidityStart = info.byteArrayToInt(0x18, 4);
        mValidityEnd = info.byteArrayToInt(0x1c, 4);
    }

    @Override
    protected ChinaTrip parseTrip(ImmutableByteArray data) {
        return new ChinaTrip(data);
    }

    @Override
    public String getSerialNumber() {
        return mSerial;
    }

    @Override
    public String getCardName() {
        return Localizer.INSTANCE.localizeString(R.string.card_name_beijing);
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeString(mSerial);
    }

    private BeijingTransitData(Parcel parcel) {
        super(parcel);
        mSerial = parcel.readString();
    }

    public final static ChinaCardTransitFactory FACTORY = new ChinaCardTransitFactory() {
        @Override
        public List<ImmutableByteArray> getAppNames() {
            return Arrays.asList(
                    ImmutableByteArray.Companion.fromASCII("OC"),
                    ImmutableByteArray.Companion.fromASCII("PBOC")
            );
        }

        @Override
        public TransitIdentity parseTransitIdentity(@NonNull ChinaCard card) {
            return new TransitIdentity(Localizer.INSTANCE.localizeString(R.string.card_name_beijing), parseSerial(card));
        }

        @Override
        public TransitData parseTransitData(@NonNull ChinaCard chinaCard) {
            return new BeijingTransitData(chinaCard);
        }

        @NonNull
        @Override
        public List<CardInfo> getAllCards() {
            return Collections.singletonList(CARD_INFO);
        }
    };

    private static String parseSerial(ChinaCard card) {
        ImmutableByteArray info = getFile(card, FILE_INFO).getBinaryData();
        return info.getHexString(0, 8);
    }
}
