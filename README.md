# metarayban-test

Meta Ray-Ban スマートグラス越しの視界を **Gemini** で解説してくれる Android アプリ。

グラスのカメラで撮った写真を Gemini 2.5 Flash に送り、「いま目の前に何が見えているか」を数秒で日本語テキストで返します。メニューを読む、看板を読む、目の前の物体の説明を聞く、といった視覚アシスタント用途の最小実装。

## 構成

| モジュール | 役割 |
|---|---|
| [Meta Wearables DAT SDK 0.6.0](https://wearables.developer.meta.com/docs/develop/) | Ray-Ban Meta グラスとの接続・映像ストリーミング・写真キャプチャ |
| [Gemini API](https://ai.google.dev/gemini-api/docs) (`gemini-2.5-flash`) | 撮影画像の視覚解析 → 自然言語で解説 |
| Jetpack Compose + Android | スマホ側 UI |

スマホ側アプリから DAT SDK でグラスに接続、ストリーミング中に Capture ボタンで写真を撮り、Describe ボタンで Gemini に解説させる、というフロー。

## セットアップ

### 1. 必須クレデンシャル

`app/local.properties` を作成し（`.gitignore` 対象）、以下を記述します:

```properties
# GitHub Packages から DAT SDK を取得するための PAT (classic, scope: read:packages)
# https://github.com/settings/tokens
github_token=ghp_xxxxxxxxxxxxxxxxxxxx

# Wearables Developer Center で登録したアプリの値
# https://wearables.developer.meta.com/
mwdat_application_id=xxxxxxxxxxxxxxxx
mwdat_client_token=AR|xxxxxxxxxxxxxxxx|xxxxxxxxxxxxxxxx

# Google AI Studio で発行した Gemini API キー
# https://aistudio.google.com/apikey
gemini_api_key=AIzaSyxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 2. ビルド

```bash
cd app
./gradlew assembleDebug
# APK: app/app/build/outputs/apk/debug/app-debug.apk
```

### 3. 実機で動かす

1. Meta AI アプリで **Developer Mode** を ON
2. このアプリをインストールして起動
3. **Connect my glasses** でグラスを接続
4. **Start streaming** でライブストリーム開始
5. カメラボタンで写真キャプチャ → ダイアログの **Describe scene** で Gemini が解説を返す

実機なしでも `MockDeviceKit` 画面からシミュレートできます（スマホ本体のカメラを入力にできる）。

## プロジェクト構成

```
metarayban-test/
├── app/                                    ← Android アプリ本体
│   ├── app/src/main/java/.../
│   │   ├── stream/           ← DAT ストリーミング + 写真キャプチャ
│   │   ├── gemini/           ← Gemini API クライアント
│   │   ├── ui/               ← Compose UI
│   │   ├── wearables/        ← デバイス接続状態管理
│   │   └── mockdevicekit/    ← 実機なしテスト用
│   └── build.gradle.kts
├── .claude/ .cursor/ .github/              ← AI 開発支援（DAT SDK 用スキル/ルール）
├── AGENTS.md                               ← AI エージェント向けコンテキスト
├── CHANGELOG.md                            ← DAT SDK 変更履歴（参考）
└── README.md
```

## 主要ファイル

| 用途 | ファイル |
|---|---|
| Gemini 呼び出し | [GeminiClient.kt](app/app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess/gemini/GeminiClient.kt) |
| ストリーミング + キャプチャ | [StreamViewModel.kt](app/app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess/stream/StreamViewModel.kt) |
| キャプチャ後ダイアログ | [SharePhotoDialog.kt](app/app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess/ui/SharePhotoDialog.kt) |
| API キー / 認証情報の注入 | [build.gradle.kts](app/app/build.gradle.kts), [settings.gradle.kts](app/settings.gradle.kts) |

## ベース

Meta 公式の [CameraAccess サンプル](https://github.com/facebook/meta-wearables-dat-android/tree/main/samples/CameraAccess) を出発点に、Gemini 統合とプロジェクト構成の整理を加えたもの。

## ライセンス

Meta 由来のコード: [LICENSE](LICENSE)（Meta Platforms, Inc.）
新規追加コード: 同じライセンスに従います。
