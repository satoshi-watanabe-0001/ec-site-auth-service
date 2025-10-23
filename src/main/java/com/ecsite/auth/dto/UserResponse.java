package com.ecsite.auth.dto;

import com.ecsite.auth.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

  private UUID id;
  private String email;
  private String firstName;
  private String lastName;
  private User.UserStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private boolean emailVerified;
}
