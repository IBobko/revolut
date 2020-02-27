package revolut.service.impl;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import revolut.model.AccountingTransaction;
import revolut.request.TransactionRequest;
import revolut.service.TransactionService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static revolut.model.Account.FixerStatus;

class TransactionServiceImplTest {
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        HolderServiceImpl holderService = new HolderServiceImpl();
        transactionService = new TransactionServiceImpl(holderService);
    }

    @Test
    void performValid() {
        final TransactionRequest request = new TransactionRequest();
        request.setSum(new BigDecimal(100));
        request.setPayeeAccountId(1L);
        request.setPayerAccountId(2L);
        AccountingTransaction.OverallStatus status = transactionService.perform(request);
        assertEquals(FixerStatus.GOOD, status.getPayerStatus());
        assertEquals(FixerStatus.GOOD, status.getPayeeStatus());
        assertEquals(Money.of(CurrencyUnit.USD, 400), status.getPayerBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 600), status.getPayeeBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 500), status.getInitialPayerBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 500), status.getInitialPayeeBalance());
        assertEquals(Money.of(CurrencyUnit.USD, 100), status.getTransferSum());
    }

    @Test
    void performInValid() {
        {
            final TransactionRequest request = new TransactionRequest();
            request.setSum(new BigDecimal(100));
            request.setPayeeAccountId(-1L);
            request.setPayerAccountId(2L);
            NullPointerException exception = assertThrows(NullPointerException.class, () -> transactionService.perform(request));
            assertEquals("Payee not found.", exception.getMessage());
        }

        {
            final TransactionRequest request = new TransactionRequest();
            request.setSum(new BigDecimal(100));
            request.setPayeeAccountId(1L);
            request.setPayerAccountId(-2L);
            NullPointerException exception = assertThrows(NullPointerException.class, () -> transactionService.perform(request));
            assertEquals("Payer not found.", exception.getMessage());
        }
        {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> transactionService.perform(null));
            assertEquals("Request can't be null.", exception.getMessage());
        }
    }

}