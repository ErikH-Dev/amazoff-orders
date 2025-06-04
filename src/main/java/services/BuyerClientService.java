package services;

import dto.BuyerDTO;
import exceptions.errors.BuyerNotFoundException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.*;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class BuyerClientService {

    private static final Logger LOG = Logger.getLogger(BuyerClientService.class);

    @Inject
    @Channel("get-buyer-requests")
    Emitter<JsonObject> requestEmitter;

    private final ConcurrentHashMap<Integer, CompletableFuture<BuyerDTO>> pendingRequests = new ConcurrentHashMap<>();

    public Uni<BuyerDTO> getBuyerByOauthId(int oauthId) {
        LOG.infof("Requesting buyer details for oauthId=%d", oauthId);
        CompletableFuture<BuyerDTO> future = new CompletableFuture<>();
        pendingRequests.put(oauthId, future);
        JsonObject requestJson = new JsonObject().put("oauthId", oauthId);
        
        try {
            requestEmitter.send(requestJson);
        } catch (Exception e) {
            LOG.errorf("Failed to send buyer request: %s", e.getMessage());
            pendingRequests.remove(oauthId);
            return Uni.createFrom().failure(e);
        }
        
        return Uni.createFrom().completionStage(future)
                .onFailure().invoke(e -> {
                    pendingRequests.remove(oauthId);
                    LOG.errorf("Failed to get buyer for oauthId=%d: %s", oauthId, e.getMessage());
                });
    }

    @Incoming("get-buyer-responses")
    public Uni<Void> onBuyerResponse(Message<JsonObject> message) {
        LOG.info("Received buyer response from Users service");
        JsonObject json = message.getPayload();

        // Check if this is an error response
        if (json.getBoolean("error", false)) {
            int oauthId = json.getInteger("oauthId");
            String errorMessage = json.getString("message", "Unknown error");
            LOG.warnf("Received error response for oauthId=%d: %s", oauthId, errorMessage);

            CompletableFuture<BuyerDTO> future = pendingRequests.remove(oauthId);
            if (future != null) {
                future.completeExceptionally(new BuyerNotFoundException(oauthId));
            }
            return Uni.createFrom().completionStage(message.ack()).replaceWithVoid();
        }

        BuyerDTO buyer;
        try {
            buyer = json.mapTo(BuyerDTO.class);
        } catch (Exception e) {
            LOG.errorf("Failed to parse buyer response: %s", e.getMessage());
            return Uni.createFrom().completionStage(message.ack()).replaceWithVoid();
        }

        int oauthId = buyer.oauthId;
        CompletableFuture<BuyerDTO> future = pendingRequests.remove(oauthId);
        if (future != null) {
            future.complete(buyer);
        }
        return Uni.createFrom().completionStage(message.ack()).replaceWithVoid();
    }
}