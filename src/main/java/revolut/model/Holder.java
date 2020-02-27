package revolut.model;

import com.google.common.collect.ImmutableMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import revolut.exception.MissedAccountException;

import java.util.Map;
import java.util.Objects;

/**
 * The class represents a single holder of accounts as client of bank.
 * It has id, full name, and money accounts. So it doesn't allow to change these parameters.
 * "id", "fullName", and "accounts" parameters are mandatory, and accounts must contain at least 1 account.
 * The same "id" from different holder instances means the same holder.
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Holder {
    @EqualsAndHashCode.Include
    final private Long id;
    final private String fullName;
    final private Map<Long, Account> accounts;

    public Holder(Long id, String fullName, Map<Long, Account> accounts) {
        Objects.requireNonNull(id, "Id can't be null");
        Objects.requireNonNull(fullName, "Full name can't be null");
        Objects.requireNonNull(accounts, "Accounts name can't be null");
        if (fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("fullName can't be empty.");
        }
        if (accounts.isEmpty()) {
            throw new MissedAccountException("Holder must contain at least 1 account.");
        }
        this.id = id;
        this.fullName = fullName;
        this.accounts = ImmutableMap.copyOf(accounts);
    }
}
