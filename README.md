# SkipTracksWithVolumeKey

## 概要
- 画面がオフのときにボリュームキーの長押しで曲飛ばしをするシンプルなXposedモジュールです
- XperiaのStock13用に作りましたが、GravityBox/AOSPModsの同等機能が機能する環境なら基本的に動作する（と思います,未確認）
- API Levelが20より低い環境では動作しません（インストールは出来てしまう）

## 動作確認
- SONY Xperia 10IV(XQ-CC44) (@r-ca)
  - Android 13
  - Security patch version: 2023/06/01
  - Build number: 65.1.A.7.67 release-keys

## 既知の問題
- 必要クラスの検索に失敗する場合がある（起動時に何度かSystem Frameworkが読み込まれるようで、その場合に失敗する...？）
 - 最低限のハンドリングはしているので動作に問題はない(はず)です

## その他
- 自身のデバイスで動作したら教えていただけるとありがたいです！
