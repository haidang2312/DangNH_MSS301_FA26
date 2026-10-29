package fu.de190025.reservation.common;

import fu.de190025.reservation.dto.ApiResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponseDTO> domain(ApiException e) {
    return ResponseEntity.status(
            switch (e.getStatus()) {
              case 2 -> 406;
              case 3 -> 226;
              case 4 -> 404;
              case 5 -> 400;
              default -> 500;
            })
        .body(ApiResponseDTO.error(e.getStatus(), e.getMessage()));
  }

  @ExceptionHandler({
    org.springframework.web.bind.MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<ApiResponseDTO> invalid(Exception e) {
    return ResponseEntity.status(406).body(ApiResponseDTO.error(2, "Invalid request"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponseDTO> unexpected(Exception e) {
    return ResponseEntity.status(500).body(ApiResponseDTO.error(0, "Server error"));
  }
}
