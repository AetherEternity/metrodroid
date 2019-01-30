/*
 * ClassicReader.kt
 *
 * Copyright 2012-2015 Eric Butler <eric@codebutler.com>
 * Copyright 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
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

package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.card.CardLostException
import au.id.micolous.metrodroid.card.CardTransceiveException
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.key.CardKeysRetriever
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.util.ImmutableByteArray

object ClassicReader {
    private fun readSectorWithKey(tech: ClassicCardTech, sectorIndex: Int,
                                  correctKey: ClassicSectorKey): ClassicSectorRaw {
        val blocks = mutableListOf<ImmutableByteArray>()
        // FIXME: First read trailer block to get type of other blocks.
        val firstBlockIndex = tech.sectorToBlock(sectorIndex)
        for (blockIndex in 0 until tech.getBlockCountInSector(sectorIndex)) {
            var data = tech.readBlock(firstBlockIndex + blockIndex)

            // Sometimes the result is just a single byte 04
            // Reauthenticate if that happens
            repeat(3) {
                if (data.size == 1) {
                    tech.authenticate(sectorIndex, correctKey)
                    data = tech.readBlock(firstBlockIndex + blockIndex)
                }
            }

            blocks.add(data)
        }
        if (correctKey.type == ClassicSectorKey.KeyType.B)
            return ClassicSectorRaw(blocks = blocks, keyA = null, keyB = correctKey.key)
        return ClassicSectorRaw(blocks = blocks, keyA = correctKey.key, keyB = null)
    }

    private const val TAG = "ClassicReader"

    private fun earlyCheck(sectors: List<ClassicSector>, feedbackInterface: TagReaderFeedbackInterface): ClassicCardTransitFactory? {
        val secnum = sectors.size
        ClassicCardFactoryRegistry.allFactories.filter { factory -> factory.earlySectors == secnum }
                .forEach lambda@{ factory ->
                    val ci = try {
                        if (!factory.earlyCheck(sectors))
                            return@lambda
                        factory.earlyCardInfo(sectors) ?: return@lambda
                    } catch (e: Exception) {
                        return@lambda
                    }

                    feedbackInterface.showCardType(ci)
                    feedbackInterface.updateStatusText(Localizer.localizeString(R.string.card_reading_type, ci.name))
                    return factory
                }
        return null
    }

    fun readCard(retriever: CardKeysRetriever, tech: ClassicCardTech,
                 feedbackInterface: TagReaderFeedbackInterface): ClassicCard {
        val sectorCount = tech.sectorCount
        val sectors = mutableListOf<ClassicSector>()
        val maxProgress = sectorCount * 5
        var cardType: ClassicCardTransitFactory? = null
        val tagId = tech.tagId
        val auth = ClassicAuthenticator.makeAuthenticator(tagId = tagId, maxProgress = maxProgress,
                retriever = retriever)

        for (sectorIndex in 0 until sectorCount) {
            try {
                val correctKey = auth.authenticate(
                        tech, feedbackInterface,
                        sectorIndex, cardType?.isDynamicKeys(sectors, sectorIndex,
                        ClassicSectorKey.KeyType.UNKNOWN) ?: false,
                        ClassicSectorKey.KeyType.UNKNOWN)

                feedbackInterface.updateProgressBar(sectorIndex * 5 + 3, maxProgress)

                // Fallback if no key is found
                if (correctKey == null) {
                    sectors.add(UnauthorizedClassicSector())
                    continue
                }

                feedbackInterface.updateStatusText(Localizer.localizeString(R.string.mfc_reading_blocks, sectorIndex))
                var sector = ClassicSector.create(ClassicReader.readSectorWithKey(tech, sectorIndex, correctKey))

                // If we used keyA and it wasn't enough try finding B
                if (sector.key?.type == ClassicSectorKey.KeyType.A
                        && sector.blocks.any { it.isUnauthorized }) {
                    val correctKeyB = auth.authenticate(
                            tech, feedbackInterface,
                            sectorIndex, cardType?.isDynamicKeys(sectors, sectorIndex,
                            ClassicSectorKey.KeyType.B) ?: false,
                            ClassicSectorKey.KeyType.B)
                    if (correctKeyB != null)
                        sector = ClassicSector.create(ClassicReader.readSectorWithKey(tech, sectorIndex, correctKey))
                }

                sectors.add(sector)

                if (cardType == null)
                    cardType = earlyCheck(sectors, feedbackInterface)

                feedbackInterface.updateProgressBar(sectorIndex * 5 + 4, maxProgress)
            } catch (ex: CardLostException) {
                Log.w(TAG, "tag lost!", ex)
                sectors.add(InvalidClassicSector(ex.message))
                return ClassicCard(sectorsRaw = sectors.map { it.raw }, isPartialRead = true)
            } catch (ex: CardTransceiveException) {
                sectors.add(InvalidClassicSector(ex.errorMessage))
            }
        }

        return ClassicCard(sectorsRaw = sectors.map { it.raw }, isPartialRead = false)
    }
}