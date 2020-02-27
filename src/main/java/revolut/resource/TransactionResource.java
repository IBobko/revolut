package revolut.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.joda.money.CurrencyMismatchException;
import org.joda.money.CurrencyUnit;
import org.joda.money.IllegalCurrencyException;
import org.joda.money.Money;
import revolut.exception.TheSamePayerAndPayeeException;
import revolut.model.Account;
import revolut.model.AccountingTransaction;
import revolut.model.Holder;
import revolut.request.TransactionRequest;
import revolut.service.HolderService;
import revolut.service.impl.TransactionServiceImpl;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Path("/transactions")
@Api
public class TransactionResource {
    @Inject
    private HolderService holderService;

    @Inject
    private TransactionServiceImpl transactionService;

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public AccountingTransaction.OverallStatus transaction(@ApiParam(required = true) TransactionRequest request) {
        try {
            Objects.requireNonNull(request, "Request can't be null");
            Objects.requireNonNull(request.getSum(), "Sum can't be null");
            Objects.requireNonNull(request.getPayeeAccountId(), "Payee account can't be null.");
            Objects.requireNonNull(request.getPayerAccountId(), "Payer account can't be null.");
            return transactionService.perform(request);
        } catch (CurrencyMismatchException | NullPointerException | TheSamePayerAndPayeeException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/total-system-balance/{currency}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response totalSystemBalance(@PathParam("currency") String currency) {
        try {
            final CurrencyUnit currencyUnit = CurrencyUnit.of(currency);
            final Money totalBalance = transactionService.getTotalSystemBalance(currencyUnit);
            HashMap<String, BigDecimal> result = new HashMap<>();
            result.put("totalBalance", totalBalance.getAmount());
            return Response.ok().entity(result).build();
        } catch (NullPointerException | IllegalCurrencyException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
