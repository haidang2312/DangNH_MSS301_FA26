package fu.de190025.reservation.client;

import fu.de190025.reservation.dto.RoomDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "room-service", url = "${room.service.url}")
public interface RoomClient {
  record RoomResponse(int status, String message, RoomDTO data, String timestamp) {}

  @GetMapping("/api/rooms/{id}")
  RoomResponse getRoom(@PathVariable("id") Long id);
}
