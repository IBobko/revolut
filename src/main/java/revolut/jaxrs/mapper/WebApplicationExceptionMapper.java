package revolut.jaxrs.mapper;

import revolut.jaxrs.ErrorMessage;

import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Adds human readable text to client errors, in addition avoids printing stack
 * trace with exception.
 *
 * @author szymon
 */
@Provider
@Singleton
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException e) {
        final ErrorMessage error = new ErrorMessage();
        error.setError(e.getMessage());
        GenericEntity<ErrorMessage> ge = new GenericEntity<>(error, ErrorMessage.class);
        return Response.fromResponse(e.getResponse()).entity(ge).build();
    }
}
