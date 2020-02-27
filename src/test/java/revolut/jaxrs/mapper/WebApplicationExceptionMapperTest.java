package revolut.jaxrs.mapper;

import org.junit.jupiter.api.Test;
import revolut.jaxrs.ErrorMessage;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebApplicationExceptionMapperTest {
    @Test
    void toResponse() {
        WebApplicationExceptionMapper mapper = new WebApplicationExceptionMapper();
        Response response = mapper.toResponse(new BadRequestException("some error"));
        ErrorMessage message = (ErrorMessage) response.getEntity();
        assertEquals("some error", message.getError());
        assertEquals(400, response.getStatus());
    }
}