package fu.de190025.reservation.service;

import fu.de190025.reservation.dto.*;

public interface ReservationService {
  ReservationDTO create(ReservationDTO dto);

  ReservationDTO update(Long id, ReservationDTO dto);

  ReservationDetailDTO get(Long id);

  void cancel(Long id);

  PageDTO list(int page, int size, String guestName, String status);
}
