package exceptions.errors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class OrderNotFoundException extends WebApplicationException {
    public OrderNotFoundException(int id) {
        super("Order with id " + id + " not found", Response.Status.NOT_FOUND);
    }
}
