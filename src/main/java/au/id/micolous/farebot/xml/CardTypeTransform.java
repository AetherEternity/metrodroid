package au.id.micolous.farebot.xml;

import au.id.micolous.farebot.card.CardType;

import org.simpleframework.xml.transform.Transform;

public class CardTypeTransform implements Transform<CardType> {
    @Override public CardType read(String value) throws Exception {
        return CardType.class.getEnumConstants()[Integer.parseInt(value)];
    }

    @Override public String write(CardType value) throws Exception {
        return String.valueOf(value.toInteger());
    }
}
