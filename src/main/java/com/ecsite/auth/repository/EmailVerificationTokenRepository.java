package com.ecsite.auth.repository;

import com.ecsite.auth.entity.EmailVerificationToken;
import com.ecsite.auth.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * メール認証トークンリポジトリ
 *
 * <p>メール認証トークンの永続化とクエリを提供します。
 */
@Repository
public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, UUID> {

  /**
   * トークン文字列からメール認証トークンを検索します。
   *
   * @param token トークン文字列
   * @return メール認証トークン（存在する場合）
   */
  Optional<EmailVerificationToken> findByToken(String token);

  /**
   * ユーザーに紐づく最新のメール認証トークンを検索します。
   *
   * @param user ユーザー
   * @return メール認証トークン（存在する場合）
   */
  Optional<EmailVerificationToken> findFirstByUserOrderByCreatedAtDesc(User user);
}
