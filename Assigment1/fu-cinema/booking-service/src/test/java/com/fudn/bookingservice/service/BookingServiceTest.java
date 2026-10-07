package com.fudn.bookingservice.service;

import com.fudn.bookingservice.client.MovieClient;
import com.fudn.bookingservice.dto.*;
import com.fudn.bookingservice.exception.ApiException;
import com.fudn.bookingservice.model.*;
import com.fudn.bookingservice.repository.*;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookings;
    @Mock BookingDetailRepository details;
    @Mock MovieClient movies;
    @Mock JdbcTemplate jdbc;
    BookingService service;
    private static final String SHOW = "66f300000000000000000001";

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, details, movies, jdbc);
    }

    private ShowtimeResponse showtime() {
        return new ShowtimeResponse(SHOW, "66f200000000000000000001", "Snapshot title",
                "66f100000000000000000001", "Room 01", 5, 8,
                LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(2),
                new BigDecimal("95000"), "SCHEDULED");
    }

    private Booking booking(long customerId, LocalDateTime start, BookingStatus status) {
        Booking booking = new Booking();
        booking.setBookingId(1L);
        booking.setCustomerId(customerId);
        booking.setBookingDate(LocalDateTime.now());
        booking.setBookingStatus(status);
        booking.setTotalPrice(new BigDecimal("95000"));
        BookingDetail detail = new BookingDetail();
        detail.setMovieId("movie-one");
        detail.setMovieTitle("Snapshot title");
        detail.setShowtimeId(SHOW);
        detail.setSeatCode("A1");
        detail.setPrice(new BigDecimal("95000"));
        detail.setShowtimeStart(start);
        booking.addDetail(detail);
        return booking;
    }

    private void assertStatus(HttpStatus status, Runnable action) {
        assertEquals(status, assertThrows(ApiException.class, action::run).getStatus());
    }

    @Test
    void createsServerPricedTicketsWithSnapshotsAndReservations() {
        when(movies.getShowtime(SHOW)).thenReturn(showtime());
        when(details.findSeatCodesByShowtime(SHOW, BookingStatus.CONFIRMED)).thenReturn(List.of());
        when(bookings.saveAndFlush(any())).thenAnswer(inv -> {
            Booking saved = inv.getArgument(0);
            saved.setBookingId(5L);
            return saved;
        });
        BookingResponse response = service.create(1L, new CreateBookingRequest(List.of(
                new BookingItemRequest(SHOW, "A1"), new BookingItemRequest(SHOW, "A2"))));
        assertEquals(new BigDecimal("190000"), response.totalPrice());
        assertEquals(2, response.details().size());
        assertEquals("Snapshot title", response.details().getFirst().movieTitle());
        verify(movies, times(1)).getShowtime(SHOW);
        verify(jdbc).update(anyString(), eq(SHOW), eq("A1"), eq(5L));
        verify(jdbc).update(anyString(), eq(SHOW), eq("A2"), eq(5L));
    }

    @Test
    void databaseRaceBecomesConflict() {
        when(movies.getShowtime(SHOW)).thenReturn(showtime());
        when(details.findSeatCodesByShowtime(SHOW, BookingStatus.CONFIRMED)).thenReturn(List.of());
        when(bookings.saveAndFlush(any())).thenAnswer(inv -> {
            Booking saved = inv.getArgument(0); saved.setBookingId(5L); return saved;
        });
        when(jdbc.update(anyString(), eq(SHOW), eq("A1"), eq(5L)))
                .thenThrow(new DuplicateKeyException("concurrent reservation"));
        assertStatus(HttpStatus.CONFLICT, () -> service.create(1L,
                new CreateBookingRequest(List.of(new BookingItemRequest(SHOW, "A1")))));
    }

    @Test
    void rejectsDuplicateSeatsWithinRequest() {
        when(movies.getShowtime(SHOW)).thenReturn(showtime());
        when(details.findSeatCodesByShowtime(SHOW, BookingStatus.CONFIRMED)).thenReturn(List.of());
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.create(1L, new CreateBookingRequest(List.of(
                new BookingItemRequest(SHOW, "A1"), new BookingItemRequest(SHOW, "A1")))));
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSeatOutsideRoom() {
        when(movies.getShowtime(SHOW)).thenReturn(showtime());
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.create(1L,
                new CreateBookingRequest(List.of(new BookingItemRequest(SHOW, "E9")))));
        verify(bookings, never()).saveAndFlush(any());
    }

    @Test
    void mapsMissingShowtimeToNotFound() {
        when(movies.getShowtime(SHOW)).thenThrow(mock(FeignException.NotFound.class));
        assertStatus(HttpStatus.NOT_FOUND, () -> service.getSeatMap(SHOW));
    }

    @Test
    void mapsUnavailableMovieServiceTo503() {
        when(movies.getShowtime(SHOW)).thenThrow(mock(FeignException.class));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> service.getSeatMap(SHOW));
    }

    @Test
    void deniesAnotherCustomersBooking() {
        when(bookings.findById(1L)).thenReturn(Optional.of(booking(1L, LocalDateTime.now().plusDays(1), BookingStatus.CONFIRMED)));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.getById(1L, 2L, "CUSTOMER"));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.cancel(1L, 2L, "CUSTOMER"));
    }

    @Test
    void customerCannotCancelWithinTwoHours() {
        when(bookings.findById(1L)).thenReturn(Optional.of(booking(1L, LocalDateTime.now().plusMinutes(119), BookingStatus.CONFIRMED)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.cancel(1L, 1L, "CUSTOMER"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void adminCanCancelAfterShowtimeAndReleasesSeat() {
        Booking booking = booking(1L, LocalDateTime.now().minusDays(1), BookingStatus.CONFIRMED);
        when(bookings.findById(1L)).thenReturn(Optional.of(booking));
        when(bookings.save(booking)).thenReturn(booking);
        assertEquals(BookingStatus.CANCELLED, service.cancel(1L, 0L, "ADMIN").bookingStatus());
        verify(jdbc).update("DELETE FROM seat_reservation WHERE booking_id = ?", 1L);
    }

    @Test
    void cannotCancelTwice() {
        when(bookings.findById(1L)).thenReturn(Optional.of(booking(1L, LocalDateTime.now().plusDays(1), BookingStatus.CANCELLED)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.cancel(1L, 1L, "CUSTOMER"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void reportUsesExclusiveNextDayAndSnapshotRevenue() {
        LocalDate day = LocalDate.of(2026, 10, 7);
        Booking low = booking(1L, day.plusDays(1).atStartOfDay(), BookingStatus.CONFIRMED);
        Booking high = booking(2L, day.plusDays(1).atStartOfDay(), BookingStatus.CONFIRMED);
        high.getDetails().getFirst().setMovieId("movie-two");
        high.getDetails().getFirst().setPrice(new BigDecimal("150000"));
        high.setTotalPrice(new BigDecimal("150000"));
        when(bookings.findForReport(BookingStatus.CONFIRMED, day.atStartOfDay(), day.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(low, high));
        ReportResponse report = service.report(day, day);
        assertEquals(2, report.totalBookings());
        assertEquals(2, report.totalTickets());
        assertEquals(new BigDecimal("245000"), report.totalRevenue());
        assertEquals("movie-two", report.revenueByMovie().getFirst().movieId());
        verifyNoInteractions(movies);
    }

    @Test
    void rejectsReversedReportRange() {
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.report(LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1)));
        verifyNoInteractions(bookings);
    }
}
