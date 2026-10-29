package fu.de190025.post.dto;

public record ApiResponse(int status, String message, Object data, String timestamp) {
  public static ApiResponse ok(Object data) {
    return new ApiResponse(1, "Successful", data, java.time.Instant.now().toString());
  }

  public static ApiResponse error(int status, String message) {
    return new ApiResponse(status, message, null, java.time.Instant.now().toString());
  }
}
