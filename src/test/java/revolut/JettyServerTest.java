package revolut;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JettyServerTest {
    public static final String API_URL = "http://localhost:8080/api/v1";

    @BeforeAll
    public static void setUp() throws Exception {
        Application.serverInitialization();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            Application.server.join();
            return null;
        });
    }

    @AfterAll
    public static void tearDown() throws Exception {
        Application.server.stop();
    }

    @Test
    public void getAllHoldersPageTest() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet httpget = new HttpGet(String.format("%s/holders", API_URL));
            try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode holders = mapper.readTree(json);
                for (final Iterator<JsonNode> it = holders.elements(); it.hasNext(); ) {
                    final JsonNode holder = it.next();
                    checkHolder(holder);
                }
            }
        }
    }

    public void checkHolder(JsonNode holder) {
        assertTrue(holder.has("fullName"));
        assertTrue(holder.has("accounts"));
        assertTrue(holder.has("id"));
        final JsonNode accounts = holder.get("accounts");
        for (final Iterator<JsonNode> accountIt = accounts.elements(); accountIt.hasNext(); ) {
            final JsonNode account = accountIt.next();
            assertTrue(account.has("id"));
            assertTrue(account.has("entries"));
            final JsonNode entries = account.get("entries");
            for (final Iterator<JsonNode> entryIt = entries.elements(); entryIt.hasNext(); ) {
                final JsonNode entry = entryIt.next();
                assertTrue(entry.has("amount"));
                assertTrue(entry.has("date"));
            }
        }
    }

    @Test
    public void getFirstHolderPageTest() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet httpget = new HttpGet(String.format("%s/holders/id/1", API_URL));
            try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode holder = mapper.readTree(json);
                checkHolder(holder);
            }
        }
    }

    @Test
    public void transactionTest() throws IOException {
        // Здесь нужно проверить что значения правильные.
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            HttpEntity requestEntity = new StringEntity("{\"sum\": 1,\"payerAccountId\": 1,\"payeeAccountId\": 2}");
            httpPut.setEntity(requestEntity);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);

                assertTrue(answer.has("payerStatus"));
                assertTrue(answer.has("payeeStatus"));
                assertTrue(answer.has("status"));
                assertTrue(answer.has("payerBalance"));
                assertTrue(answer.has("payeeBalance"));
                assertTrue(answer.has("initialPayerBalance"));
                assertTrue(answer.has("initialPayeeBalance"));
                assertTrue(answer.has("transferSum"));
            }
        }
    }

    @Test
    public void transactionInsufficientBalanceTest() throws IOException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            HttpEntity requestEntity = new StringEntity("{\"sum\": 1000,\"payerAccountId\": 1,\"payeeAccountId\": 2}");
            httpPut.setEntity(requestEntity);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);
                assertEquals("INSUFFICIENT_SUM", answer.get("payerStatus").asText());
                assertEquals("GOOD", answer.get("payeeStatus").asText());
                assertEquals("BAD", answer.get("status").asText());
                assertEquals("$ 500.00", answer.get("payerBalance").asText());
                assertEquals("$ 500.00", answer.get("payeeBalance").asText());
                assertEquals("$ 500.00", answer.get("initialPayerBalance").asText());
                assertEquals("$ 500.00", answer.get("initialPayeeBalance").asText());
                assertEquals("$ 1,000.00", answer.get("transferSum").asText());
            }
        }
    }

    @Test
    public void transactionBetweenTheSameAccountTest() throws IOException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            HttpEntity requestEntity = new StringEntity("{\"sum\": 1000,\"payerAccountId\": 1,\"payeeAccountId\": 1}");
            httpPut.setEntity(requestEntity);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);
                assertEquals("The Payer and the payee can't be the same.", answer.get("error").asText());
            }
        }
    }

    @Test
    public void transactionWithWrongRequestAccountTest() throws IOException {
        // lack of sum parameter.
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            HttpEntity requestEntity = new StringEntity("{\"payerAccountId\": 1,\"payeeAccountId\": 1}");
            httpPut.setEntity(requestEntity);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);
                assertEquals("Sum can't be null", answer.get("error").asText());
            }
        }

        // lack of full request.
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);
                assertEquals("Request can't be null", answer.get("error").asText());
            }
        }

        // lack of full request.
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            HttpEntity requestEntity = new StringEntity("{\"payerAccountId\": \"hello world\",\"payeeAccountId\": 1}");
            httpPut.setEntity(requestEntity);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);
                assertTrue(answer.get("error").asText().startsWith("java.lang.NumberFormatException"));
            }
        }

        // Ignoring unrecognized field
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            HttpEntity requestEntity = new StringEntity("{\"sum\":\"10\",\"test\": \"hello world\",\"payeeAccountId\": 1,\"payerAccountId\": 2}");
            httpPut.setEntity(requestEntity);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            }
        }

        // Incorrect Json
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            HttpEntity requestEntity = new StringEntity("Hello World");
            httpPut.setEntity(requestEntity);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);
                assertTrue(answer.get("error").asText().startsWith("java.lang.IllegalStateException"));
            }
        }
    }

    @Test
    public void transactionWithNonexistingAccountTest() throws IOException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            HttpEntity requestEntity = new StringEntity("{\"sum\": 100, \"payerAccountId\": -1,\"payeeAccountId\": 1}");
            httpPut.setEntity(requestEntity);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);
                assertEquals("Payer not found.", answer.get("error").asText());
            }
        }
    }

    @Test
    public void transactionWithAccountWithIncorrectCurrencyTest() throws IOException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            HttpEntity requestEntity = new StringEntity("{\"sum\": 100, \"payerAccountId\": 7,\"payeeAccountId\": 1}");
            httpPut.setEntity(requestEntity);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);
                assertEquals("Currencies differ: GBP/USD", answer.get("error").asText());
            }
        }
    }

    @Test
    public void checkTotalSystemBalanceTest() throws IOException {
        BigDecimal totalSystemBalance = getToTalSystemBalance();
        assertEquals(new BigDecimal("3000"), totalSystemBalance);
    }

    @Test
    public void transactionCheckingSystemForRobustWhenALotOfRequestsChangeBalanceTest() throws IOException {
        final BigDecimal totalBalance = getToTalSystemBalance();
        final ExecutorService service = Executors.newCachedThreadPool();
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
            connManager.setMaxTotal(10);
            HttpClients.custom().setConnectionManager(connManager);
            for (int i = 1; i < 7; i++) {
                final Integer payerAccountId = i;
                final Integer payeeAccountId = (i == 6) ? 1 : i + 1;
                service.submit((Callable<String>) () -> {
                    final HttpPut httpPut = new HttpPut(String.format("%s/transactions", API_URL));
                    httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
                    HttpEntity requestEntity = new StringEntity(String.format("{\"sum\": 100, \"payerAccountId\": %d,\"payeeAccountId\": %d}", payerAccountId, payeeAccountId));
                    httpPut.setEntity(requestEntity);
                    try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                        final HttpEntity entity = response.getEntity();
                        assertNotNull(entity);
                        assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                        final String json = EntityUtils.toString(entity);
                        final ObjectMapper mapper = new ObjectMapper();
                        final JsonNode answer = mapper.readTree(json);
                        assertEquals("OK", answer.get("status").asText());
                    }
                    return null;
                });
            }
            awaitTerminationAfterShutdown(service);
        }
        final BigDecimal afterTotalBalance = getToTalSystemBalance();
        assertEquals(totalBalance, afterTotalBalance);
    }

    public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public BigDecimal getToTalSystemBalance() throws IOException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(String.format("%s/transactions/total-system-balance/USD", API_URL));
            try (final CloseableHttpResponse response = httpClient.execute(httpGet)) {
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);
                return new BigDecimal(answer.get("totalBalance").asLong());
            }
        }
    }

    @Test
    public void getToTalSystemBalanceWithIncorrectCurrencyTest() throws IOException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(String.format("%s/transactions/total-system-balance/USD1", API_URL));
            try (final CloseableHttpResponse response = httpClient.execute(httpGet)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
                final HttpEntity entity = response.getEntity();
                assertNotNull(entity);
                assertEquals(MediaType.APPLICATION_JSON, entity.getContentType().getValue());
                final String json = EntityUtils.toString(entity);
                final ObjectMapper mapper = new ObjectMapper();
                final JsonNode answer = mapper.readTree(json);
                assertTrue(answer.get("error").asText().startsWith("Unknown currency"));
            }
        }
    }

    @Test
    public void page302StatusTest() throws IOException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(API_URL);
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void pageNotFoundTest() throws IOException {
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut httpPut = new HttpPut(String.format("%s/hello", API_URL));
            httpPut.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            try (final CloseableHttpResponse response = httpClient.execute(httpPut)) {
                assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            }
        }
    }
}