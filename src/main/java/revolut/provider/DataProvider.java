package revolut.provider;

import com.google.common.collect.ImmutableMap;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import revolut.model.Account;
import revolut.model.Entry;
import revolut.model.Holder;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DataProvider {

    static Long account_id = 0L;
    static Long holder_id = 0L;

    private static Holder getRandomHolder() {
        String fullName = generateRandomWord(10);

        Holder holder = new Holder(++holder_id, fullName, generateAccounts());
        return holder;
    }

    public static Map<Long, Holder> generateHolders(final Integer count) {
        account_id = 0L;
        holder_id = 0L;
        final Map<Long, Holder> result = new HashMap<>();
        for (int i = 0; i < count; i++) {
            Holder holder = getRandomHolder();
            result.put(holder.getId(), holder);
        }

        String fullName = "Bruce Willis";
        Account account = generateAccountWithCurrency(CurrencyUnit.GBP);
        HashMap<Long, Account> accounts = new HashMap<>();
        accounts.put(account.getId(), account);
        Holder holder = new Holder(++holder_id, fullName, accounts);
        result.put(holder.getId(),holder);
        return ImmutableMap.copyOf(result);
    }

    private static String generateRandomWord(int wordLength) {
        Random r = new Random(); // Intialize a Random Number Generator with SysTime as the seed
        StringBuilder sb = new StringBuilder(wordLength);
        for (int i = 0; i < wordLength; i++) { // For each letter in the word
            char tmp = (char) ('a' + r.nextInt('z' - 'a')); // Generate a letter between a and z
            sb.append(tmp); // Add it to the String
        }
        return sb.toString();
    }

    private static Map<Long, Account> generateAccounts() {
        Map<Long, Account> accounts = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            Account account = generateAccount();
            accounts.put(account.getId(), account);
        }
        return accounts;
    }

    private static Account generateAccountWithCurrency(CurrencyUnit currency) {
        Account account = new Account(++account_id, currency, null, generatesEntriesWithCurrency(currency));
        return account;
    }

    private static List<Entry> generatesEntriesWithCurrency(CurrencyUnit currency) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            entries.add(generateEntry(currency));
        }
        return entries;
    }

    private static Account generateAccount() {
        return generateAccountWithCurrency(CurrencyUnit.USD);
    }

    private static List<Entry> generatesEntries() {
        return generatesEntriesWithCurrency(CurrencyUnit.USD);
    }

    private static Entry generateEntry(CurrencyUnit currency) {
        return new Entry(Money.of(currency, 100), ZonedDateTime.now());
    }


}
