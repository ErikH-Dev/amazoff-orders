package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import enums.OrderStatus;

public class UpdateOrderStatusRequest {
    @JsonProperty("id")
    public int id;

    @NotNull(message = "Order status cannot be null")
    @JsonProperty("status")
    public OrderStatus status;

    public UpdateOrderStatusRequest() {}

    public UpdateOrderStatusRequest(int id, OrderStatus orderStatus) {
        this.id = id;
        this.status = orderStatus;
    }
}