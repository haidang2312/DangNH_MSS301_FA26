package com.mss301.account.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder
public class LoginResponse {
 private String accessToken;
 private String refreshToken;
 private String tokenType;
 private Integer role;
 private String username;
}
