package com.fudn.movieservice.service;

import com.fudn.movieservice.dto.ShowtimeRequest;
import com.fudn.movieservice.dto.ShowtimeResponse;
import com.fudn.movieservice.exception.ApiException;
import com.fudn.movieservice.model.*;
import com.fudn.movieservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTest {
    @Mock ShowtimeRepository showtimes;
    @Mock MovieRepository movieRepository;
    @Mock RoomRepository roomRepository;
    @Mock MovieService movies;
    @Mock RoomService rooms;
    ShowtimeService service;
    Movie movie;
    CinemaRoom room;
    ShowtimeRequest request;

    @BeforeEach
    void setUp() {
        service = new ShowtimeService(showtimes, movieRepository, roomRepository, movies, rooms);
        movie = new Movie();
        movie.setMovieId("movie"); movie.setTitle("Test movie");
        movie.setDurationMinutes(125); movie.setMovieStatus(MovieStatus.NOW_SHOWING);
        room = new CinemaRoom("room", "Room 01", RoomType.STANDARD, 5, 8, RoomStatus.ACTIVE);
        request = new ShowtimeRequest("movie", "room", LocalDateTime.now().plusDays(7), new BigDecimal("95000"));
        when(movies.find("movie")).thenReturn(movie);
        when(rooms.find("room")).thenReturn(room);
    }

    @Test
    void computesEndTimeAndIncludesRoomDimensions() {
        when(showtimes.save(any())).thenAnswer(inv -> {
            Showtime s = inv.getArgument(0); s.setShowtimeId("showtime"); return s;
        });
        ShowtimeResponse response = service.create(request);
        assertEquals(request.startTime().plusMinutes(125), response.endTime());
        assertEquals(5, response.seatRows());
        assertEquals(8, response.seatsPerRow());
        assertEquals(ShowtimeStatus.SCHEDULED, response.showtimeStatus());
        verify(showtimes).countByRoomIdAndShowtimeStatusAndStartTimeLessThanAndEndTimeGreaterThanAndShowtimeIdNot(
                "room", ShowtimeStatus.SCHEDULED, request.startTime().plusMinutes(125), request.startTime(), "");
    }

    @Test
    void rejectsOverlappingScheduledShowtimes() {
        when(showtimes.countByRoomIdAndShowtimeStatusAndStartTimeLessThanAndEndTimeGreaterThanAndShowtimeIdNot(
                anyString(), eq(ShowtimeStatus.SCHEDULED), any(), any(), anyString())).thenReturn(1L);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ApiException.class, () -> service.create(request)).getStatus());
        verify(showtimes, never()).save(any());
    }

    @Test
    void rejectsEndedMovie() {
        movie.setMovieStatus(MovieStatus.ENDED);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class, () -> service.create(request)).getStatus());
        verifyNoInteractions(showtimes);
    }

    @Test
    void rejectsRoomUnderMaintenance() {
        room.setRoomStatus(RoomStatus.MAINTENANCE);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class, () -> service.create(request)).getStatus());
        verifyNoInteractions(showtimes);
    }
}
