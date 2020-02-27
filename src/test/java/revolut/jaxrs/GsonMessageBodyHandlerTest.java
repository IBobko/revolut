package revolut.jaxrs;

import org.jboss.resteasy.util.CommitHeaderOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import revolut.request.TransactionRequest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GsonMessageBodyHandlerTest {
    private GsonMessageBodyHandler gsonMessageBodyHandler;

    @BeforeEach
    void setUp() {
        gsonMessageBodyHandler = new GsonMessageBodyHandler();
    }

    @Test
    void writeTo() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        CommitHeaderOutputStream os = new CommitHeaderOutputStream(byteArrayOutputStream, () -> {
        });
        ErrorMessage entity = new ErrorMessage();
        entity.setError("some error");
        gsonMessageBodyHandler.writeTo(entity, entity.getClass(), entity.getClass(), null, MediaType.APPLICATION_OCTET_STREAM_TYPE, null, os);
        assertEquals("{\"error\":\"some error\"}", os.getDelegate().toString());
    }

    @Test
    void readFrom() throws IOException {
        String inputString = "{\"sum\": 1,\"payerAccountId\": 1,\"payeeAccountId\": 2}";
        InputStream in = org.apache.commons.io.IOUtils.toInputStream(inputString, "UTF-8");
        TransactionRequest transactionRequest = (TransactionRequest) gsonMessageBodyHandler.readFrom(Object.class, TransactionRequest.class, new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(), in);
        assertEquals(new BigDecimal(1), transactionRequest.getSum());
        assertEquals(2, transactionRequest.getPayeeAccountId());
        assertEquals(1, transactionRequest.getPayerAccountId());
    }
}