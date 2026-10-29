package fu.de190025.reservation.repository;

import fu.de190025.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.*;

public interface ReservationRepository
    extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {}
