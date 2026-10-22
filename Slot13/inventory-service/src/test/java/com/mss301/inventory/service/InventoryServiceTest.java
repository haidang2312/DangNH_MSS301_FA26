package com.mss301.inventory.service;
import com.mss301.inventory.dto.*;
import com.mss301.inventory.entity.*;
import com.mss301.inventory.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest {
    final ProductRepository resources = mock(ProductRepository.class);
    final ReservationRepository records = mock(ReservationRepository.class);
    final InventoryService service = new InventoryService(resources, records);
    final ReserveRequest request = new ReserveRequest(1L, "P1", 2);

    @Test void repeatedRequestDoesNotChangeResource() {
        Reservation r = new Reservation(); r.setOrderId(1L); r.setStatus(ReservationStatus.RESERVED);
        when(records.findById(1L)).thenReturn(Optional.of(r));
        assertSame(r,service.reserve(request));
        verifyNoInteractions(resources); verify(records,never()).save(any());
    }
    @Test void repeatedCompensationDoesNothing() {
        Reservation r = new Reservation(); r.setStatus(ReservationStatus.RELEASED);
        when(records.findById(1L)).thenReturn(Optional.of(r));
        service.release(1L); service.release(1L);
        verifyNoInteractions(resources); verify(records,never()).save(any());
    }
    @Test void missingCompensationDoesNothing() {
        when(records.findById(1L)).thenReturn(Optional.empty());
        service.release(1L); verifyNoInteractions(resources);
    }
    @Test void rejectsMissingResource() {
        when(records.findById(1L)).thenReturn(Optional.empty());
        when(resources.findById("P1")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,()->service.reserve(request)).getStatusCode());
        verify(records,never()).save(any());
    }
    @Test void rejectsInsufficientResourceWithoutWriting() {
        when(records.findById(1L)).thenReturn(Optional.empty());
        when(resources.findById("P1")).thenReturn(Optional.of(new Product("P1", "Laptop", 1000, 0)));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,()->service.reserve(request)).getStatusCode());
        verify(records,never()).save(any()); verify(resources,never()).save(any());
    }
    @Test void compensationRestoresExactlyOnce() {
        Product product = new Product("P1", "Laptop", 1000, 10);
        Reservation r = new Reservation(); r.setOrderId(1L); r.setProductId("P1"); r.setQuantity(2); r.setStatus(ReservationStatus.RESERVED);
        when(records.findById(1L)).thenReturn(Optional.of(r));
        when(resources.findById("P1")).thenReturn(Optional.of(product));
        service.release(1L);
        assertEquals(10 + 2, product.getStock());
        service.release(1L);
        assertEquals(ReservationStatus.RELEASED,r.getStatus());
        verify(resources,times(1)).findById("P1");
    }
}
