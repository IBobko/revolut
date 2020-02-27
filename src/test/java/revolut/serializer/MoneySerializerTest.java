package revolut.serializer;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneySerializerTest {
    @Test
    void serialize() {
        MoneySerializer moneySerialize = new MoneySerializer();
        assertEquals("$ 50.00", moneySerialize.serialize(Money.of(CurrencyUnit.USD, 50), null, null).getAsString());
    }
}