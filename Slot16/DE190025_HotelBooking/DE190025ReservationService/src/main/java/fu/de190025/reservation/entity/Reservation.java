package fu.de190025.reservation.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "reservations")
public class Reservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reservation_id", nullable = false)
  private Long reservationId;

  public Long getReservationId() {
    return reservationId;
  }

  public void setReservationId(Long value) {
    this.reservationId = value;
  }

  @Column(name = "guest_name", nullable = false, length = 100)
  private String guestName;

  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String value) {
    this.guestName = value;
  }

  @Column(name = "guest_email", nullable = false, length = 100)
  private String guestEmail;

  public String getGuestEmail() {
    return guestEmail;
  }

  public void setGuestEmail(String value) {
    this.guestEmail = value;
  }

  @Column(name = "guest_phone", nullable = false, length = 20)
  private String guestPhone;

  public String getGuestPhone() {
    return guestPhone;
  }

  public void setGuestPhone(String value) {
    this.guestPhone = value;
  }

  @Convert(converter = DateOnlyConverter.class)
  @Column(name = "check_in_date", nullable = false)
  private java.util.Date checkInDate;

  public java.util.Date getCheckInDate() {
    return checkInDate;
  }

  public void setCheckInDate(java.util.Date value) {
    this.checkInDate = value;
  }

  @Convert(converter = DateOnlyConverter.class)
  @Column(name = "check_out_date", nullable = false)
  private java.util.Date checkOutDate;

  public java.util.Date getCheckOutDate() {
    return checkOutDate;
  }

  public void setCheckOutDate(java.util.Date value) {
    this.checkOutDate = value;
  }

  @Column(name = "number_of_guests", nullable = false)
  private Integer numberOfGuests;

  public Integer getNumberOfGuests() {
    return numberOfGuests;
  }

  public void setNumberOfGuests(Integer value) {
    this.numberOfGuests = value;
  }

  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private java.math.BigDecimal totalAmount;

  public java.math.BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(java.math.BigDecimal value) {
    this.totalAmount = value;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 15)
  private ReservationStatus status;

  public ReservationStatus getStatus() {
    return status;
  }

  public void setStatus(ReservationStatus value) {
    this.status = value;
  }

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long value) {
    this.roomId = value;
  }
}
