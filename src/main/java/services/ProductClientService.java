package services;

import dto.ProductDTO;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProductClientService {

    @Inject
    @Channel("get-products-requests")
    Emitter<String> productRequestEmitter;

    // Use a queue to track pending requests (FIFO)
    private final ConcurrentLinkedQueue<CompletableFuture<List<ProductDTO>>> pending = new ConcurrentLinkedQueue<>();

    public Uni<List<ProductDTO>> getProductsByIds(List<Integer> ids) {
        // Prepare request JSON without correlationId
        JsonObject requestJson = new JsonObject()
                .put("productIds", ids);

        CompletableFuture<List<ProductDTO>> future = new CompletableFuture<>();
        pending.add(future);

        // Send request as JSON string
        productRequestEmitter.send(requestJson.encode());

        // Return as Uni
        return Uni.createFrom().completionStage(future);
    }

    @Incoming("get-products-responses")
    public Uni<Void> onProductsResponse(Message<JsonObject> responseJson) {
        JsonObject body = responseJson.getPayload();

        CompletableFuture<List<ProductDTO>> future = pending.poll();
        if (future == null) return Uni.createFrom().voidItem();

        // Map JSON array to List<ProductDTO>
        List<ProductDTO> products = body.getJsonArray("products")
                .stream()
                .map(obj -> ((JsonObject) obj).mapTo(ProductDTO.class))
                .toList();

        future.complete(products);
        return Uni.createFrom().voidItem();
    }
}