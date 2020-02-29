package revolut.model;

import lombok.EqualsAndHashCode;
import org.joda.money.CurrencyMismatchException;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The class represents an account with concrete currency and operation made for this account.
 * It allowed any negative values.
 * The same "id" from different account instances means the same account.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Account {
    public static final long WAITING_INTERVAL = 100L;
    public static final Logger logger = LoggerFactory.getLogger(Account.class);
    @EqualsAndHashCode.Include
    private final Long id;
    private final transient Lock lock = new ReentrantLock();
    private final List<Entry> entries = new CopyOnWriteArrayList<>();
    private final Money initBalance;
    private final CurrencyUnit currency;

    public Account(final Long id, final CurrencyUnit currency, final Money initBalance, final Collection<Entry> entries) {
        Objects.requireNonNull(id, "Id can't be null");
        Objects.requireNonNull(currency, "Currency can't be null");
        this.initBalance = Optional.ofNullable(initBalance).orElse(Money.zero(currency));

        if (!currency.equals(this.initBalance.getCurrencyUnit())) {
            throw new CurrencyMismatchException(this.initBalance.getCurrencyUnit(), currency);
        }
        if (entries != null && entries.size() > 0) {
            for (final Entry entry : entries) {
                if (!entry.getAmount().getCurrencyUnit().equals(currency)) {
                    throw new CurrencyMismatchException(entry.getAmount().getCurrencyUnit(), currency);
                }
                this.entries.add(entry);
            }
        }
        this.id = id;
        this.currency = currency;
        logger.info("Account {} initialized with balance  {}.", getId(), getBalance());
    }

    /**
     * Returns copy of entries to prevent collection modification.
     *
     * @return copy of account operations.
     */
    public Collection<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Returns initial balance of account.
     * In the future there would be method to collapse entries into initial balance for performance.
     */
    public Money getInitBalance() {
        return initBalance;
    }

    /**
     * Currency of current account.
     *
     * @return Account's currency.
     */
    public CurrencyUnit getCurrency() {
        return currency;
    }

    public Money getBalance() {
        try {
            lock.lock();
            if (entries.isEmpty()) {
                return initBalance;
            }
            return entries.stream().map(Entry::getAmount).reduce(Money::plus).get().plus(initBalance);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds new record into account. If entry is null
     * We are sure that parameters passes all prechecks.
     *
     * @param entry . Operation for performing.
     * @return Success or fail.
     */
    private boolean addEntry(final Entry entry) {
        try {
            if (lock.tryLock(WAITING_INTERVAL, TimeUnit.MILLISECONDS)) {
                try {
                    // method above checking for currency and returns CurrencyMismatchException if mismatch is.
                    if (getBalance().plus(entry.getAmount()).compareTo(Money.zero(currency)) != -1) {
                        entries.add(entry);
                        logger.info("Account {} changed balance for {}.", getId(), entry.getAmount());
                        return true;
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    /**
     * This method must contain all checkers for entry.
     * Calling this method implies that the object was already blocked.
     **/
    public Fixer checkEntry(Entry entry) {
        Fixer fixer = null;
        try {
            Objects.requireNonNull(entry, "Entry can't be null");
            if (entry.getAmount().getCurrencyUnit().compareTo(this.currency) != 0) {
                fixer = new Fixer(this, entry, FixerStatus.INCORRECT_CURRENCY);
                throw new CurrencyMismatchException(entry.getAmount().getCurrencyUnit(), currency);
            }
            // method above checks for currency and returns CurrencyMismatchException if mismatch is.
            if (getBalance().plus(entry.getAmount()).compareTo(Money.zero(currency)) != -1) {
                return new Fixer(this, entry, FixerStatus.GOOD);
            } else {
                return new Fixer(this, entry, FixerStatus.INSUFFICIENT_SUM);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        if (fixer == null) {
            fixer = new Fixer(this, entry, FixerStatus.BAD);
        }
        return fixer;
    }

    private void removeEntry(Entry entry) {
        try {
            if (lock.tryLock(WAITING_INTERVAL, TimeUnit.MILLISECONDS)) {
                try {
                    if (entries.remove(entry)) {
                        logger.info("Account {} lost operation for balance {}.", getId(), entry.getAmount());
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    public Lock getLock() {
        return lock;
    }

    public Long getId() {
        return id;
    }

    public enum FixerStatus {
        GOOD, BAD, INSUFFICIENT_SUM, INCORRECT_CURRENCY, NOT_DEFINED
    }

    /**
     * An instance of this class holds the corresponding entry for inserting.
     * If status is "GOOD", it can push entry to the account otherwise it does nothing.
     * This class must exist always in locked scoped.
     * Only Account class can create instance.
     */
    static public class Fixer {
        private static final Logger logger = LoggerFactory.getLogger(Fixer.class);
        private final Account account;
        private final Entry entry;
        private FixerStatus status;

        private Fixer(Account account, Entry entry, FixerStatus status) {
            Objects.requireNonNull(account, "Account can't be null");
            Objects.requireNonNull(entry, "Entry can't be null");
            Objects.requireNonNull(status, "Status can't be null");
            this.account = account;
            this.entry = entry;
            this.status = status;
        }

        /**
         * If status "GOOD" Adds new operation/entry into the account, which means that operation is done.
         *
         * @return success or failure.
         */
        public boolean push() {
            if (status.equals(FixerStatus.GOOD)) {
                boolean result = account.addEntry(entry);
                if (!result) {
                    this.status = FixerStatus.BAD;
                    logger.warn("Could not added operation to account {}.", account.getId());
                }
                return result;
            }
            logger.warn("Trying to push operation which has failed status for account {}.", account.getId());
            return false;
        }

        /**
         * @return Current status.
         */
        public FixerStatus getStatus() {
            return status;
        }

        /**
         * Seldom case but there is just in case.
         * Cancel operation if it was done.
         **/
        public void cancel() {
            this.account.removeEntry(this.entry);
        }
    }
}
