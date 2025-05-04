package services;

import dto.BuyerDTO;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class BuyerClientService {

    @Inject
    @Channel("get-buyer-requests")
    Emitter<String> requestEmitter;

    private final ConcurrentHashMap<Integer, CompletableFuture<BuyerDTO>> pendingRequests = new ConcurrentHashMap<>();

    public Uni<BuyerDTO> getBuyerByOauthId(int oauthId) {
        CompletableFuture<BuyerDTO> future = new CompletableFuture<>();
        pendingRequests.put(oauthId, future);
        requestEmitter.send(String.valueOf(oauthId));
        return Uni.createFrom().completionStage(future);
    }

    @Incoming("get-buyer-responses")
    public Uni<Void> onVendorResponse(Message<JsonObject> message) {
        JsonObject json = message.getPayload();
        BuyerDTO buyer;
        try {
            buyer = json.mapTo(BuyerDTO.class);
        } catch (Exception e) {
            // handle error, maybe log and ack
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