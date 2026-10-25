package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class WallpaperGalleryNavigationTest {
    @Test public void headerAndPartialLastRowAreReachable() {
        assertEquals(-1, GridNavigation.target(0, 34, 4, false, 19));
        assertEquals(-1, GridNavigation.target(3, 34, 4, false, 19));
        assertEquals(0, GridNavigation.target(4, 34, 4, false, 19));
        assertEquals(33, GridNavigation.target(31, 34, 4, false, 20));
        assertEquals(32, GridNavigation.target(32, 34, 4, false, 20));
        assertEquals(33, GridNavigation.target(33, 34, 4, false, 20));
        assertEquals(29, GridNavigation.target(33, 34, 4, false, 19));
    }

    @Test public void horizontalEdgesNeverWrapInEitherReadingDirection() {
        for (boolean rtl : new boolean[]{false, true}) {
            assertEquals(0, GridNavigation.target(0, 34, 4, rtl, rtl ? 22 : 21));
            assertEquals(3, GridNavigation.target(3, 34, 4, rtl, rtl ? 21 : 22));
            assertEquals(33, GridNavigation.target(33, 34, 4, rtl, rtl ? 21 : 22));
            assertEquals(1, GridNavigation.target(0, 34, 4, rtl, rtl ? 21 : 22));
        }
    }

    @Test public void allDirectionalTargetsStayInsideTheCollectionOrHeader() {
        for (int count = 1; count <= 100; count++) {
            for (int position = 0; position < count; position++) {
                for (int key = 19; key <= 22; key++) {
                    for (boolean rtl : new boolean[]{false, true}) {
                        int target = GridNavigation.target(position, count, 4, rtl, key);
                        assertTrue(target >= -1 && target < count);
                    }
                }
            }
        }
        assertEquals(-2, GridNavigation.target(0, 34, 4, false, 23)); // OK stays a click.
        assertEquals(-2, GridNavigation.target(0, 34, 4, false, 4)); // Back stays Back.
    }
}
