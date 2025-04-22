package exceptions.errors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class VendorNotFoundException extends WebApplicationException {
    public VendorNotFoundException(int id) {
        super("Vendor with id " + id + " not found", Response.Status.NOT_FOUND);
    }
}