package exceptions.errors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class ProductNotFoundException extends WebApplicationException {
    public ProductNotFoundException(int id) {
        super("Product with id " + id + " not found", Response.Status.NOT_FOUND);
    }
}