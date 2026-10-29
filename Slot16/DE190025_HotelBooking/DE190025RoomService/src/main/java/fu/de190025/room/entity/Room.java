package fu.de190025.room.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "room_id", nullable = false)
  private Long roomId;

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long value) {
    this.roomId = value;
  }

  @Column(name = "room_number", nullable = false, unique = true, length = 20)
  private String roomNumber;

  public String getRoomNumber() {
    return roomNumber;
  }

  public void setRoomNumber(String value) {
    this.roomNumber = value;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "room_type", nullable = false, length = 10)
  private RoomType roomType;

  public RoomType getRoomType() {
    return roomType;
  }

  public void setRoomType(RoomType value) {
    this.roomType = value;
  }

  @Column(name = "price_per_night", nullable = false, precision = 18, scale = 2)
  private java.math.BigDecimal pricePerNight;

  public java.math.BigDecimal getPricePerNight() {
    return pricePerNight;
  }

  public void setPricePerNight(java.math.BigDecimal value) {
    this.pricePerNight = value;
  }

  @Column(name = "capacity", nullable = false)
  private Integer capacity;

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer value) {
    this.capacity = value;
  }

  @Column(name = "floor", nullable = false)
  private Integer floor;

  public Integer getFloor() {
    return floor;
  }

  public void setFloor(Integer value) {
    this.floor = value;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 15)
  private RoomStatus status;

  public RoomStatus getStatus() {
    return status;
  }

  public void setStatus(RoomStatus value) {
    this.status = value;
  }
}
