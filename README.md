# md_memo_app

# 要件定義書

## 1. アプリ概要
個人利用を想定したAndroid向けマークダウンメモアプリ

## 2. 主要機能
1. ディレクトリ選択機能
2. マークダウンファイル一覧表示
3. 新規マークダウンファイル作成機能
4. テキスト入力と投稿機能
5. ファイルへの追記機能

## 3. 詳細要件
### 3.1 ディレクトリ選択
- ユーザーが任意のディレクトリを選択可能
- 選択したディレクトリのURIを保存し、次回起動時に再利用

### 3.2 マークダウンファイル一覧表示
- 選択したディレクトリ内のマークダウンファイルをリスト表示
- ファイル名とアイコンを表示

### 3.3 新規マークダウンファイル作成
- ユーザーがファイル名を入力して新規ファイルを作成可能
- 作成したファイルを即座に一覧に追加

### 3.4 テキスト入力と投稿
- テキスト入力エリアを提供
- 「投稿」ボタンタップで入力内容を現在選択中のファイルに追記

### 3.5 ファイルへの追記
- 投稿内容を '- yyyy/MM/dd/HHmm/ss {投稿内容}' の形式で追記
- 追記は選択したファイルの末尾に行う

## 4. 非機能要件
- Android 8.0以上のデバイスをサポート
- オフライン動作可能
- ダークモード対応

## 5. 使用技術
- 開発言語: Kotlin
- アーキテクチャ: MVVM
- 主要ライブラリ:
  - Android Jetpack (ViewModel, LiveData)
  - Coroutines
  - Storage Access Framework

---

# 開発階層

## 1. プロジェクト構造
```
app-name/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/yourcompany/appname/
│   │   │   │   ├── data/
│   │   │   │   ├── ui/
│   │   │   │   ├── utils/
│   │   │   │   └── viewmodels/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle
├── gradle/
├── build.gradle
└── settings.gradle
```

## 2. パッケージ構成
### 2.1 data/
- repository/
  - FileRepository.kt
- models/
  - MarkdownFile.kt

### 2.2 ui/
- main/
  - MainActivity.kt
- filelist/
  - FileListFragment.kt
  - FileListAdapter.kt
- editor/
  - EditorFragment.kt

### 2.3 utils/
- FileUtils.kt
- DateUtils.kt

### 2.4 viewmodels/
- MainViewModel.kt
- FileListViewModel.kt
- EditorViewModel.kt

## 3. 主要クラスの役割
### 3.1 FileRepository
- ファイルシステムとの相互作用を管理
- ファイルの読み書き、一覧取得を担当

### 3.2 MainActivity
- アプリのメインエントリーポイント
- ナビゲーション管理

### 3.3 FileListFragment
- マークダウンファイルの一覧表示
- 新規ファイル作成機能の提供

### 3.4 EditorFragment
- テキスト入力と投稿機能の提供
- 選択されたファイルへの追記機能

### 3.5 MainViewModel
- アプリ全体の状態管理
- ディレクトリ選択の処理

### 3.6 FileListViewModel
- ファイル一覧の状態管理
- 新規ファイル作成のロジック

### 3.7 EditorViewModel
- テキスト入力と投稿のロジック管理
- ファイル追記処理の制御
