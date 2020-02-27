package revolut.serializer;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZonedDateTimeSerializerTest {
    @Test
    void serialize() {
        ZonedDateTimeSerializer zonedDateTimeSerialize = new ZonedDateTimeSerializer();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals("01/01/2020 - 00:00:00 +0000",
                zonedDateTimeSerialize.serialize(zonedDateTime, null, null).getAsString());
    }
}