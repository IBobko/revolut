package revolut.model;

import org.joda.money.CurrencyUnit;
import org.junit.jupiter.api.Test;
import revolut.exception.MissedAccountException;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class HolderTest {
    public static final String VALID_FULL_NAME = "Will Smith";
    @Test
    void createValidHolderTest() {
        final Account account = new Account(1L, CurrencyUnit.USD, null, null);
        HashMap<Long, Account> accounts = new HashMap<>();
        accounts.put(1L, account);
        Holder holder = new Holder(1L, VALID_FULL_NAME, accounts);
        assertEquals(1L, holder.getId());
        assertEquals(VALID_FULL_NAME, holder.getFullName());
        assertFalse(holder.getAccounts().isEmpty());
    }

    @Test
    void createInValidHolderTest() {
        assertThrows(NullPointerException.class, () -> new Holder(null, null, null));
        assertThrows(NullPointerException.class, () -> new Holder(1L, null, null));
        assertThrows(NullPointerException.class, () -> new Holder(1L, VALID_FULL_NAME, null));

        final Account account = new Account(1L, CurrencyUnit.USD, null, null);
        HashMap<Long, Account> accounts = new HashMap<>();
        assertThrows(MissedAccountException.class, () -> new Holder(1L, VALID_FULL_NAME, accounts));
        accounts.put(1L, account);
        assertThrows(IllegalArgumentException.class, () -> new Holder(1L, "", accounts));
    }

    @Test
    void equalsIfIdIsTheSame() {
        final Account account = new Account(1L, CurrencyUnit.USD, null, null);
        HashMap<Long, Account> accounts = new HashMap<>();
        accounts.put(1L, account);
        Holder holder1 = new Holder(1L, VALID_FULL_NAME, accounts);

        final Account account2 = new Account(1L, CurrencyUnit.USD, null, null);
        HashMap<Long, Account> accounts2 = new HashMap<>();
        accounts2.put(1L, account2);
        Holder holder2 = new Holder(1L, VALID_FULL_NAME, accounts2);
        assertEquals(holder1, holder2);
    }
}
