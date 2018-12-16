/*
 * NumericalStringComparatorTest.java
 *
 * Copyright 2016 Michael Farrell <micolous+git@gmail.com>
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
package au.id.micolous.metrodroid.test;

import au.id.micolous.metrodroid.util.NumericalStringComparator;

import junit.framework.TestCase;

/**
 * Unit tests for the NumericalStringComparator
 */
public class NumericalStringComparatorTest extends TestCase {
    NumericalStringComparator comparator = new NumericalStringComparator();

    public void testSimple() {
        assertEquals(-1, comparator.compare("1", "2"));
        assertEquals(0, comparator.compare("1", "1"));
        assertEquals(1, comparator.compare("2", "1"));
    }

    public void testLex() {
        assertEquals(-1, comparator.compare("9", "20"));
        assertEquals(0, comparator.compare("20", "20"));
        assertEquals(1, comparator.compare("20", "9"));
    }

    public void testPreLetters() {
        assertEquals(-1, comparator.compare("x9", "x20"));
        assertEquals(0, comparator.compare("x20", "x20"));
        assertEquals(1, comparator.compare("x20", "x9"));

        assertEquals(-1, comparator.compare("x9", "y20"));
        assertEquals(-1, comparator.compare("x20", "y20"));
        assertEquals(-1, comparator.compare("x20", "y9"));

        assertEquals(1, comparator.compare("y9", "x20"));
        assertEquals(1, comparator.compare("y20", "x20"));
        assertEquals(1, comparator.compare("y20", "x9"));
    }

    public void testPostLetters() {
        assertEquals(-1, comparator.compare("9x", "20x"));
        assertEquals(0, comparator.compare("20x", "20x"));
        assertEquals(1, comparator.compare("20x", "9x"));

        assertEquals(-1, comparator.compare("9x", "20y"));
        assertEquals(-1, comparator.compare("20x", "20y"));
        assertEquals(1, comparator.compare("20x", "9y"));

        assertEquals(-1, comparator.compare("9y", "20x"));
        assertEquals(1, comparator.compare("20y", "20x"));
        assertEquals(1, comparator.compare("20y", "9x"));
    }

    public void testPrePostLetters() {
        assertEquals(-1, comparator.compare("x9x", "x20x"));
        assertEquals(0, comparator.compare("x20x", "x20x"));
        assertEquals(1, comparator.compare("x20x", "x9x"));

        assertEquals(-1, comparator.compare("x9x", "y20y"));
        assertEquals(-1, comparator.compare("x20x", "y20y"));
        assertEquals(-1, comparator.compare("x20x", "y9y"));

        assertEquals(1, comparator.compare("y9y", "x20x"));
        assertEquals(1, comparator.compare("y20y", "x20x"));
        assertEquals(1, comparator.compare("y20y", "x9x"));
    }

    public void testSeqGoZones() {
        assertEquals(-1, comparator.compare("1", "3"));
        assertEquals(1, comparator.compare("airtrain", "3"));
        assertEquals(0, comparator.compare("airtrain", "airtrain"));
        assertEquals(-1, comparator.compare("3", "airtrain"));
    }
}

