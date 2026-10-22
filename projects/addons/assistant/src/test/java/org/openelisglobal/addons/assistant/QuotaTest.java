package org.openelisglobal.addons.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class QuotaTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void limits() {
        assertTrue(Quota.status(0, 20).allowed());
        assertEquals(20, Quota.status(0, 20).remaining());
        assertTrue(Quota.status(19, 20).allowed());
        assertFalse(Quota.status(20, 20).allowed());
        assertEquals(0, Quota.status(25, 20).remaining());
        assertTrue(Quota.status(500, 0).allowed()); // 0 = không giới hạn
        assertEquals(-1, Quota.status(500, 0).remaining());
    }

    @Test
    void dayStartsAtMidnightVietnamNotUtc() {
        // 23:30 UTC ngày 14 = 06:30 sáng ngày 15 giờ VN → "hôm nay" bắt đầu 15/10 00:00 VN = 14/10 17:00 UTC
        assertEquals(Instant.parse("2026-10-14T17:00:00Z"), Quota.dayStart(Instant.parse("2026-10-14T23:30:00Z"), VN));
        // 16:59 UTC ngày 14 = 23:59 ngày 14 VN
        assertEquals(Instant.parse("2026-10-13T17:00:00Z"), Quota.dayStart(Instant.parse("2026-10-14T16:59:00Z"), VN));
    }

    @Test
    void serviceBlocksAfterLimitAndOnlyCountsSuccess() throws Exception {
        Fixture f = new Fixture();
        f.config.put("dailyQuestionLimit", 2);
        assertEquals("done", f.ask(1, "patient:A", "q1", null, false).get("event"));
        f.provider.fail = true;
        assertEquals("error", f.ask(1, "patient:A", "q2", null, false).get("event")); // lỗi không ăn lượt
        f.provider.fail = false;
        assertEquals("done", f.ask(1, "patient:A", "q3", null, false).get("event"));
        java.util.Map<String, Object> blocked = f.ask(1, "patient:A", "q4", null, false);
        assertEquals("quota", blocked.get("code"));
        assertEquals("done", f.ask(2, "patient:A", "q5", null, false).get("event")); // người khác vẫn hỏi được
    }
}
