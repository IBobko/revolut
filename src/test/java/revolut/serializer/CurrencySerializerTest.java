package revolut.serializer;

import org.joda.money.CurrencyUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencySerializerTest {
    @Test
    void serialize() {
        CurrencySerializer currencySerialize = new CurrencySerializer();
        assertEquals("USD", currencySerialize.serialize(CurrencyUnit.USD, null, null).getAsString());
    }
}