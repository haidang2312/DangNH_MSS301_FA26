package fu.de190025.room.service;

import fu.de190025.room.dto.*;

public interface RoomService {
  RoomDTO create(RoomDTO dto);

  RoomDTO update(Long id, RoomDTO dto);

  RoomDTO get(Long id);

  void maintenance(Long id);

  PageDTO list(int page, int size, String roomType, String status);
}
