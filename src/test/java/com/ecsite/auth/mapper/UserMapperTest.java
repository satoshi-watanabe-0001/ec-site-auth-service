package com.ecsite.auth.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecsite.auth.dto.UserResponse;
import com.ecsite.auth.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserMapperTest {

  private UserMapper userMapper;
  private User user;

  @BeforeEach
  void setUp() {
    userMapper = new UserMapper();

    user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail("test@example.com");
    user.setPasswordHash("$2a$12$hashedPassword");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setStatus(User.UserStatus.PENDING);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    user.setEmailVerifiedAt(null);
  }

  @Test
  void toUserResponse_Success() {
    UserResponse response = userMapper.toUserResponse(user);

    assertNotNull(response);
    assertEquals(user.getId(), response.getId());
    assertEquals(user.getEmail(), response.getEmail());
    assertEquals(user.getFirstName(), response.getFirstName());
    assertEquals(user.getLastName(), response.getLastName());
    assertEquals(user.getStatus(), response.getStatus());
    assertEquals(user.getCreatedAt(), response.getCreatedAt());
    assertEquals(user.getUpdatedAt(), response.getUpdatedAt());
    assertFalse(response.isEmailVerified());
  }

  @Test
  void toUserResponse_WithVerifiedEmail() {
    user.setEmailVerifiedAt(LocalDateTime.now());

    UserResponse response = userMapper.toUserResponse(user);

    assertNotNull(response);
    assertTrue(response.isEmailVerified());
  }
}
