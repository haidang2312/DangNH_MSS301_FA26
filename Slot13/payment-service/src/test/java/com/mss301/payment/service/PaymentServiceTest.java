package com.mss301.payment.service;
import com.mss301.payment.dto.*;
import com.mss301.payment.entity.*;
import com.mss301.payment.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    final WalletRepository resources = mock(WalletRepository.class);
    final PaymentRepository records = mock(PaymentRepository.class);
    final PaymentService service = new PaymentService(resources, records);
    final PaymentRequest request = new PaymentRequest(1L, "C1", 2000);

    @Test void repeatedRequestDoesNotChangeResource() {
        Payment r = new Payment(); r.setOrderId(1L); r.setStatus(PaymentStatus.PAID);
        when(records.findById(1L)).thenReturn(Optional.of(r));
        assertSame(r,service.pay(request));
        verifyNoInteractions(resources); verify(records,never()).save(any());
    }
    @Test void repeatedCompensationDoesNothing() {
        Payment r = new Payment(); r.setStatus(PaymentStatus.REFUNDED);
        when(records.findById(1L)).thenReturn(Optional.of(r));
        service.refund(1L); service.refund(1L);
        verifyNoInteractions(resources); verify(records,never()).save(any());
    }
    @Test void missingCompensationDoesNothing() {
        when(records.findById(1L)).thenReturn(Optional.empty());
        service.refund(1L); verifyNoInteractions(resources);
    }
    @Test void rejectsMissingResource() {
        when(records.findById(1L)).thenReturn(Optional.empty());
        when(resources.findById("C1")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,()->service.pay(request)).getStatusCode());
        verify(records,never()).save(any());
    }
    @Test void rejectsInsufficientResourceWithoutWriting() {
        when(records.findById(1L)).thenReturn(Optional.empty());
        when(resources.findById("C1")).thenReturn(Optional.of(new Wallet("C1", 100)));
        assertEquals(HttpStatus.PAYMENT_REQUIRED,assertThrows(ResponseStatusException.class,()->service.pay(request)).getStatusCode());
        verify(records,never()).save(any()); verify(resources,never()).save(any());
    }
    @Test void compensationRestoresExactlyOnce() {
        Wallet product = new Wallet("C1", 5000);
        Payment r = new Payment(); r.setOrderId(1L); r.setCustomerId("C1"); r.setAmount(2000); r.setStatus(PaymentStatus.PAID);
        when(records.findById(1L)).thenReturn(Optional.of(r));
        when(resources.findById("C1")).thenReturn(Optional.of(product));
        service.refund(1L);
        assertEquals(7000.0, product.getBalance());
        service.refund(1L);
        assertEquals(PaymentStatus.REFUNDED,r.getStatus());
        verify(resources,times(1)).findById("C1");
    }
}
