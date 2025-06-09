package exceptions.errors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class BuyerNotFoundException extends WebApplicationException {
    public BuyerNotFoundException(String keycloakId) {
        super("Buyer with id " + keycloakId + " not found", Response.Status.NOT_FOUND);
    }
}