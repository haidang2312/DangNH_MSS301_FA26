package fu.de190025.reservation.dto;

import fu.de190025.reservation.entity.RoomStatus;
import fu.de190025.reservation.entity.RoomType;

public class RoomDTO {

  private Long roomId;

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long value) {
    this.roomId = value;
  }

  private String roomNumber;

  public String getRoomNumber() {
    return roomNumber;
  }

  public void setRoomNumber(String value) {
    this.roomNumber = value;
  }

  private RoomType roomType;

  public RoomType getRoomType() {
    return roomType;
  }

  public void setRoomType(RoomType value) {
    this.roomType = value;
  }

  private java.math.BigDecimal pricePerNight;

  public java.math.BigDecimal getPricePerNight() {
    return pricePerNight;
  }

  public void setPricePerNight(java.math.BigDecimal value) {
    this.pricePerNight = value;
  }

  private Integer capacity;

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer value) {
    this.capacity = value;
  }

  private Integer floor;

  public Integer getFloor() {
    return floor;
  }

  public void setFloor(Integer value) {
    this.floor = value;
  }

  private RoomStatus status;

  public RoomStatus getStatus() {
    return status;
  }

  public void setStatus(RoomStatus value) {
    this.status = value;
  }
}
