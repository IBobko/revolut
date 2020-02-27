package revolut.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode
@ToString
public class TransactionRequest {
    private BigDecimal sum;
    private Long payerAccountId;
    private Long payeeAccountId;
}
