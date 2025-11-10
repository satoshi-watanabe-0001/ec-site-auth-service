package com.ecsite.auth.service;

import com.ecsite.auth.dto.WithdrawalRequest;
import com.ecsite.auth.dto.WithdrawalResponse;
import com.ecsite.auth.entity.User;
import com.ecsite.auth.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ユーザー退会処理サービス
 *
 * <p>ユーザーアカウントの退会処理を管理します。 多段階削除プロセスを実装し、猶予期間を設けることで誤操作からの復旧を可能にします。
 *
 * <p>退会処理フロー:
 *
 * <ol>
 *   <li>Phase 1（即時）: ステータスをPENDING_DELETIONに変更、削除予定日時を設定
 *   <li>Phase 2（猶予期間）: 30日間の猶予期間、ユーザーはログイン不可
 *   <li>Phase 3（最終処理）: ステータスをDELETEDに変更、個人情報を匿名化（将来実装）
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

  private final UserRepository userRepository;
  private final NotificationService notificationService;

  private static final int GRACE_PERIOD_DAYS = 30;

  /**
   * ユーザー退会処理を実行します
   *
   * <p>指定されたユーザーIDのアカウントを退会処理します。 ステータスをPENDING_DELETIONに変更し、削除予定日時を設定します。
   *
   * @param userId 退会処理対象のユーザーID
   * @param request 退会リクエスト（退会理由を含む）
   * @return 退会処理結果レスポンス
   * @throws IllegalArgumentException ユーザーが存在しない場合
   * @throws IllegalStateException ユーザーが既に退会処理中または退会済みの場合
   */
  @Transactional
  public WithdrawalResponse withdrawUser(UUID userId, WithdrawalRequest request) {
    log.info("Starting withdrawal process for user ID: {}", userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

    if (user.getStatus() == User.UserStatus.PENDING_DELETION) {
      log.warn("User {} is already pending deletion", userId);
      throw new IllegalStateException("User is already pending deletion");
    }

    if (user.getStatus() == User.UserStatus.DELETED) {
      log.warn("User {} is already deleted", userId);
      throw new IllegalStateException("User is already deleted");
    }

    LocalDateTime scheduledDeletionAt = LocalDateTime.now().plusDays(GRACE_PERIOD_DAYS);

    user.setStatus(User.UserStatus.PENDING_DELETION);
    user.setDeletionScheduledAt(scheduledDeletionAt);
    user.setWithdrawalReason(request.getReason());

    User updatedUser = userRepository.save(user);
    log.info(
        "User {} status updated to PENDING_DELETION, scheduled for deletion at: {}",
        userId,
        scheduledDeletionAt);

    sendWithdrawalNotification(updatedUser);

    return buildWithdrawalResponse(updatedUser);
  }

  /**
   * 退会確認通知を送信します
   *
   * @param user 退会処理対象のユーザー
   */
  private void sendWithdrawalNotification(User user) {
    try {
      notificationService.sendWithdrawalConfirmation(
          user.getEmail(), user.getDeletionScheduledAt());
      log.info("Withdrawal confirmation email sent to user: {}", user.getId());
    } catch (Exception e) {
      log.error("Failed to send withdrawal confirmation email to user: {}", user.getId(), e);
    }
  }

  /**
   * 退会処理レスポンスを構築します
   *
   * @param user 退会処理されたユーザー
   * @return 退会処理レスポンス
   */
  private WithdrawalResponse buildWithdrawalResponse(User user) {
    WithdrawalResponse.WithdrawalData data =
        WithdrawalResponse.WithdrawalData.builder()
            .userId(user.getId())
            .userStatus(user.getStatus().name())
            .scheduledDeletionAt(user.getDeletionScheduledAt())
            .gracePeriodDays(GRACE_PERIOD_DAYS)
            .build();

    return WithdrawalResponse.builder()
        .status("success")
        .message("退会処理を受け付けました")
        .data(data)
        .build();
  }
}
