package fu.de190025.post.dto;


public class PostDTO {
 
 private Long postId;
 public Long getPostId() { return postId; }
 public void setPostId(Long value) { this.postId=value; }
 
 private String advertiserName;
 public String getAdvertiserName() { return advertiserName; }
 public void setAdvertiserName(String value) { this.advertiserName=value; }
 
 private String advertiserEmail;
 public String getAdvertiserEmail() { return advertiserEmail; }
 public void setAdvertiserEmail(String value) { this.advertiserEmail=value; }
 
 private String advertiserPhone;
 public String getAdvertiserPhone() { return advertiserPhone; }
 public void setAdvertiserPhone(String value) { this.advertiserPhone=value; }
 @com.fasterxml.jackson.annotation.JsonFormat(pattern="dd/MM/yyyy", timezone="UTC", lenient=com.fasterxml.jackson.annotation.OptBoolean.FALSE)
 private java.util.Date startDate;
 public java.util.Date getStartDate() { return startDate; }
 public void setStartDate(java.util.Date value) { this.startDate=value; }
 @com.fasterxml.jackson.annotation.JsonFormat(pattern="dd/MM/yyyy", timezone="UTC", lenient=com.fasterxml.jackson.annotation.OptBoolean.FALSE)
 private java.util.Date endDate;
 public java.util.Date getEndDate() { return endDate; }
 public void setEndDate(java.util.Date value) { this.endDate=value; }
 
 private Integer numberOfPosts;
 public Integer getNumberOfPosts() { return numberOfPosts; }
 public void setNumberOfPosts(Integer value) { this.numberOfPosts=value; }
 
 private java.math.BigDecimal totalCost;
 public java.math.BigDecimal getTotalCost() { return totalCost; }
 public void setTotalCost(java.math.BigDecimal value) { this.totalCost=value; }
 
 private String status;
 public String getStatus() { return status; }
 public void setStatus(String value) { this.status=value; }
 
 private Long userId;
 public Long getUserId() { return userId; }
 public void setUserId(Long value) { this.userId=value; }
}
