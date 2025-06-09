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

    private final ConcurrentHashMap<String, CompletableFuture<BuyerDTO>> pendingRequests = new ConcurrentHashMap<>();

    public Uni<BuyerDTO> getBuyerByKeycloakId(String keycloakId) {
        LOG.infof("Requesting buyer details for keycloakId=%s", keycloakId);
        CompletableFuture<BuyerDTO> future = new CompletableFuture<>();
        pendingRequests.put(keycloakId, future);
        JsonObject requestJson = new JsonObject().put("keycloakId", keycloakId);

        try {
            requestEmitter.send(requestJson);
        } catch (Exception e) {
            LOG.errorf("Failed to send buyer request: %s", e.getMessage());
            pendingRequests.remove(keycloakId);
            return Uni.createFrom().failure(e);
        }
        
        return Uni.createFrom().completionStage(future)
                .onFailure().invoke(e -> {
                    pendingRequests.remove(keycloakId);
                    LOG.errorf("Failed to get buyer for keycloakId=%d: %s", keycloakId, e.getMessage());
                });
    }

    @Incoming("get-buyer-responses")
    public Uni<Void> onBuyerResponse(Message<JsonObject> message) {
        LOG.info("Received buyer response from Users service");
        JsonObject json = message.getPayload();

        if (json.getBoolean("error", false)) {
            String keycloakId = json.getString("keycloakId");
            String errorMessage = json.getString("message", "Unknown error");
            LOG.warnf("Received error response for keycloakId=%s: %s", keycloakId, errorMessage);

            CompletableFuture<BuyerDTO> future = pendingRequests.remove(keycloakId);
            if (future != null) {
                future.completeExceptionally(new BuyerNotFoundException(keycloakId));
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

        String keycloakId = buyer.keycloakId;
        CompletableFuture<BuyerDTO> future = pendingRequests.remove(keycloakId);
        if (future != null) {
            future.complete(buyer);
        }
        return Uni.createFrom().completionStage(message.ack()).replaceWithVoid();
    }
}