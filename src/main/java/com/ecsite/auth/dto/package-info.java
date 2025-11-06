/**
 * 認証サービスのデータ転送オブジェクト（DTO）パッケージ
 *
 * <p>このパッケージは、認証APIのリクエスト/レスポンスを表現するDTOクラスを含む。 クライアントアプリケーションとの通信に使用されるデータ構造を定義する。
 *
 * <p>主要なDTOクラス:
 *
 * <ul>
 *   <li>{@link com.ecsite.auth.dto.LoginRequest} - ログインリクエスト
 *   <li>{@link com.ecsite.auth.dto.LoginResponse} - ログインレスポンス（JWTトークン含む）
 *   <li>{@link com.ecsite.auth.dto.CreateUserRequest} - ユーザー登録リクエスト
 *   <li>{@link com.ecsite.auth.dto.RegistrationResponse} - 登録レスポンス
 * </ul>
 *
 * @since 1.0
 */
package com.ecsite.auth.dto;
