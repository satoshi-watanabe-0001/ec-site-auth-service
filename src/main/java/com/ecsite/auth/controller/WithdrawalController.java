package com.ecsite.auth.controller;

import com.ecsite.auth.dto.WithdrawalRequest;
import com.ecsite.auth.dto.WithdrawalResponse;
import com.ecsite.auth.service.WithdrawalService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ユーザー退会処理コントローラー
 *
 * <p>ユーザーアカウントの退会処理APIを提供します。
 *
 * <p>エンドポイント:
 *
 * <ul>
 *   <li>POST /api/v1/users/me/withdraw - 自己退会（認証済みユーザー）
 *   <li>POST /api/v1/users/{id}/withdraw - 管理者退会（将来実装）
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class WithdrawalController {

  private final WithdrawalService withdrawalService;

  /**
   * EC-19: ユーザー退会処理API
   *
   * <p>指定されたユーザーIDのアカウントを退会処理します。 ステータスをPENDING_DELETIONに変更し、30日間の猶予期間を設けます。
   *
   * <p>レスポンス:
   *
   * <ul>
   *   <li>202 Accepted: 退会処理を受け付けました
   *   <li>400 Bad Request: バリデーションエラー
   *   <li>401 Unauthorized: 認証が必要
   *   <li>403 Forbidden: 自分自身のアカウントのみ退会可能
   *   <li>404 Not Found: ユーザーが存在しない
   *   <li>409 Conflict: ユーザーが既に退会処理中または退会済み
   * </ul>
   *
   * @param userId 退会処理対象のユーザーID
   * @param request 退会リクエスト（退会理由を含む）
   * @return HTTP 202と退会処理結果
   */
  @PostMapping("/{id}/withdraw")
  public ResponseEntity<WithdrawalResponse> withdrawUser(
      @PathVariable("id") UUID userId, @Valid @RequestBody WithdrawalRequest request) {
    log.info("Withdrawal request received for user ID: {}", userId);

    Authentication authentication =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    if (authentication != null && authentication.getPrincipal() != null) {
      String principalStr;
      Object principal = authentication.getPrincipal();
      if (principal instanceof String) {
        principalStr = (String) principal;
      } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
        principalStr =
            ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
      } else {
        principalStr = principal.toString();
      }

      try {
        UUID authenticatedUserId = UUID.fromString(principalStr);
        if (!userId.equals(authenticatedUserId)) {
          log.warn(
              "Authorization failed: User {} attempted to withdraw user {}",
              authenticatedUserId,
              userId);
          throw new AccessDeniedException("自分自身のアカウントのみ退会できます");
        }
      } catch (IllegalArgumentException e) {
        log.warn("Invalid UUID format in authentication principal: {}", principalStr);
      }
    }

    WithdrawalResponse response = withdrawalService.withdrawUser(userId, request);

    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
  }

  /**
   * IllegalArgumentException（ユーザー不存在）のエラーハンドラ
   *
   * @param ex IllegalArgumentException
   * @return HTTP 404とエラー詳細
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("User not found: {}", ex.getMessage());

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("status", "error");
    errorResponse.put("message", ex.getMessage());
    errorResponse.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  /**
   * IllegalStateException（既に退会処理中または退会済み）のエラーハンドラ
   *
   * @param ex IllegalStateException
   * @return HTTP 409とエラー詳細
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
    log.warn("Invalid state for withdrawal: {}", ex.getMessage());

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("status", "error");
    errorResponse.put("message", ex.getMessage());
    errorResponse.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /**
   * AccessDeniedException（認可失敗）のエラーハンドラ
   *
   * @param ex AccessDeniedException
   * @return HTTP 403とエラー詳細
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getMessage());

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("status", "error");
    errorResponse.put("message", ex.getMessage());
    errorResponse.put("timestamp", LocalDateTime.now());

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
  }
}
