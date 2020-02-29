package revolut.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.joda.money.CurrencyMismatchException;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import revolut.exception.TheSamePayerAndPayeeException;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@EqualsAndHashCode
public class AccountingTransaction {
    private static final Logger logger = LoggerFactory.getLogger(AccountingTransaction.class);
    private final Account payer;
    private final Account payee;
    private final Entry from;
    private final Entry to;
    private OverallStatus overallStatus = new OverallStatus();
    private ZonedDateTime date;

    public AccountingTransaction(final Money amount, final Account payer, final Account payee, final ZonedDateTime date) {
        Objects.requireNonNull(amount, "Amount of money can't be null.");
        Objects.requireNonNull(payer, "Payer can't be null.");
        Objects.requireNonNull(payee, "Payee can't be null.");
        Objects.requireNonNull(date, "Date can't be null.");

        if (!payer.getCurrency().equals(amount.getCurrencyUnit())) {
            throw new CurrencyMismatchException(payer.getCurrency(), amount.getCurrencyUnit());
        }

        if (!payee.getCurrency().equals(amount.getCurrencyUnit())) {
            throw new CurrencyMismatchException(payee.getCurrency(), amount.getCurrencyUnit());
        }

        if (payer.equals(payee)) {
            throw new TheSamePayerAndPayeeException("The Payer and the payee can't be the same.");
        }

        this.date = date;
        this.payee = payee;
        this.payer = payer;
        from = new Entry(amount.negated(), date);
        to = new Entry(amount, date);
    }

    public synchronized OverallStatus perform() {
        if (overallStatus.getStatus() != TransactionStatus.OK) {
            doRun();
        }
        return overallStatus;
    }

    private void doRun() {
        final Lock payerLock = payer.getLock();
        try {
            if (payerLock.tryLock(Account.WAITING_INTERVAL, TimeUnit.MILLISECONDS)) {
                try {
                    final Lock payeeLock = payee.getLock();
                    if (payeeLock.tryLock(Account.WAITING_INTERVAL, TimeUnit.MILLISECONDS)) {
                        try {
                            overallStatus.setPayerStatus(Account.FixerStatus.NOT_DEFINED);
                            overallStatus.setPayeeStatus(Account.FixerStatus.NOT_DEFINED);
                            overallStatus.setInitialPayeeBalance(payee.getBalance());
                            overallStatus.setInitialPayerBalance(payer.getBalance());
                            overallStatus.setTransferSum(to.getAmount());

                            Account.Fixer fromFixer = payer.checkEntry(from);
                            Account.Fixer toFixer = payee.checkEntry(to);

                            if (fromFixer.getStatus().equals(Account.FixerStatus.GOOD) &&
                                    toFixer.getStatus().equals(Account.FixerStatus.GOOD)) {
                                boolean fromResult = fromFixer.push();
                                boolean toResult = toFixer.push();
                                overallStatus.setPayerStatus(fromFixer.getStatus());
                                overallStatus.setPayeeStatus(toFixer.getStatus());
                                if (fromResult && toResult) {
                                    overallStatus.setPayeeBalance(payee.getBalance());
                                    overallStatus.setPayerBalance(payer.getBalance());
                                    overallStatus.setStatus(TransactionStatus.OK);
                                    return;
                                } else {
                                    fromFixer.cancel();
                                    toFixer.cancel(); // Optional call, exists just in case
                                }
                            }
                            overallStatus.setPayeeBalance(payee.getBalance());
                            overallStatus.setPayerBalance(payer.getBalance());
                            overallStatus.setPayerStatus(fromFixer.getStatus());
                            overallStatus.setPayeeStatus(toFixer.getStatus());
                            overallStatus.setStatus(TransactionStatus.BAD);
                        } finally {
                            payeeLock.unlock();
                        }
                    } else {
                        overallStatus.setStatus(TransactionStatus.PAYEE_BUSY);
                    }
                } finally {
                    payerLock.unlock();
                }
            } else {
                overallStatus.setStatus(TransactionStatus.PAYER_BUSY);
            }
        } catch (InterruptedException e) {
            overallStatus.setStatus(TransactionStatus.BAD);
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public enum TransactionStatus {
        PAYEE_BUSY, PAYER_BUSY, BAD, OK
    }

    @Data
    public static class OverallStatus {
        private Account.FixerStatus payerStatus;
        private Account.FixerStatus payeeStatus;
        private Money payerBalance;
        private Money payeeBalance;
        private Money initialPayerBalance;
        private Money initialPayeeBalance;
        private Money transferSum;
        private AccountingTransaction.TransactionStatus status;
    }
}