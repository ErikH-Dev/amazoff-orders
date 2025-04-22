package clients;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import dto.BuyerDTO;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/buyers")
@RegisterRestClient(configKey = "buyer-service")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface IBuyerServiceClient {

    @GET
    @Path("/{id}")
    Uni<BuyerDTO> getBuyerById(@PathParam("id") int oauthId);
}