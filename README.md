# ahamo-dummy-demo2-backend-template

マイクロサービスバックエンドリポジトリのテンプレート

## 概要

このリポジトリは、ahamoダミーデモ2プロジェクトにおけるマイクロサービス開発用のテンプレートです。新しいバックエンドサービスを作成する際の基盤として使用できます。

## 特徴

- **Spring Boot 3.2.0** ベース
- **Java 17** 対応
- **PostgreSQL** データベース統合
- **Docker** コンテナ化サポート
- **GitHub Actions** CI/CD パイプライン
- **JaCoCo** テストカバレッジ
- **SonarQube** 静的解析対応
- **Flyway** データベースマイグレーション

## 使用方法

### 1. テンプレートからの新規サービス作成

1. このリポジトリをテンプレートとして新しいリポジトリを作成
2. 以下の設定を新しいサービス名に変更：

#### ファイル名の変更
- `TemplateApplication.java` → `{ServiceName}Application.java`
- パッケージ名: `com.ahamo.dummy.demo2.template` → `com.ahamo.dummy.demo2.{servicename}`

#### 設定ファイルの変更
- `settings.gradle`: `rootProject.name` を新しいサービス名に変更
- `build.gradle`: `sonar.projectKey` と `sonar.projectName` を変更
- `application.yml`: `spring.application.name` を変更
- `Dockerfile`: JAR ファイル名を変更
- `docker-compose.yml`: サービス名を変更

### 2. 環境変数設定

以下の環境変数を設定してください：

```bash
# データベース設定
export DB_NAME=your_database_name
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password

# サービス設定
export SERVICE_NAME=your-service-name

# JWT設定（認証が必要な場合）
export JWT_SECRET=your_jwt_secret_key
```

### 3. ローカル開発環境のセットアップ

```bash
# 依存関係のインストールとビルド
./gradlew build

# テストの実行
./gradlew test

# Docker Composeでの起動
docker-compose up -d
```

### 4. 開発時の注意事項

- [Javaコーディング規約](https://github.com/satoshi-watanabe-0001/ahamo-dummy-demo2-system-design-docs/blob/main/ai-context/coding-standards/java-coding-standards.md)に従って開発してください
- 単体テストのカバレッジは80%以上を維持してください
- コミット前に必ずテストが通ることを確認してください

## プロジェクト構造

```
src/
├── main/
│   ├── java/
│   │   └── com/ahamo/dummy/demo2/template/
│   │       ├── TemplateApplication.java
│   │       ├── config/
│   │       │   └── SecurityConfig.java
│   │       └── controller/
│   │           └── HealthController.java
│   └── resources/
│       ├── application.yml
│       ├── application-docker.yml
│       └── application-test.yml
└── test/
    └── java/
        └── com/ahamo/dummy/demo2/template/
            ├── TemplateApplicationTests.java
            └── controller/
                └── HealthControllerTest.java
```

## API エンドポイント

### ヘルスチェック
- `GET /api/v1/health` - サービスの稼働状況確認

## オプション機能

以下の機能は必要に応じてコメントアウトを解除して使用してください：

### JWT認証
- `build.gradle` の JWT 依存関係
- `application*.yml` の JWT 設定
- 認証関連のコンポーネント

### レート制限
- `application*.yml` のレート制限設定
- レート制限関連のコンポーネント

## CI/CD

GitHub Actions を使用したCI/CDパイプラインが設定されています：

- **テスト実行**: PostgreSQL サービスコンテナを使用
- **カバレッジレポート**: JaCoCo を使用
- **静的解析**: SonarQube 対応（設定が必要）

## トラブルシューティング

### よくある問題

1. **データベース接続エラー**
   - PostgreSQL が起動していることを確認
   - 環境変数が正しく設定されていることを確認

2. **ビルドエラー**
   - Java 17 が正しくインストールされていることを確認
   - `./gradlew clean build` でクリーンビルドを実行

3. **テスト失敗**
   - H2 データベースが正しく設定されていることを確認
   - テスト用の設定ファイルを確認

## 貢献

プルリクエストを作成する際は、以下を確認してください：

- [ ] コーディング規約に従っている
- [ ] 単体テストが作成されている
- [ ] 全てのテストが通っている
- [ ] カバレッジが80%以上である

## ライセンス

このプロジェクトは内部使用のためのテンプレートです。
