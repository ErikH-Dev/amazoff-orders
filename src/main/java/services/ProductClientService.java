package services;

import dto.ProductDTO;
import dto.ReserveStockItem;
import dto.StockReleaseFailed;
import dto.StockReleased;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.*;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

@ApplicationScoped
public class ProductClientService {

    private static final Logger LOG = Logger.getLogger(ProductClientService.class);

    @Inject
    @Channel("get-products-requests")
    Emitter<JsonObject> productRequestEmitter;

    private final ConcurrentLinkedQueue<CompletableFuture<List<ProductDTO>>> pending = new ConcurrentLinkedQueue<>();

    public Uni<List<ProductDTO>> getProductsByIds(List<String> ids) { // Changed parameter type
        LOG.infof("Requesting product details for productIds=%s", ids);
        JsonObject requestJson = new JsonObject().put("productIds", ids);

        CompletableFuture<List<ProductDTO>> future = new CompletableFuture<>();
        pending.add(future);

        productRequestEmitter.send(requestJson);

        return Uni.createFrom().completionStage(future);
    }

    @Incoming("get-products-responses")
    public Uni<Void> onProductsResponse(Message<JsonObject> responseJson) {
        LOG.info("Received products response from Products service");
        JsonObject body = responseJson.getPayload();

        CompletableFuture<List<ProductDTO>> future = pending.poll();
        if (future == null)
            return Uni.createFrom().voidItem();

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
        LOG.infof("Requesting stock reservation for items=%s", items);
        JsonObject requestJson = new JsonObject().put("items", items);

        CompletableFuture<Object> future = new CompletableFuture<>();
        reservePending.add(future);

        reserveStockEmitter.send(requestJson);

        return Uni.createFrom().completionStage(future);
    }

    @Incoming("reserve-stock-responses")
    public Uni<Void> onReserveStockResponse(Message<JsonObject> responseJson) {
        LOG.info("Received reserve stock response from Products service");
        JsonObject body = responseJson.getPayload();

        CompletableFuture<Object> future = reservePending.poll();
        if (future == null)
            return Uni.createFrom().voidItem();

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

    @Inject
    @Channel("release-stock-requests")
    Emitter<JsonObject> releaseStockEmitter;

    private final ConcurrentLinkedQueue<CompletableFuture<Object>> releasePending = new ConcurrentLinkedQueue<>();

    public Uni<Object> releaseStock(List<ReserveStockItem> items) {
        LOG.infof("Requesting stock release for items=%s", items);
        JsonObject requestJson = new JsonObject().put("items", items);

        CompletableFuture<Object> future = new CompletableFuture<>();
        releasePending.add(future);

        releaseStockEmitter.send(requestJson);

        return Uni.createFrom().completionStage(future);
    }

    @Incoming("release-stock-responses")
    public Uni<Void> onReleaseStockResponse(Message<JsonObject> responseJson) {
        LOG.info("Received release stock response from Products service");
        JsonObject body = responseJson.getPayload();

        CompletableFuture<Object> future = releasePending.poll();
        if (future == null)
            return Uni.createFrom().voidItem();

        String status = body.getString("status");
        if ("StockReleased".equals(status)) {
            Object released = body.mapTo(StockReleased.class);
            future.complete(released);
        } else if ("StockReleaseFailed".equals(status)) {
            Object failed = body.mapTo(StockReleaseFailed.class);
            future.complete(failed);
        } else {
            future.completeExceptionally(new RuntimeException("Unknown release stock response"));
        }
        return Uni.createFrom().voidItem();
    }
}