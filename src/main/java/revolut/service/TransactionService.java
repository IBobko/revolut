package revolut.service;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import revolut.model.AccountingTransaction;
import revolut.request.TransactionRequest;

public interface TransactionService {
    AccountingTransaction.OverallStatus perform(TransactionRequest request);
    Money getTotalSystemBalance(final CurrencyUnit currency);
}
