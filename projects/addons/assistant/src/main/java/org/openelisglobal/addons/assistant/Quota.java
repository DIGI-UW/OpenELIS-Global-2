package org.openelisglobal.addons.assistant;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Questions per user per day. The "day" is the laboratory's day (config
 * {@code timeZone}), not the container's. Only SUCCESSFUL answers count: a
 * provider error does not use up a question.
 */
public final class Quota {

    private Quota() {
    }

    /** {@code limit <= 0} = no limit ({@code remaining = -1}). */
    public record Status(int used, int limit, int remaining, boolean allowed) {
    }

    /** 00:00 today in {@code zone}. */
    public static Instant dayStart(Instant now, ZoneId zone) {
        return now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant();
    }

    public static Status status(int used, int limit) {
        int u = Math.max(0, used);
        if (limit <= 0) {
            return new Status(u, 0, -1, true);
        }
        int remaining = Math.max(0, limit - u);
        return new Status(u, limit, remaining, remaining > 0);
    }
}
