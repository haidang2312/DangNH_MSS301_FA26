package fu.de190025.post.entity;
import jakarta.persistence.*;
@Entity @Table(name="posts")
public class Post {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="post_id",nullable=false)
 private Long postId;
 public Long getPostId() { return postId; }
 public void setPostId(Long value) { this.postId=value; }
 @Column(name="advertiser_name",nullable=false, length=100)
 private String advertiserName;
 public String getAdvertiserName() { return advertiserName; }
 public void setAdvertiserName(String value) { this.advertiserName=value; }
 @Column(name="advertiser_email",nullable=false, length=100)
 private String advertiserEmail;
 public String getAdvertiserEmail() { return advertiserEmail; }
 public void setAdvertiserEmail(String value) { this.advertiserEmail=value; }
 @Column(name="advertiser_phone",nullable=false, length=20)
 private String advertiserPhone;
 public String getAdvertiserPhone() { return advertiserPhone; }
 public void setAdvertiserPhone(String value) { this.advertiserPhone=value; }
 @Convert(converter=DateOnlyConverter.class) @Column(name="start_date",nullable=false)
 private java.util.Date startDate;
 public java.util.Date getStartDate() { return startDate; }
 public void setStartDate(java.util.Date value) { this.startDate=value; }
 @Convert(converter=DateOnlyConverter.class) @Column(name="end_date",nullable=false)
 private java.util.Date endDate;
 public java.util.Date getEndDate() { return endDate; }
 public void setEndDate(java.util.Date value) { this.endDate=value; }
 @Column(name="number_of_posts",nullable=false)
 private Integer numberOfPosts;
 public Integer getNumberOfPosts() { return numberOfPosts; }
 public void setNumberOfPosts(Integer value) { this.numberOfPosts=value; }
 @Column(name="total_cost",nullable=false, precision=18, scale=2)
 private java.math.BigDecimal totalCost;
 public java.math.BigDecimal getTotalCost() { return totalCost; }
 public void setTotalCost(java.math.BigDecimal value) { this.totalCost=value; }
 @Enumerated(EnumType.STRING) @Column(name="status",nullable=false, length=15)
 private PostStatus status;
 public PostStatus getStatus() { return status; }
 public void setStatus(PostStatus value) { this.status=value; }
 @Column(name="user_id",nullable=false)
 private Long userId;
 public Long getUserId() { return userId; }
 public void setUserId(Long value) { this.userId=value; }
}
