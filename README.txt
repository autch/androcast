
Androcast
=========

Swing で書かれた、Android 用スクリーンモニタです。

Android ソースツリーの、
development/tools/screenshot/src/com/android/screenshot/Screenshot.java
からのパクリです（ぉ

Linux で画面モニタをしたくて自分で使うために作ったのでかなり荒削りです。
とくにエラー処理とか（ぉ


機能
----
・デバイスもしくはエミュレータを選択してモニタ開始
・ポートレート／ランドスケープ切り替え
・PNG 形式でのキャプチャ
・50% 〜 200% のズーム
・ツールバーを切り離す


必要なもの
----------

・JRE6（javax.swing.SwingWorker を使っています）
・Android SDK 1.5r2 以降

r13 から ddmlib.jar には依存しなくなりました。androcast.jar だけで動きます。

adb へのパスを通しておきます。もしくは VM のオプションで、
  -Dcom.android.screenshot.bindir=/path/to/tools/adb
と adb へのパスを指定してください。


つかいかた
----------

デバイスがつながっているかエミュレータが動いている状態で、
java -jar androcast.jar と実行すると開始します。

Device プルダウンからターゲットを選んで Start ボタンを押すとモニタを開
始します。Stop でモニタを止めます。

モニタ中は Capture ボタンを押すことで、その時の画面イメージを PNG 形式
で保存できます。

ランドスケープ画面でキャプチャするには Landscape チェックボックスをオン
にしてください。

Zoom プルダウンでは画面モニタの大きさを 50%, 75%, 100%, 150%, 200% から
選択出来ます。ふつうの java.awt.Graphics で描画しているので拡縮したとき
に補完はききません。

ツールバーは左端を掴んでウィンドウから離すとはずれます。ツールバー右端
の Pack ボタンを押すと残されたウィンドウがキャプチャの大きさにリサイズ
されます。動画撮影などでウィンドウサイズが正確に欲しいときに便利です。


既知の問題
----------

ターゲットがなにもない状態で起動すると、エラーが出てくるまでに時間がか
かる。

フレームレートが低い。

モニタ側からターゲットへの UI 入力は出来ない。

（たぶん）Swing の書き方が古い。

nudge による連続フレーム取得がなくなってしまったので、Android 2.x デバ
イスへの負担がものすごい。


ライセンス
----------

Copyright (C) 2008 The Android Open Source Project
Copyright (c) 2009-2010 Autch.net

Androcast は Apache ライセンス 2.0 で配布されます。
このプログラムは、Android ソースツリーの以下のファイルを元にしています。

development/tools/screenshot/src/com/android/screenshot/Screenshot.java
sdk/ddms/lib/ddmlib/src/com/android/ddmlib/ 以下の多数のファイル
system/core/adb/framebuffer_service.c

Apache License 2.0 の全文は LICENSE-2.0.txt をご覧ください。


--------
Autch
autch@autch.net
http://www.autch.net/
