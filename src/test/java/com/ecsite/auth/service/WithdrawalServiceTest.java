package com.ecsite.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecsite.auth.dto.WithdrawalRequest;
import com.ecsite.auth.dto.WithdrawalResponse;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * WithdrawalServiceのユニットテスト
 *
 * <p>ユーザー退会処理サービスの各メソッドをテストします。
 */
@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private NotificationService notificationService;

  @InjectMocks private WithdrawalService withdrawalService;

  private UUID userId;
  private User activeUser;
  private WithdrawalRequest withdrawalRequest;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();

    activeUser = new User();
    activeUser.setId(userId);
    activeUser.setEmail("test@example.com");
    activeUser.setPasswordHash("$2a$12$hashedPassword");
    activeUser.setFirstName("John");
    activeUser.setLastName("Doe");
    activeUser.setStatus(User.UserStatus.ACTIVE);
    activeUser.setCreatedAt(LocalDateTime.now());
    activeUser.setUpdatedAt(LocalDateTime.now());

    withdrawalRequest = new WithdrawalRequest();
    withdrawalRequest.setReason("サービスを利用しなくなったため");
  }

  @Test
  void withdrawUser_Success() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
    when(userRepository.save(any(User.class))).thenReturn(activeUser);

    WithdrawalResponse response = withdrawalService.withdrawUser(userId, withdrawalRequest);

    assertNotNull(response);
    assertEquals("success", response.getStatus());
    assertEquals("退会処理を受け付けました", response.getMessage());
    assertNotNull(response.getData());
    assertEquals(userId, response.getData().getUserId());
    assertEquals("PENDING_DELETION", response.getData().getUserStatus());
    assertNotNull(response.getData().getScheduledDeletionAt());
    assertEquals(30, response.getData().getGracePeriodDays());

    verify(userRepository).findById(userId);
    verify(userRepository)
        .save(
            argThat(
                user ->
                    user.getStatus() == User.UserStatus.PENDING_DELETION
                        && user.getDeletionScheduledAt() != null
                        && user.getWithdrawalReason().equals("サービスを利用しなくなったため")));
    verify(notificationService).sendWithdrawalConfirmation(anyString(), any(LocalDateTime.class));
  }

  @Test
  void withdrawUser_UserNotFound_ThrowsException() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> withdrawalService.withdrawUser(userId, withdrawalRequest));

    assertEquals("User not found with ID: " + userId, exception.getMessage());

    verify(userRepository).findById(userId);
    verify(userRepository, never()).save(any(User.class));
    verify(notificationService, never())
        .sendWithdrawalConfirmation(anyString(), any(LocalDateTime.class));
  }

  @Test
  void withdrawUser_AlreadyPendingDeletion_ThrowsException() {
    activeUser.setStatus(User.UserStatus.PENDING_DELETION);
    when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> withdrawalService.withdrawUser(userId, withdrawalRequest));

    assertEquals("User is already pending deletion", exception.getMessage());

    verify(userRepository).findById(userId);
    verify(userRepository, never()).save(any(User.class));
    verify(notificationService, never())
        .sendWithdrawalConfirmation(anyString(), any(LocalDateTime.class));
  }

  @Test
  void withdrawUser_AlreadyDeleted_ThrowsException() {
    activeUser.setStatus(User.UserStatus.DELETED);
    when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> withdrawalService.withdrawUser(userId, withdrawalRequest));

    assertEquals("User is already deleted", exception.getMessage());

    verify(userRepository).findById(userId);
    verify(userRepository, never()).save(any(User.class));
    verify(notificationService, never())
        .sendWithdrawalConfirmation(anyString(), any(LocalDateTime.class));
  }

  @Test
  void withdrawUser_WithoutReason_Success() {
    WithdrawalRequest requestWithoutReason = new WithdrawalRequest();
    requestWithoutReason.setReason(null);

    when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
    when(userRepository.save(any(User.class))).thenReturn(activeUser);

    WithdrawalResponse response = withdrawalService.withdrawUser(userId, requestWithoutReason);

    assertNotNull(response);
    assertEquals("success", response.getStatus());

    verify(userRepository)
        .save(
            argThat(
                user ->
                    user.getStatus() == User.UserStatus.PENDING_DELETION
                        && user.getWithdrawalReason() == null));
  }

  @Test
  void withdrawUser_SetsCorrectDeletionSchedule() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
    when(userRepository.save(any(User.class))).thenReturn(activeUser);

    LocalDateTime beforeCall = LocalDateTime.now().plusDays(30);
    WithdrawalResponse response = withdrawalService.withdrawUser(userId, withdrawalRequest);
    LocalDateTime afterCall = LocalDateTime.now().plusDays(30);

    assertNotNull(response.getData().getScheduledDeletionAt());
    LocalDateTime scheduledAt = response.getData().getScheduledDeletionAt();

    assert scheduledAt.isAfter(beforeCall.minusMinutes(1));
    assert scheduledAt.isBefore(afterCall.plusMinutes(1));
  }

  @Test
  void withdrawUser_InactiveUser_Success() {
    activeUser.setStatus(User.UserStatus.INACTIVE);
    when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
    when(userRepository.save(any(User.class))).thenReturn(activeUser);

    WithdrawalResponse response = withdrawalService.withdrawUser(userId, withdrawalRequest);

    assertNotNull(response);
    assertEquals("success", response.getStatus());

    verify(userRepository)
        .save(argThat(user -> user.getStatus() == User.UserStatus.PENDING_DELETION));
  }

  @Test
  void withdrawUser_SuspendedUser_Success() {
    activeUser.setStatus(User.UserStatus.SUSPENDED);
    when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
    when(userRepository.save(any(User.class))).thenReturn(activeUser);

    WithdrawalResponse response = withdrawalService.withdrawUser(userId, withdrawalRequest);

    assertNotNull(response);
    assertEquals("success", response.getStatus());

    verify(userRepository)
        .save(argThat(user -> user.getStatus() == User.UserStatus.PENDING_DELETION));
  }
}
