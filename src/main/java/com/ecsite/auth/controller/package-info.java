/**
 * 認証サービスのRESTコントローラーパッケージ
 *
 * <p>このパッケージは、認証APIのHTTPエンドポイントを提供するコントローラークラスを含む。 クライアントアプリケーションからのHTTPリクエストを受け付け、サービス層に処理を委譲する。
 *
 * <p>主要なコントローラークラス:
 *
 * <ul>
 *   <li>{@link com.ecsite.auth.controller.AuthController} - 認証関連エンドポイント（登録、ログイン、メール認証）
 * </ul>
 *
 * <p>提供するエンドポイント:
 *
 * <ul>
 *   <li>POST /api/v1/auth/register - ユーザー登録
 *   <li>POST /api/v1/auth/login - ログイン
 *   <li>POST /api/v1/auth/verify-email - メール認証
 * </ul>
 *
 * @since 1.0
 */
package com.ecsite.auth.controller;
