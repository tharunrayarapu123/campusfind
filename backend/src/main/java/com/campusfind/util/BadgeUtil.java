package com.campusfind.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BadgeUtil {

    private BadgeUtil() {}

    public static List<String> computeBadges(long returnsCount) {
        List<String> badges = new ArrayList<>();
        if (returnsCount >= 1) {
            badges.add("FIRST_RETURN");
        }
        if (returnsCount >= 3) {
            badges.add("HELPER");
        }
        if (returnsCount >= 10) {
            badges.add("TOP_FINDER");
        }
        return Collections.unmodifiableList(badges);
    }
}
