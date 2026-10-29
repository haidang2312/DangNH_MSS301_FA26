package fu.de190025.reservation.service.impl;

import fu.de190025.reservation.client.RoomClient;
import fu.de190025.reservation.common.*;
import fu.de190025.reservation.dto.*;
import fu.de190025.reservation.entity.*;
import fu.de190025.reservation.repository.ReservationRepository;
import fu.de190025.reservation.service.ReservationService;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {
  private final ReservationRepository repo;
  private final RoomClient rooms;

  public ReservationServiceImpl(ReservationRepository repo, RoomClient rooms) {
    this.repo = repo;
    this.rooms = rooms;
  }

  private Reservation find(Long id) {
    return repo.findById(id).orElseThrow(() -> new ApiException(4, "Reservation not found"));
  }

  private RoomDTO room(Long id) {
    try {
      var response = rooms.getRoom(id);
      return response == null ? null : response.data();
    } catch (feign.FeignException.NotFound e) {
      return null;
    }
  }

  public ReservationDTO create(ReservationDTO d) {
    Reservation p = new Reservation();
    apply(p, d);
    p.setStatus(ReservationStatus.CONFIRMED);
    validate(p);
    RoomDTO u = room(p.getRoomId());
    if (u == null) throw new ApiException(4, "Room ID is not found");
    if (u.getStatus() != RoomStatus.AVAILABLE)
      throw new ApiException(5, "Room is not AVAILABLE for reservation");
    capacity(p, u);
    cost(p, u);
    return dto(repo.saveAndFlush(p));
  }

  public ReservationDTO update(Long id, ReservationDTO d) {
    Reservation current = find(id);
    // Validate a detached candidate before mutating the managed entity.
    Reservation candidate = new Reservation();
    apply(candidate, dto(current));
    apply(candidate, d);
    validate(candidate);
    RoomDTO u = room(candidate.getRoomId());
    if (u == null) throw new ApiException(4, "Room ID is not found");
    capacity(candidate, u);
    cost(candidate, u);
    apply(current, dto(candidate));
    current.setTotalAmount(candidate.getTotalAmount());
    return dto(repo.saveAndFlush(current));
  }

  public ReservationDetailDTO get(Long id) {
    return detail(find(id));
  }

  public void cancel(Long id) {
    find(id).setStatus(ReservationStatus.CANCELLED);
  }

  public PageDTO list(int page, int size, String guestName, String status) {
    var pageable = Validation.page(page, size);
    ReservationStatus s = status == null ? null : ReservationStatus.valueOf(status);
    var result =
        repo.findAll(
            (root, q, cb) -> {
              var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
              if (guestName != null && !guestName.isBlank())
                predicates.add(
                    cb.like(
                        cb.lower(root.get("guestName")),
                        "%"
                            + guestName
                                .toLowerCase(java.util.Locale.ROOT)
                                .replace("\\", "\\\\")
                                .replace("%", "\\%")
                                .replace("_", "\\_")
                            + "%",
                        '\\'));
              if (s != null) predicates.add(cb.equal(root.get("status"), s));
              return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
            },
            pageable);
    return PageDTO.of(result, result.getContent().stream().map(this::detail).toList());
  }

  private void validate(Reservation p) {
    Validation.text(p.getGuestName(), 100, "guestName");
    Validation.email(p.getGuestEmail());
    Validation.text(p.getGuestPhone(), 20, "guestPhone");
    Validation.require(
        p.getCheckInDate() != null
            && p.getCheckOutDate() != null
            && p.getCheckOutDate().after(p.getCheckInDate()),
        "checkOutDate must be after checkInDate");
    Validation.require(
        p.getNumberOfGuests() != null && p.getNumberOfGuests() >= 1 && p.getNumberOfGuests() <= 10,
        "Invalid numberOfGuests");
    Validation.require(p.getRoomId() != null && p.getRoomId() > 0, "roomId is required");
  }

  private void capacity(Reservation p, RoomDTO u) {
    Validation.require(p.getNumberOfGuests() <= u.getCapacity(), "numberOfGuests exceeds capacity");
  }

  private void cost(Reservation p, RoomDTO u) {
    long days =
        ChronoUnit.DAYS.between(
            java.time.Instant.ofEpochMilli(p.getCheckInDate().getTime())
                .atZone(ZoneOffset.UTC)
                .toLocalDate(),
            java.time.Instant.ofEpochMilli(p.getCheckOutDate().getTime())
                .atZone(ZoneOffset.UTC)
                .toLocalDate());
    BigDecimal cost = u.getPricePerNight().multiply(BigDecimal.valueOf(days));
    Validation.require(
        cost.precision() - cost.scale() <= 16, "totalAmount exceeds database precision");
    p.setTotalAmount(cost);
  }

  private void apply(Reservation p, ReservationDTO d) {
    if (d.getGuestName() != null) p.setGuestName(d.getGuestName());
    if (d.getGuestEmail() != null) p.setGuestEmail(d.getGuestEmail());
    if (d.getGuestPhone() != null) p.setGuestPhone(d.getGuestPhone());
    if (d.getCheckInDate() != null) p.setCheckInDate(d.getCheckInDate());
    if (d.getCheckOutDate() != null) p.setCheckOutDate(d.getCheckOutDate());
    if (d.getNumberOfGuests() != null) p.setNumberOfGuests(d.getNumberOfGuests());
    if (d.getStatus() != null) p.setStatus(d.getStatus());
    if (d.getRoomId() != null) p.setRoomId(d.getRoomId());
  }

  private ReservationDTO dto(Reservation p) {
    ReservationDTO d = new ReservationDTO();
    d.setReservationId(p.getReservationId());
    d.setGuestName(p.getGuestName());
    d.setGuestEmail(p.getGuestEmail());
    d.setGuestPhone(p.getGuestPhone());
    d.setCheckInDate(p.getCheckInDate());
    d.setCheckOutDate(p.getCheckOutDate());
    d.setNumberOfGuests(p.getNumberOfGuests());
    d.setTotalAmount(p.getTotalAmount());
    d.setStatus(p.getStatus());
    d.setRoomId(p.getRoomId());
    return d;
  }

  private ReservationDetailDTO detail(Reservation p) {
    ReservationDetailDTO d = new ReservationDetailDTO();
    d.setReservationId(p.getReservationId());
    d.setGuestName(p.getGuestName());
    d.setGuestEmail(p.getGuestEmail());
    d.setGuestPhone(p.getGuestPhone());
    d.setCheckInDate(p.getCheckInDate());
    d.setCheckOutDate(p.getCheckOutDate());
    d.setNumberOfGuests(p.getNumberOfGuests());
    d.setTotalAmount(p.getTotalAmount());
    d.setStatus(p.getStatus());
    d.setRoom(room(p.getRoomId()));
    return d;
  }
}
