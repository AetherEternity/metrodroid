/*
 * ISO7816SelectorByName.java
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

package au.id.micolous.metrodroid.card.iso7816;

import org.jetbrains.annotations.NonNls;
import org.simpleframework.xml.Element;

import java.io.IOException;

import au.id.micolous.metrodroid.util.Utils;
import au.id.micolous.metrodroid.xml.Base64String;
import au.id.micolous.metrodroid.util.ImmutableByteArray;

class ISO7816SelectorByName extends ISO7816SelectorElement {
    @Element(name="name")
    @NonNls
    private final Base64String mName;

    public static final String KIND = "name";

    ISO7816SelectorByName() { /* for XML serializer. */ this(ImmutableByteArray.Companion.empty()); }

    @Override
    ImmutableByteArray select(ISO7816Protocol tag) throws IOException, ISO7816Exception {
        return tag.selectByName(mName, false);
    }

    @Override
    @NonNls
    public String formatString() {
        return "#" + Utils.getHexString(mName.getData());
    }

    public ISO7816SelectorByName(@Element(name="name") ImmutableByteArray name) {
        super(KIND);
        mName = new Base64String(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ISO7816SelectorByName))
            return false;
        return ((ISO7816SelectorByName) obj).mName.equals(mName);
    }
}
