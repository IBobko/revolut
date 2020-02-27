package revolut.model;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntryTest {
    @Test
    void createValidEntryTest() {
        ZonedDateTime date = ZonedDateTime.now();
        Entry entry = new Entry(Money.of(CurrencyUnit.USD, 1), date);
        assertEquals(Money.of(CurrencyUnit.USD, 1), entry.getAmount());
        assertEquals(date, entry.getDate());
    }

    @Test
    void createEntryWithoutAmountTest() {
        assertThrows(NullPointerException.class, () -> new Entry(null, ZonedDateTime.now()));
    }

    @Test
    void createEntryWithoutDateTest() {
        assertThrows(NullPointerException.class, () -> new Entry(Money.of(CurrencyUnit.USD, 1), null));
    }
}