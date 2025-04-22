package exceptions.mappers;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import exceptions.ValidationErrorResponse;
import exceptions.Violation;

import java.util.ArrayList;
import java.util.List;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @ServerExceptionMapper
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<Violation> violations = new ArrayList<>();

        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            if (field.contains(".")) {
                field = field.substring(field.lastIndexOf('.') + 1);
            }
            String message = violation.getMessage();
            violations.add(new Violation(field, message));
        }

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ValidationErrorResponse(violations))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}