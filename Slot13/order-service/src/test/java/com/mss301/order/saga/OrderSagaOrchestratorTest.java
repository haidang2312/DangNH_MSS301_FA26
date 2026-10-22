package com.mss301.order.saga;

import com.mss301.order.client.InventoryClient;
import com.mss301.order.client.PaymentClient;
import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.*;
import com.mss301.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderSagaOrchestratorTest {
    private final OrderService orders = mock(OrderService.class);
    private final InventoryClient inventory = mock(InventoryClient.class);
    private final PaymentClient payment = mock(PaymentClient.class);
    private final CreateOrderRequest request = new CreateOrderRequest("C1", "P1", 1, 1000);
    private final OrderSagaOrchestrator saga = new OrderSagaOrchestrator(orders, inventory, payment);

    @BeforeEach
    void setUp() {
        Order order = new Order();
        order.setId(1L);
        when(orders.createPendingOrder(request)).thenReturn(order);
        when(orders.findById(1L)).thenReturn(order);
    }

    @Test
    void completesInOrderWithoutCompensation() {
        saga.placeOrder(request);
        var sequence = inOrder(inventory, payment, orders);
        sequence.verify(inventory).reserve(1L, "P1", 1);
        sequence.verify(payment).pay(1L, "C1", 1000);
        sequence.verify(orders).confirmOrder(1L);
        verify(inventory, never()).release(anyLong());
        verify(payment, never()).refund(anyLong());
        verify(orders, never()).cancelOrder(anyLong());
    }

    @Test
    void releasesInventoryBeforeCancellingWhenPaymentFails() {
        doThrow(new RestClientException("payment unavailable"))
                .when(payment).pay(1L, "C1", 1000);
        saga.placeOrder(request);
        var sequence = inOrder(orders, inventory);
        sequence.verify(orders).markFailed(eq(1L), contains("payment unavailable"));
        sequence.verify(orders).updateSagaStatus(1L, SagaStatus.COMPENSATING);
        sequence.verify(inventory).release(1L);
        sequence.verify(orders).cancelOrder(1L);
        verify(payment, never()).refund(anyLong());
    }

    @Test
    void cancelsWithoutReleasingWhenReservationFails() {
        doThrow(new RestClientException("out of stock"))
                .when(inventory).reserve(1L, "P1", 1);
        saga.placeOrder(request);
        verify(orders).cancelOrder(1L);
        verify(inventory, never()).release(anyLong());
        verifyNoInteractions(payment);
    }

    @Test
    void compensatesSuccessfulStepsInReverseOrder() {
        doThrow(new RestClientException("confirmation failed"))
                .when(orders).confirmOrder(1L);
        saga.placeOrder(request);
        var sequence = inOrder(payment, inventory, orders);
        sequence.verify(payment).refund(1L);
        sequence.verify(inventory).release(1L);
        sequence.verify(orders).cancelOrder(1L);
    }

    @Test
    void keepsCompensatingAfterExactlyThreeFailedAttempts() {
        doThrow(new RestClientException("payment unavailable"))
                .when(payment).pay(1L, "C1", 1000);
        doThrow(new RestClientException("inventory unavailable"))
                .when(inventory).release(1L);
        saga.placeOrder(request);
        verify(inventory, times(3)).release(1L);
        verify(orders, times(3)).log(eq(1L), eq("COMPENSATION"), eq("FAILED"), anyString());
        verify(orders).updateSagaStatus(1L, SagaStatus.COMPENSATING);
        verify(orders, never()).cancelOrder(anyLong());
    }
}
