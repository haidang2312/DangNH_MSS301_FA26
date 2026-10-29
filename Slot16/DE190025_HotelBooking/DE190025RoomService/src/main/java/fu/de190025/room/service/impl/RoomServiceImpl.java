package fu.de190025.room.service.impl;

import fu.de190025.room.common.*;
import fu.de190025.room.dto.*;
import fu.de190025.room.entity.*;
import fu.de190025.room.repository.RoomRepository;
import fu.de190025.room.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {
  private final RoomRepository repo;

  public RoomServiceImpl(RoomRepository repo) {
    this.repo = repo;
  }

  private Room find(Long id) {
    return repo.findById(id).orElseThrow(() -> new ApiException(4, "Room not found"));
  }

  public RoomDTO create(RoomDTO d) {
    Room u = new Room();
    apply(u, d);
    u.setStatus(RoomStatus.AVAILABLE);
    validate(u);
    duplicate(u);
    return dto(repo.saveAndFlush(u));
  }

  public RoomDTO update(Long id, RoomDTO d) {
    Room u = find(id);
    apply(u, d);
    validate(u);
    duplicate(u);
    return dto(repo.saveAndFlush(u));
  }

  public RoomDTO get(Long id) {
    return dto(find(id));
  }

  public void maintenance(Long id) {
    find(id).setStatus(RoomStatus.MAINTENANCE);
  }

  public PageDTO list(int page, int size, String roomType, String status) {
    var pageable = Validation.page(page, size);
    RoomType r = roomType == null ? null : RoomType.valueOf(roomType);
    RoomStatus s = status == null ? null : RoomStatus.valueOf(status);
    var result =
        repo.findAll(
            (root, q, cb) -> {
              var p = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
              if (r != null) p.add(cb.equal(root.get("roomType"), r));
              if (s != null) p.add(cb.equal(root.get("status"), s));
              return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
            },
            pageable);
    return PageDTO.of(result, result.getContent().stream().map(this::dto).toList());
  }

  private void duplicate(Room u) {
    if (repo.existsByRoomNumberAndRoomIdNot(
        u.getRoomNumber(), u.getRoomId() == null ? 0L : u.getRoomId()))
      throw new ApiException(3, "Duplicate roomNumber");
  }

  private void validate(Room u) {
    Validation.text(u.getRoomNumber(), 20, "roomNumber");
    Validation.require(u.getRoomType() != null, "roomType is required");
    Validation.require(
        u.getPricePerNight() != null
            && u.getPricePerNight().signum() > 0
            && u.getPricePerNight().scale() <= 2
            && u.getPricePerNight().precision() - u.getPricePerNight().scale() <= 16,
        "Invalid pricePerNight");
    Validation.require(
        u.getCapacity() != null && u.getCapacity() >= 1 && u.getCapacity() <= 10,
        "Invalid capacity");
    Validation.require(u.getFloor() != null && u.getFloor() >= 1, "Invalid floor");
  }

  private void apply(Room u, RoomDTO d) {
    if (d.getRoomNumber() != null) u.setRoomNumber(d.getRoomNumber());
    if (d.getRoomType() != null) u.setRoomType(d.getRoomType());
    if (d.getPricePerNight() != null) u.setPricePerNight(d.getPricePerNight());
    if (d.getCapacity() != null) u.setCapacity(d.getCapacity());
    if (d.getFloor() != null) u.setFloor(d.getFloor());
    if (d.getStatus() != null) u.setStatus(d.getStatus());
  }

  private RoomDTO dto(Room u) {
    RoomDTO d = new RoomDTO();
    d.setRoomId(u.getRoomId());
    d.setRoomNumber(u.getRoomNumber());
    d.setRoomType(u.getRoomType());
    d.setPricePerNight(u.getPricePerNight());
    d.setCapacity(u.getCapacity());
    d.setFloor(u.getFloor());
    d.setStatus(u.getStatus());
    return d;
  }
}
