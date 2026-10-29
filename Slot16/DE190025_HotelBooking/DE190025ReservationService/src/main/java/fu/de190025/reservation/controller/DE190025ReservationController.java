package fu.de190025.reservation.controller;

import fu.de190025.reservation.dto.*;
import fu.de190025.reservation.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/reservations", produces = "application/json")
public class DE190025ReservationController {
  private final ReservationService service;

  public DE190025ReservationController(ReservationService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ApiResponseDTO> create(@RequestBody ReservationDTO d) {
    return ResponseEntity.status(201).body(ApiResponseDTO.ok(service.create(d)));
  }

  @PutMapping("/{id}")
  public ApiResponseDTO update(@PathVariable Long id, @RequestBody ReservationDTO d) {
    return ApiResponseDTO.ok(service.update(id, d));
  }

  @GetMapping("/{id}")
  public ApiResponseDTO get(@PathVariable Long id) {
    return ApiResponseDTO.ok(service.get(id));
  }

  @DeleteMapping("/{id}")
  public ApiResponseDTO delete(@PathVariable Long id) {
    service.cancel(id);
    return ApiResponseDTO.ok(null);
  }

  @GetMapping
  public ApiResponseDTO list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String guestName,
      @RequestParam(required = false) String status) {
    return ApiResponseDTO.ok(service.list(page, size, guestName, status));
  }
}
