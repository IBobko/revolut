package revolut.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.joda.money.Money;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Based on https://martinfowler.com/eaaDev/AccountingEntry.html.
 * <p>
 * This class represents a single account operation.
 * Any instance of this class can't have null fields otherwise it throws NullPointerException.
 * It is an immutable class.
 * </p>
 */
@Getter
public class Entry {
    private final Money amount;
    private final ZonedDateTime date;

    public Entry(final Money amount, final ZonedDateTime date) {
        Objects.requireNonNull(amount, "Amount can't be null");
        Objects.requireNonNull(date, "Date can't be null");
        this.amount = amount;
        this.date = date;
    }
}
