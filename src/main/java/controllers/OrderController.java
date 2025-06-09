package controllers;

import dto.CreateOrderRequest;
import dto.UpdateOrderStatusRequest;
import interfaces.IOrderService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import saga.OrderSagaOrchestrator;
import utils.JwtUtil;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@Path("/orders")
@RolesAllowed({"buyer", "admin"})
public class OrderController {
    private static final Logger LOG = Logger.getLogger(OrderController.class);
    private final IOrderService orderService;
    private final OrderSagaOrchestrator orderSagaOrchestrator;
    private JwtUtil jwtUtil;

    public OrderController(IOrderService orderService, OrderSagaOrchestrator orderSagaOrchestrator, JwtUtil jwtUtil) {
        this.orderSagaOrchestrator = orderSagaOrchestrator;
        this.orderService = orderService;
        this.jwtUtil = jwtUtil;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> createOrder(@Valid CreateOrderRequest orderRequest) {
        String keycloakId = jwtUtil.getCurrentKeycloakUserId();
        LOG.infof("Received createOrder request for buyer keycloakId=%d", keycloakId);
        return orderSagaOrchestrator.createOrderWithSaga(orderRequest, keycloakId)
            .onItem().invoke(order -> {
                MDC.put("orderId", order.getId());
                LOG.infof("Order created successfully: orderId=%d", order.getId());
                MDC.remove("orderId");
            })
            .onItem().transform(order -> Response.ok(order).build())
            .onFailure().invoke(e -> LOG.errorf("Failed to create order: %s", e.getMessage()))
            .onFailure().recoverWithItem(e -> Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage()).build());
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getOrderById(@PathParam("id") int id) {
        MDC.put("orderId", id);
        LOG.infof("Received getOrderById request: orderId=%d", id);
        return orderService.read(id)
            .onItem().invoke(order -> LOG.infof("Order retrieved: orderId=%d", order.getId()))
            .onItem().transform(order -> Response.ok(order).build())
            .onFailure().invoke(e -> LOG.errorf("Failed to get order: %s", e.getMessage()))
            .eventually(() -> {
                MDC.remove("orderId");
                return Uni.createFrom().voidItem();
            });
    }

    @GET
    @Path("/user/{keycloakId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getAllOrdersByUser(@PathParam("keycloakId") String keycloakId) {
        LOG.infof("Received getAllOrdersByUser request: keycloakId=%s", keycloakId);
        return orderService.readAllByUser(keycloakId)
            .onItem().invoke(orders -> LOG.infof("Orders retrieved for user: keycloakId=%s, count=%d", keycloakId, orders.size()))
            .onItem().transform(orders -> Response.ok(orders).build())
            .onFailure().invoke(e -> LOG.errorf("Failed to get orders for user: %s", e.getMessage()));
    }

    @PUT
    @Path("/order-status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> updateOrderStatus(@Valid UpdateOrderStatusRequest orderStatusRequest) {
        MDC.put("orderId", orderStatusRequest.id);
        LOG.infof("Received updateOrderStatus request: orderId=%d, newStatus=%s", orderStatusRequest.id, orderStatusRequest.status);
        return orderService.updateOrderStatus(orderStatusRequest)
            .onItem().invoke(updatedOrder -> LOG.infof("Order status updated: orderId=%d, newStatus=%s", updatedOrder.getId(), updatedOrder.getStatus()))
            .onItem().transform(updatedOrder -> Response.ok(updatedOrder).build())
            .onFailure().invoke(e -> LOG.errorf("Failed to update order status: %s", e.getMessage()))
            .eventually(() -> {
                MDC.remove("orderId");
                return Uni.createFrom().voidItem();
            });
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteOrder(@PathParam("id") int id) {
        MDC.put("orderId", id);
        LOG.infof("Received deleteOrder request: orderId=%d", id);
        return orderService.delete(id)
            .onItem().invoke(v -> LOG.infof("Order deleted: orderId=%d", id))
            .onItem().transform(v -> Response.noContent().build())
            .onFailure().invoke(e -> LOG.errorf("Failed to delete order: %s", e.getMessage()))
            .eventually(() -> {
                MDC.remove("orderId");
                return Uni.createFrom().voidItem();
            });
    }
}