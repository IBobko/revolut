package revolut.model;

import org.joda.money.CurrencyMismatchException;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import revolut.service.TransactionService;
import revolut.service.impl.HolderServiceImpl;
import revolut.service.impl.TransactionServiceImpl;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountingTransactionTest {

    private TransactionService transactionService;
    private HolderServiceImpl holderService;

    @BeforeEach
    void setUp() {
        holderService = new HolderServiceImpl();
        transactionService = new TransactionServiceImpl(holderService);
    }

    private Account createAccount(Long id) {
        return createAccountWithCurrency(id, CurrencyUnit.USD);
    }

    private Account createAccountWithCurrency(Long id, CurrencyUnit currencyUnit) {
        Entry entry1 = new Entry(Money.of(currencyUnit, 100), ZonedDateTime.now());
        Entry entry2 = new Entry(Money.of(currencyUnit, 200), ZonedDateTime.now());
        return new Account(id, currencyUnit, null, List.of(entry1, entry2));
    }

    private ZonedDateTime getZonedDateTime() {
        return ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    }

    @Test
    void createValidAccountTransaction() {
        Account payer = createAccount(1L);
        Account payee = createAccount(2L);
        AccountingTransaction accountingTransaction = new AccountingTransaction(Money.of(CurrencyUnit.USD, 100), payer, payee, getZonedDateTime());
        assertEquals(getZonedDateTime(), accountingTransaction.getDate());
    }

    @Test
    void createInValidAccountTransaction() {
        {
            Throwable exception = assertThrows(NullPointerException.class, () ->
                    new AccountingTransaction(null, null, null, null));
            assertEquals("Amount of money can't be null.", exception.getMessage());
        }
        {
            Throwable exception = assertThrows(NullPointerException.class, () ->
                    new AccountingTransaction(Money.of(CurrencyUnit.USD, 100), null, null, null));
            assertEquals("Payer can't be null.", exception.getMessage());
        }

        {
            Account payer = createAccount(1L);
            Throwable exception = assertThrows(NullPointerException.class, () ->
                    new AccountingTransaction(Money.of(CurrencyUnit.USD, 100), payer, null, null));
            assertEquals("Payee can't be null.", exception.getMessage());
        }

        {
            Account payer = createAccount(1L);
            Account payee = createAccount(2L);
            Throwable exception = assertThrows(NullPointerException.class, () ->
                    new AccountingTransaction(Money.of(CurrencyUnit.USD, 100), payer, payee, null));
            assertEquals("Date can't be null.", exception.getMessage());
        }

        {
            Account payer = createAccountWithCurrency(1L, CurrencyUnit.GBP);
            Account payee = createAccount(2L);
            Throwable exception = assertThrows(CurrencyMismatchException.class, () ->
                    new AccountingTransaction(Money.of(CurrencyUnit.USD, 100), payer, payee, getZonedDateTime()));
            assertEquals("Currencies differ: GBP/USD", exception.getMessage());
        }

        {
            Account payer = createAccount(1L);
            Account payee = createAccountWithCurrency(2L, CurrencyUnit.AUD);
            Throwable exception = assertThrows(CurrencyMismatchException.class, () ->
                    new AccountingTransaction(Money.of(CurrencyUnit.USD, 100), payer, payee, getZonedDateTime()));
            assertEquals("Currencies differ: AUD/USD", exception.getMessage());
        }

        {
            Account payer = createAccount(1L);
            Account payee = createAccount(1L);
            Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                    new AccountingTransaction(Money.of(CurrencyUnit.USD, 100), payer, payee, getZonedDateTime()));
            assertEquals("The Payer and the payee can't be the same.", exception.getMessage());
        }
    }

    @Test
    void performValid() {
        Account payer = createAccount(1L);
        Account payee = createAccount(2L);
        AccountingTransaction accountingTransaction = new AccountingTransaction(Money.of(CurrencyUnit.USD, 100), payer, payee, getZonedDateTime());
        AccountingTransaction.OverallStatus overallStatus = accountingTransaction.perform();

        assertEquals(AccountingTransaction.TransactionStatus.OK, overallStatus.getStatus());
        assertEquals(Money.of(CurrencyUnit.USD, 300), overallStatus.getInitialPayeeBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 300), overallStatus.getInitialPayerBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 400), overallStatus.getPayeeBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 200), overallStatus.getPayerBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 100), overallStatus.getTransferSum());
        assertEquals(Account.FixerStatus.GOOD, overallStatus.getPayeeStatus());
        assertEquals(Account.FixerStatus.GOOD, overallStatus.getPayerStatus());
    }

    @Test
    void performInValid() {
        Account payer = createAccount(1L);
        Account payee = createAccount(2L);
        AccountingTransaction accountingTransaction = new AccountingTransaction(Money.of(CurrencyUnit.USD, 1000), payer, payee, getZonedDateTime());
        AccountingTransaction.OverallStatus overallStatus = accountingTransaction.perform();
        assertEquals(AccountingTransaction.TransactionStatus.BAD, overallStatus.getStatus());
        assertEquals(Money.of(CurrencyUnit.USD, 300), overallStatus.getInitialPayeeBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 300), overallStatus.getInitialPayerBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 300), overallStatus.getPayeeBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 300), overallStatus.getPayerBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 1000), overallStatus.getTransferSum());
        assertEquals(Account.FixerStatus.GOOD, overallStatus.getPayeeStatus());
        assertEquals(Account.FixerStatus.INSUFFICIENT_SUM, overallStatus.getPayerStatus());
    }

    @Test
    public void transactionCheckingSystemForRobustWhenALotOfRequestsChangeBalanceTest() throws IOException {
        long startingTime = System.nanoTime();
        final Money totalBalance = transactionService.getTotalSystemBalance(CurrencyUnit.USD);
        final ExecutorService service = Executors.newCachedThreadPool();
        for (int j = 0; j < 10000; j++) {
            for (long i = 1; i < 7; i++) {
                final Long payerAccountId = i;
                final Long payeeAccountId = (i == 6) ? 1 : i + 1;
                service.submit(() -> {
                    Account payer = holderService.getAccountById(payerAccountId);
                    Account payee = holderService.getAccountById(payeeAccountId);
                    AccountingTransaction accountingTransaction = new AccountingTransaction(Money.of(CurrencyUnit.USD, 100), payer, payee, getZonedDateTime());
                    return accountingTransaction.perform();
                });
            }
        }
        awaitTerminationAfterShutdown(service);
        long finishingTime = System.nanoTime() - startingTime;
        System.out.printf("transactionCheckingSystemForRobustWhenALotOfRequestsChangeBalanceTest takes : %d\n", finishingTime);
        final Money afterTotalBalance = transactionService.getTotalSystemBalance(CurrencyUnit.USD);
        assertEquals(totalBalance, afterTotalBalance);
    }

    public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
