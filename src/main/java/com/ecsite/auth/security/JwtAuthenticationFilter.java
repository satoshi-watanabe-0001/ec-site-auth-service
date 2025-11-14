package com.ecsite.auth.security;

import com.ecsite.auth.entity.User;
import com.ecsite.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT認証フィルター
 *
 * <p>HTTPリクエストのAuthorizationヘッダーからJWTトークンを抽出し、検証します。 トークンが有効な場合、SecurityContextに認証情報を設定します。
 *
 * <p>退会処理中（PENDING_DELETION）または退会済み（DELETED）のユーザーのトークンは無効として扱います。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String token = authHeader.substring(7);
      Claims claims = jwtUtil.validateToken(token);

      String userId = claims.getSubject();
      String role = claims.get("role", String.class);

      if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        boolean isWithdrawalEndpoint = isWithdrawalEndpoint(request);

        User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (user == null) {
          if (isWithdrawalEndpoint) {
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug(
                "JWT authentication set for withdrawal endpoint (user not found, userId: {})",
                userId);
          } else {
            log.warn("JWT authentication failed: User not found (userId: {})", userId);
            filterChain.doFilter(request, response);
            return;
          }
        } else {
          if (!isWithdrawalEndpoint
              && (user.getStatus() == User.UserStatus.PENDING_DELETION
                  || user.getStatus() == User.UserStatus.DELETED)) {
            log.warn(
                "JWT authentication rejected: User is {} (userId: {})", user.getStatus(), userId);
            filterChain.doFilter(request, response);
            return;
          }

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  userId,
                  null,
                  Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);

          log.debug("JWT authentication successful for user: {}", userId);
        }
      }
    } catch (Exception e) {
      log.warn("JWT authentication failed: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }

  private boolean isWithdrawalEndpoint(HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();
    return "POST".equals(method)
        && (path.matches(".*/api/v1/users/[^/]+/withdraw")
            || path.matches(".*/api/v1/users/me/withdraw"));
  }
}
