package fu.de190025.user.entity;
import jakarta.persistence.*;
@Entity @Table(name="users")
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="user_id", nullable=false)
 private Long userId;
 public Long getUserId() { return userId; }
 public void setUserId(Long value) { this.userId = value; }
 @Column(name="username", nullable=false, unique=true, length=30)
 private String username;
 public String getUsername() { return username; }
 public void setUsername(String value) { this.username = value; }
 @Column(name="email", nullable=false, length=100)
 private String email;
 public String getEmail() { return email; }
 public void setEmail(String value) { this.email = value; }
 @Enumerated(EnumType.STRING) @Column(name="role", nullable=false, length=15)
 private Role role;
 public Role getRole() { return role; }
 public void setRole(Role value) { this.role = value; }
 @Column(name="rate_per_post", nullable=false, precision=18, scale=2)
 private java.math.BigDecimal ratePerPost;
 public java.math.BigDecimal getRatePerPost() { return ratePerPost; }
 public void setRatePerPost(java.math.BigDecimal value) { this.ratePerPost = value; }
 @Column(name="max_daily_posts", nullable=false)
 private Integer maxDailyPosts;
 public Integer getMaxDailyPosts() { return maxDailyPosts; }
 public void setMaxDailyPosts(Integer value) { this.maxDailyPosts = value; }
 @Column(name="level", nullable=false)
 private Integer level;
 public Integer getLevel() { return level; }
 public void setLevel(Integer value) { this.level = value; }
 @Enumerated(EnumType.STRING) @Column(name="status", nullable=false, length=15)
 private UserStatus status;
 public UserStatus getStatus() { return status; }
 public void setStatus(UserStatus value) { this.status = value; }
}
