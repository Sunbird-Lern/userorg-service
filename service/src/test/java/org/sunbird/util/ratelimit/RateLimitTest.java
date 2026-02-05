package org.sunbird.util.ratelimit;

import static org.junit.Assert.assertEquals;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sunbird.keys.JsonKey;

public class RateLimitTest {

    @Test
    public void testConstructorWithIntegerValues() {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.RATE_LIMIT_UNIT, "HOUR");
        map.put(JsonKey.RATE, 10);
        map.put(JsonKey.COUNT, 5);
        map.put(JsonKey.TTL, 3600);

        RateLimit rateLimit = new RateLimit("testKey", map);
        assertEquals(Integer.valueOf(10), rateLimit.getLimit());
        assertEquals(Integer.valueOf(5), rateLimit.getCount());
        assertEquals(Integer.valueOf(3600), rateLimit.getTTL());
    }

    @Test
    public void testConstructorWithLongValues() {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.RATE_LIMIT_UNIT, "DAY");
        map.put(JsonKey.RATE, 100L);
        map.put(JsonKey.COUNT, 50L);
        map.put(JsonKey.TTL, 86400L);

        // This would have thrown ClassCastException before the fix
        RateLimit rateLimit = new RateLimit("testKey", map);
        assertEquals(Integer.valueOf(100), rateLimit.getLimit());
        assertEquals(Integer.valueOf(50), rateLimit.getCount());
        assertEquals(Integer.valueOf(86400), rateLimit.getTTL());
    }

    @Test
    public void testConstructorWithNullValues() {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.RATE_LIMIT_UNIT, "MINUTE");
        // missing other keys

        RateLimit rateLimit = new RateLimit("testKey", map);
        assertEquals(Integer.valueOf(0), rateLimit.getLimit());
        assertEquals(Integer.valueOf(0), rateLimit.getCount());
        assertEquals(Integer.valueOf(0), rateLimit.getTTL());
    }
}
