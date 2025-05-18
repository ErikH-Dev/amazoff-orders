package services;

import dto.ProductDTO;
import dto.ReserveStockItem;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

@ApplicationScoped
public class ProductClientService {

    @Inject
    @Channel("get-products-requests")
    Emitter<JsonObject> productRequestEmitter;

    private final ConcurrentLinkedQueue<CompletableFuture<List<ProductDTO>>> pending = new ConcurrentLinkedQueue<>();

    public Uni<List<ProductDTO>> getProductsByIds(List<Integer> ids) {
        JsonObject requestJson = new JsonObject().put("productIds", ids);

        CompletableFuture<List<ProductDTO>> future = new CompletableFuture<>();
        pending.add(future);

        productRequestEmitter.send(requestJson);

        return Uni.createFrom().completionStage(future);
    }

    @Incoming("get-products-responses")
    public Uni<Void> onProductsResponse(Message<JsonObject> responseJson) {
        JsonObject body = responseJson.getPayload();

        CompletableFuture<List<ProductDTO>> future = pending.poll();
        if (future == null) return Uni.createFrom().voidItem();

        List<ProductDTO> products = body.getJsonArray("products")
                .stream()
                .map(obj -> ((JsonObject) obj).mapTo(ProductDTO.class))
                .toList();

        future.complete(products);
        return Uni.createFrom().voidItem();
    }

    @Inject
    @Channel("reserve-stock-requests")
    Emitter<JsonObject> reserveStockEmitter;

    private final ConcurrentLinkedQueue<CompletableFuture<Object>> reservePending = new ConcurrentLinkedQueue<>();

    public Uni<Object> reserveStock(List<ReserveStockItem> items) {
        JsonObject requestJson = new JsonObject().put("items", items);

        CompletableFuture<Object> future = new CompletableFuture<>();
        reservePending.add(future);

        reserveStockEmitter.send(requestJson);

        return Uni.createFrom().completionStage(future);
    }

    @Incoming("reserve-stock-responses")
    public Uni<Void> onReserveStockResponse(Message<JsonObject> responseJson) {
        JsonObject body = responseJson.getPayload();

        CompletableFuture<Object> future = reservePending.poll();
        if (future == null) return Uni.createFrom().voidItem();

        String status = body.getString("status");
        if ("StockReserved".equals(status)) {
            Object reserved = body.mapTo(dto.StockReserved.class);
            future.complete(reserved);
        } else if ("StockReservationFailed".equals(status)) {
            Object failed = body.mapTo(dto.StockReservationFailed.class);
            future.complete(failed);
        } else {
            future.completeExceptionally(new RuntimeException("Unknown reserve stock response"));
        }
        return Uni.createFrom().voidItem();
    }
}