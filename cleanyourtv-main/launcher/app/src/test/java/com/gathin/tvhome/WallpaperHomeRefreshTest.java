package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WallpaperHomeRefreshTest {
    @Test
    public void disabledKeepsTheExistingWallpaperBehaviour() {
        WallpaperHomeRefresh refresh = new WallpaperHomeRefresh(false);
        refresh.onResume();
        refresh.onHomeIntent();
        refresh.onResume();
        assertFalse(refresh.hasPendingChange());
        assertFalse(refresh.consumeChange());
    }

    @Test
    public void firstResumeDoesNotDecodeAnotherPictureAfterStartup() {
        WallpaperHomeRefresh refresh = new WallpaperHomeRefresh(true);
        refresh.onResume();
        assertFalse(refresh.consumeChange());
    }

    @Test
    public void homeWhileAlreadyVisibleRequestsANewPicture() {
        WallpaperHomeRefresh refresh = new WallpaperHomeRefresh(true);
        refresh.onResume();
        refresh.onHomeIntent();
        assertTrue(refresh.consumeChange());
        assertFalse(refresh.consumeChange());
    }

    @Test
    public void homeIntentAndFollowingResumeBecomeOneChange() {
        WallpaperHomeRefresh refresh = new WallpaperHomeRefresh(true);
        refresh.onResume();
        refresh.onHomeIntent();
        refresh.onResume();
        assertTrue(refresh.consumeChange());
        assertFalse(refresh.consumeChange());
    }

    @Test
    public void returningOrWakingRequestsANewPicture() {
        WallpaperHomeRefresh refresh = new WallpaperHomeRefresh(true);
        refresh.onResume();
        refresh.onResume();
        assertTrue(refresh.consumeChange());
    }

    @Test
    public void repeatedKeysDoNotCreateAQueueOfDecodes() {
        WallpaperHomeRefresh refresh = new WallpaperHomeRefresh(true);
        refresh.onResume();
        for (int i = 0; i < 100; i++) refresh.onHomeIntent();
        assertTrue(refresh.consumeChange());
        assertFalse(refresh.hasPendingChange());
    }

    @Test
    public void eventDuringADecodeRemainsPendingForItsCompletion() {
        WallpaperHomeRefresh refresh = new WallpaperHomeRefresh(true);
        refresh.onResume();
        refresh.onHomeIntent();
        assertTrue(refresh.consumeChange());
        refresh.onHomeIntent();
        assertTrue(refresh.hasPendingChange());
        assertTrue(refresh.consumeChange());
        assertFalse(refresh.hasPendingChange());
    }

    @Test
    public void aPendingIntentSurvivesTheFirstResume() {
        WallpaperHomeRefresh refresh = new WallpaperHomeRefresh(true);
        refresh.onHomeIntent();
        refresh.onResume();
        assertTrue(refresh.consumeChange());
    }
}
