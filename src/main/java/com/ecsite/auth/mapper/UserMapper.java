package com.ecsite.auth.mapper;

import com.ecsite.auth.dto.UserResponse;
import com.ecsite.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserResponse toUserResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .status(user.getStatus())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .emailVerified(user.isEmailVerified())
        .build();
  }
}
