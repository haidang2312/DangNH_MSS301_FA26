package fu.de190025.post.dto;



public class UserDTO {
 
 private Long userId;
 public Long getUserId() { return userId; }
 public void setUserId(Long value) { this.userId = value; }
 
 private String username;
 public String getUsername() { return username; }
 public void setUsername(String value) { this.username = value; }
 
 private String email;
 public String getEmail() { return email; }
 public void setEmail(String value) { this.email = value; }
 
 private String role;
 public String getRole() { return role; }
 public void setRole(String value) { this.role = value; }
 
 private java.math.BigDecimal ratePerPost;
 public java.math.BigDecimal getRatePerPost() { return ratePerPost; }
 public void setRatePerPost(java.math.BigDecimal value) { this.ratePerPost = value; }
 
 private Integer maxDailyPosts;
 public Integer getMaxDailyPosts() { return maxDailyPosts; }
 public void setMaxDailyPosts(Integer value) { this.maxDailyPosts = value; }
 
 private Integer level;
 public Integer getLevel() { return level; }
 public void setLevel(Integer value) { this.level = value; }
 
 private String status;
 public String getStatus() { return status; }
 public void setStatus(String value) { this.status = value; }
}
