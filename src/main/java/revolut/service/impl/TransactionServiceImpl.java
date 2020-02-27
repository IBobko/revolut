package revolut.service.impl;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import revolut.model.Account;
import revolut.model.AccountingTransaction;
import revolut.model.Holder;
import revolut.request.TransactionRequest;
import revolut.service.HolderService;
import revolut.service.TransactionService;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class TransactionServiceImpl implements TransactionService {
    private HolderService holderService;

    @Inject
    public TransactionServiceImpl(HolderService holderService) {
        this.holderService = holderService;
    }

    public AccountingTransaction.OverallStatus perform(TransactionRequest request) {
        Objects.requireNonNull(request, "Request can't be null.");
        Account payer = holderService.getAccountById(request.getPayerAccountId());
        Objects.requireNonNull(payer, "Payer not found.");
        Account payee = holderService.getAccountById(request.getPayeeAccountId());
        Objects.requireNonNull(payee, "Payee not found.");
        AccountingTransaction at = new AccountingTransaction(Money.of(CurrencyUnit.USD, request.getSum()), payer, payee, ZonedDateTime.now());
        return at.perform();
    }

    public Money getTotalSystemBalance(final CurrencyUnit currency) {
        Objects.requireNonNull(currency);
        Money totalBalance = Money.zero(currency);
        final Map<Long, Holder> holders = holderService.getHolders();
        for (final Map.Entry<Long, Holder> holder : holders.entrySet()) {
            Optional<Money> money = holder.getValue().getAccounts().values()
                    .stream().filter(s -> s.getCurrency().equals(currency))
                    .map(Account::getBalance).reduce(Money::plus);
            if (money.isPresent()) {
                totalBalance = totalBalance.plus(money.get());
            }
        }
        return totalBalance;
    }
}
