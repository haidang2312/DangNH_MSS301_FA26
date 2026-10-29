package fu.de190025.post.client;

import fu.de190025.post.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserClient {
  record UserResponse(int status, String message, UserDTO data, String timestamp) {}

  @GetMapping("/api/users/{id}")
  UserResponse getUser(@PathVariable("id") Long id);
}
