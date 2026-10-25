package com.gathin.tvhome;

/** Physical left/right boundaries for a grid whose first column follows its reading direction. */
final class GridNavigation {
    private GridNavigation() { }

    static boolean atHorizontalEdge(int position, int count, int columns, boolean rtl, boolean left) {
        boolean towardStart = left != rtl;
        if (towardStart) return position % columns == 0;
        return position % columns == columns - 1 || position == count - 1;
    }

    /** Gallery target: -1 is its header, -2 leaves a non-directional key to Android. */
    static int target(int position, int count, int columns, boolean rtl, int key) {
        if (key == 19) return position < columns ? -1 : position - columns; // DPAD_UP
        if (key == 20) return position / columns == (count - 1) / columns ? position
                : Math.min(position + columns, count - 1); // DPAD_DOWN, including a short last row
        if (key != 21 && key != 22) return -2;
        boolean left = key == 21;
        if (atHorizontalEdge(position, count, columns, rtl, left)) return position;
        return position + (left != rtl ? -1 : 1);
    }
}
