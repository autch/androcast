
Androcast
=========

Swing で書かれた、Android 用スクリーンモニタです。

Android cupcake の、
development/tools/screenshot/src/com/android/screenshot/Screenshot.java
からのパクリです（ぉ

Linux で画面モニタをしたくて自分で使うために作ったのでかなり荒削りです。
とくにエラー処理とか（ぉ


機能
----
・デバイスもしくはエミュレータを選択してモニタ開始
・ポートレイト／ランドスケープ切り替え
・PNG 形式でのキャプチャ
・50% 〜 200% のズーム


必要なもの
----------

・JRE6（javax.swing.SwingWorker を使っています）
・Android SDK 1.5r2 以降

実行には Android SDK に入っている tools/lib/ddmlib.jar が必要です。この
ファイルを SDK からコピーして、androcast.jar と同じディレクトリにおいて
ください。

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


既知の問題
----------

ターゲットがなにもない状態で起動すると、エラーが出てくるまでに時間がか
かる。

フレームレートが低い。

モニタ側からターゲットへの UI 入力は出来ない。

（たぶん）Swing の書き方が古い。


ライセンス
----------

Copyright (C) 2008 The Android Open Source Project
Copyright (c) 2009 Autch.net

Androcast は Apache ライセンス 2.0 で配布されます。
このプログラムは、Android ソースツリーの以下のファイルを元にしています。

development/tools/screenshot/src/com/android/screenshot/Screenshot.java

Apache License 2.0 の全文は LICENSE-2.0.txt をご覧ください。


--------
Autch
autch@autch.net
http://www.autch.net/
