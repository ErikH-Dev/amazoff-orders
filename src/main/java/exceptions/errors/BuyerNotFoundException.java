package exceptions.errors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class BuyerNotFoundException extends WebApplicationException {
    public BuyerNotFoundException(int id) {
        super("Buyer with id " + id + " not found", Response.Status.NOT_FOUND);
    }
}