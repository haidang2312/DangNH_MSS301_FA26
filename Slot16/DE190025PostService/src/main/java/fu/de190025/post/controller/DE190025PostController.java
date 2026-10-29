package fu.de190025.post.controller;

import fu.de190025.post.dto.*;
import fu.de190025.post.service.PostService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestController @RequestMapping(value="/api/posts",produces="application/json")
public class DE190025PostController {
 private final PostService service;
 public DE190025PostController(PostService service) { this.service=service; }
 @PostMapping public ResponseEntity<ApiResponse> create(@RequestBody PostDTO d) { return ResponseEntity.status(201).body(ApiResponse.ok(service.create(d))); }
 @PutMapping("/{id}") public ApiResponse update(@PathVariable Long id,@RequestBody PostDTO d) { return ApiResponse.ok(service.update(id,d)); }
 @GetMapping("/{id}") public ApiResponse get(@PathVariable Long id) { return ApiResponse.ok(service.get(id)); }
 @DeleteMapping("/{id}") public ApiResponse delete(@PathVariable Long id) { service.cancel(id); return ApiResponse.ok(null); }
 @GetMapping public ApiResponse list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(required=false) String advertiserName,@RequestParam(required=false) String status) { return ApiResponse.ok(service.list(page,size,advertiserName,status)); }
}