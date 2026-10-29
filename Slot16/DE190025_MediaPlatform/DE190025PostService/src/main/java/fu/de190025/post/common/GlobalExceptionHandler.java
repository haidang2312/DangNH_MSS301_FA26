package fu.de190025.post.common;

import fu.de190025.post.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse> domain(ApiException e) {
    return ResponseEntity.status(
            switch (e.getStatus()) {
              case 2 -> 406;
              case 3 -> 226;
              case 4 -> 404;
              case 5 -> 400;
              default -> 500;
            })
        .body(ApiResponse.error(e.getStatus(), e.getMessage()));
  }

  @ExceptionHandler({
    org.springframework.web.bind.MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<ApiResponse> invalid(Exception e) {
    return ResponseEntity.status(406).body(ApiResponse.error(2, "Invalid request"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse> unexpected(Exception e) {
    return ResponseEntity.status(500).body(ApiResponse.error(0, "Server error"));
  }
}
