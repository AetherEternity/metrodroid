package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.card.CardImporter
import au.id.micolous.metrodroid.transit.TransitData
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Base class for building tests that need Assets data.
 *
 * @param C A [Card] subclass for the type of media to accept.
 * @param importer A reference to a [CardImporter] which produces [C].
 */
abstract class CardReaderWithAssetDumpsTest(
        val importer: CardImporter<out Card>
) : BaseInstrumentedTest() {
    /**
     * Parses a card and checks that it was the correct reader.
     */
    inline fun <reified TD: TransitData>parseCard(c: Card): TD {
        val d = c.parseTransitData()
        assertNotNull(d, "Transit data not parsed. Card $c")
        assertTrue(d is TD,
                "Transit data is not of right type")
        return d
    }

    /**
     * Loads a card dump from assets.
     *
     * The non-tests versions of Metrodroid must not contain any of this sort of data. It is only
     * useful for validating publicly published dump files.
     *
     * The preference is to include third-party dumps from git submodules, and then include them
     * with Gradle. Alternatively, files can go into <code>third_party/</code> with a
     * <code>README</code>.
     *
     * @param path Path to the dump, relative to <code>/assets/</code>
     * @return Parsed [C] from the file.
     */
    inline fun <reified C: Card>loadCard(path: String): C {
        val card = importer.readCard(loadAsset(path))
        assertNotNull(card)
        assertTrue(card is C)
        return card
    }

    inline fun <reified TD: TransitData>loadAndParseCard(path: String): TD {
        return parseCard(loadCard(path))
    }
}