package fu.de190025.room.controller;

import fu.de190025.room.dto.*;
import fu.de190025.room.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/rooms", produces = "application/json")
public class DE190025RoomController {
  private final RoomService service;

  public DE190025RoomController(RoomService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ApiResponseDTO> create(@RequestBody RoomDTO d) {
    return ResponseEntity.status(201).body(ApiResponseDTO.ok(service.create(d)));
  }

  @PutMapping("/{id}")
  public ApiResponseDTO update(@PathVariable Long id, @RequestBody RoomDTO d) {
    return ApiResponseDTO.ok(service.update(id, d));
  }

  @GetMapping("/{id}")
  public ApiResponseDTO get(@PathVariable Long id) {
    return ApiResponseDTO.ok(service.get(id));
  }

  @DeleteMapping("/{id}")
  public ApiResponseDTO delete(@PathVariable Long id) {
    service.maintenance(id);
    return ApiResponseDTO.ok(null);
  }

  @GetMapping
  public ApiResponseDTO list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String roomType,
      @RequestParam(required = false) String status) {
    return ApiResponseDTO.ok(service.list(page, size, roomType, status));
  }
}
