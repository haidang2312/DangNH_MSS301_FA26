package fu.de190025.reservation.dto;

public record ApiResponseDTO(int status, String message, Object data, String timestamp) {
  public static ApiResponseDTO ok(Object data) {
    return new ApiResponseDTO(1, "Successful", data, java.time.Instant.now().toString());
  }

  public static ApiResponseDTO error(int status, String message) {
    return new ApiResponseDTO(status, message, null, java.time.Instant.now().toString());
  }
}
