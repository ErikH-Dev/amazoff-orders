package clients;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import dto.ProductDTO;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/products")
@RegisterRestClient(configKey = "product-service")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface IProductServiceClient {
    @GET
    @Path("/batch")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<List<ProductDTO>> getProductsByIds(@QueryParam("ids") List<Integer> ids);
}
