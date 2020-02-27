package revolut.model;


import org.joda.money.CurrencyMismatchException;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {
    @Test
    void createValidAccountTest() {
        final Account account1 = new Account(1L, CurrencyUnit.USD, null, null);
        assertEquals(Money.zero(CurrencyUnit.USD), account1.getBalance());
        assertNotEquals(Money.zero(CurrencyUnit.GBP), account1.getBalance());
        assertEquals(1L, account1.getId());
        assertEquals(CurrencyUnit.USD, account1.getCurrency());
        assertEquals(Money.zero(CurrencyUnit.USD), account1.getInitBalance());
        assertEquals(0, account1.getEntries().size());

        final Account account2 = new Account(2L, CurrencyUnit.USD, Money.of(CurrencyUnit.USD, 100), null);
        assertEquals(Money.of(CurrencyUnit.USD, 100), account2.getBalance());
        assertNotEquals(Money.of(CurrencyUnit.GBP, 100), account2.getBalance());
        assertEquals(2L, account2.getId());
        assertEquals(CurrencyUnit.USD, account2.getCurrency());
        assertEquals(Money.of(CurrencyUnit.USD, 100), account2.getInitBalance());
        assertEquals(0, account2.getEntries().size());

        Entry entry1 = new Entry(Money.of(CurrencyUnit.USD, 100), ZonedDateTime.now());
        Entry entry2 = new Entry(Money.of(CurrencyUnit.USD, 200), ZonedDateTime.now());

        final Account account3 = new Account(3L, CurrencyUnit.USD, Money.of(CurrencyUnit.USD, 100), List.of(entry1, entry2));
        assertEquals(Money.of(CurrencyUnit.USD, 400), account3.getBalance());
        assertNotEquals(Money.of(CurrencyUnit.GBP, 400), account3.getBalance());
        assertEquals(3L, account3.getId());
        assertEquals(CurrencyUnit.USD, account3.getCurrency());
        assertEquals(Money.of(CurrencyUnit.USD, 100), account3.getInitBalance());
        assertEquals(2, account3.getEntries().size());
    }

    @Test
    void createInvalidAccountTest() {
        // All values are null
        assertThrows(NullPointerException.class, () -> new Account(null, null, null, null));
        // Currency is  null
        assertThrows(NullPointerException.class, () -> new Account(1L, null, null, null));
        // Id is  null
        assertThrows(NullPointerException.class, () -> new Account(null, CurrencyUnit.USD, null, null));
        // initBalance doesn't have the same currency.
        assertThrows(CurrencyMismatchException.class, () -> new Account(1L, CurrencyUnit.USD, Money.zero(CurrencyUnit.GBP), null));

        // Entry has a wrong currency.
        Entry entry1 = new Entry(Money.of(CurrencyUnit.GBP, 100), ZonedDateTime.now());
        Entry entry2 = new Entry(Money.of(CurrencyUnit.USD, 200), ZonedDateTime.now());
        assertThrows(CurrencyMismatchException.class, () -> new Account(1L, CurrencyUnit.USD, null, List.of(entry1, entry2)));
    }

    @Test
    void checkEntryValidScenariosTest() {
        Entry entry1 = new Entry(Money.of(CurrencyUnit.USD, 100), ZonedDateTime.now());
        Entry entry2 = new Entry(Money.of(CurrencyUnit.USD, 200), ZonedDateTime.now());
        final Account account = new Account(3L, CurrencyUnit.USD, Money.of(CurrencyUnit.USD, 100), List.of(entry1, entry2));
        Entry insertingEntry = new Entry(Money.of(CurrencyUnit.USD, 100), ZonedDateTime.now());
        Account.Fixer fixer = account.checkEntry(insertingEntry);
        assertEquals(Account.FixerStatus.GOOD, fixer.getStatus());
        assertTrue(fixer.push());
        assertEquals(Money.of(CurrencyUnit.USD, 500), account.getBalance());
        fixer.cancel();
        assertEquals(Money.of(CurrencyUnit.USD, 400), account.getBalance());
    }

    @Test
    void checkEntryIValidScenariosTest() {
        Entry entry1 = new Entry(Money.of(CurrencyUnit.USD, 100), ZonedDateTime.now());
        Entry entry2 = new Entry(Money.of(CurrencyUnit.USD, 200), ZonedDateTime.now());
        final Account account = new Account(3L, CurrencyUnit.USD, Money.of(CurrencyUnit.USD, 100), List.of(entry1, entry2));
        Entry insertingEntryWithWrongCurrency = new Entry(Money.of(CurrencyUnit.GBP, 100), ZonedDateTime.now());
        Account.Fixer fixer1 = account.checkEntry(insertingEntryWithWrongCurrency);
        assertEquals(Account.FixerStatus.INCORRECT_CURRENCY, fixer1.getStatus());

        //Push doesn't do anything in this case.
        assertFalse(fixer1.push());
        assertEquals(Money.of(CurrencyUnit.USD, 400), account.getBalance());

        //Push doesn't do anything in this case.
        fixer1.cancel();
        assertEquals(Money.of(CurrencyUnit.USD, 400), account.getBalance());

        Entry insertingEntryWithBigNegatedSum = new Entry(Money.of(CurrencyUnit.USD, 1000).negated(), ZonedDateTime.now());
        Account.Fixer fixer2 = account.checkEntry(insertingEntryWithBigNegatedSum);
        assertEquals(Account.FixerStatus.INSUFFICIENT_SUM, fixer2.getStatus());

        //Push doesn't do anything in this case.
        assertFalse(fixer2.push());
        assertEquals(Money.of(CurrencyUnit.USD, 400), account.getBalance());

        //Push doesn't do anything in this case.
        fixer2.cancel();
        assertEquals(Money.of(CurrencyUnit.USD, 400), account.getBalance());
    }

    @Test
    void checkThatEntriesUnmodifiableTest() {
        Entry entry1 = new Entry(Money.of(CurrencyUnit.USD, 100), ZonedDateTime.now());
        Entry entry2 = new Entry(Money.of(CurrencyUnit.USD, 200), ZonedDateTime.now());
        final Account account = new Account(3L, CurrencyUnit.USD, Money.of(CurrencyUnit.USD, 100), List.of(entry1, entry2));
        Collection<Entry> entries = account.getEntries();
        Entry insertingEntry = new Entry(Money.of(CurrencyUnit.USD, 100), ZonedDateTime.now());
        assertThrows(UnsupportedOperationException.class, () -> entries.add(insertingEntry));
        assertThrows(UnsupportedOperationException.class, () -> entries.remove(entry2));
        assertEquals(2, account.getEntries().size());
        assertEquals(Money.of(CurrencyUnit.USD, 400), account.getBalance());
    }

    @Test
    void equalsIfIdIsTheSame() {
        Entry entry1 = new Entry(Money.of(CurrencyUnit.USD, 100), ZonedDateTime.now());
        Entry entry2 = new Entry(Money.of(CurrencyUnit.USD, 200), ZonedDateTime.now());
        final Account account1 = new Account(3L, CurrencyUnit.USD, Money.of(CurrencyUnit.USD, 100), List.of(entry1, entry2));

        Entry entry3 = new Entry(Money.of(CurrencyUnit.USD, 300), ZonedDateTime.now());
        Entry entry4 = new Entry(Money.of(CurrencyUnit.USD, 400), ZonedDateTime.now());
        final Account account2 = new Account(3L, CurrencyUnit.USD, Money.of(CurrencyUnit.USD, 100), List.of(entry3, entry4));
        assertEquals(account1, account2);
    }
}