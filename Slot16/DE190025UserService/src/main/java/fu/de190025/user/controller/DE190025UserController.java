package fu.de190025.user.controller;
import fu.de190025.user.dto.*;
import fu.de190025.user.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestController @RequestMapping(value="/api/users",produces="application/json")
public class DE190025UserController {
 private final UserService service;
 public DE190025UserController(UserService service) { this.service=service; }
 @PostMapping public ResponseEntity<ApiResponse> create(@RequestBody UserDTO d) { return ResponseEntity.status(201).body(ApiResponse.ok(service.create(d))); }
 @PutMapping("/{id}") public ApiResponse update(@PathVariable Long id,@RequestBody UserDTO d) { return ApiResponse.ok(service.update(id,d)); }
 @GetMapping("/{id}") public ApiResponse get(@PathVariable Long id) { return ApiResponse.ok(service.get(id)); }
 @DeleteMapping("/{id}") public ApiResponse delete(@PathVariable Long id) { service.deactivate(id); return ApiResponse.ok(null); }
 @GetMapping public ApiResponse list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(required=false) String role,@RequestParam(required=false) String status) { return ApiResponse.ok(service.list(page,size,role,status)); }
}