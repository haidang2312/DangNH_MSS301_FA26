package fu.de190025.reservation.dto;

import fu.de190025.reservation.entity.ReservationStatus;

public class ReservationDetailDTO {

  private Long reservationId;

  public Long getReservationId() {
    return reservationId;
  }

  public void setReservationId(Long value) {
    this.reservationId = value;
  }

  private String guestName;

  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String value) {
    this.guestName = value;
  }

  private String guestEmail;

  public String getGuestEmail() {
    return guestEmail;
  }

  public void setGuestEmail(String value) {
    this.guestEmail = value;
  }

  private String guestPhone;

  public String getGuestPhone() {
    return guestPhone;
  }

  public void setGuestPhone(String value) {
    this.guestPhone = value;
  }

  @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC")
  private java.util.Date checkInDate;

  public java.util.Date getCheckInDate() {
    return checkInDate;
  }

  public void setCheckInDate(java.util.Date value) {
    this.checkInDate = value;
  }

  @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy", timezone = "UTC")
  private java.util.Date checkOutDate;

  public java.util.Date getCheckOutDate() {
    return checkOutDate;
  }

  public void setCheckOutDate(java.util.Date value) {
    this.checkOutDate = value;
  }

  private Integer numberOfGuests;

  public Integer getNumberOfGuests() {
    return numberOfGuests;
  }

  public void setNumberOfGuests(Integer value) {
    this.numberOfGuests = value;
  }

  private java.math.BigDecimal totalAmount;

  public java.math.BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(java.math.BigDecimal value) {
    this.totalAmount = value;
  }

  private ReservationStatus status;

  public ReservationStatus getStatus() {
    return status;
  }

  public void setStatus(ReservationStatus value) {
    this.status = value;
  }

  private RoomDTO room;

  public RoomDTO getRoom() {
    return room;
  }

  public void setRoom(RoomDTO value) {
    this.room = value;
  }
}
