package fu.de190025.room.repository;

import fu.de190025.room.entity.Room;
import org.springframework.data.jpa.repository.*;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
  boolean existsByRoomNumberAndRoomIdNot(String roomNumber, Long roomId);
}
