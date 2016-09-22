## [Stuff goes bad: Erlang in Anger](http://www.erlang-in-anger.com/)の紹介

桐野 俊輔([skirino](https://github.com/skirino))

***

### この本について

- 著者
    -  [Learn You Some Erlang for Great Good!](http://learnyousomeerlang.com/)のFred Hebertさん & Heroku
    - FredさんはHerokuのrouting teamのメンバー
- Creative Commons License
- Erlang関連のtips集、比較的運用寄り
    - この手の情報源は貴重
    - 初心者向けではない

---

### この発表について

- 本の中の大事なところの抜粋・要約
- ツールの細かい使い方などはスキップ
- (based on v1.0.3 of the book)

***
***

### Erlangについておさらい

- Chapter 1: How to Dive into a Code Base
- Chapter 2: Building Open Source Erlang Software

---

### Actor Model

- ErlangVMは軽量な内部プロセスを実装している
    - OSプロセスとの区別に注意
- Erlangプロセスは
    - 自分の実行コンテキスト・スタック/ヒープ・mailboxを持つ
    - VMのスケジューラにCPUコアを割り当てられたら動作(preemptive)
    - メモリ領域を共有しない(プロセス単位でGC)

---

### Supervision Tree

- crashして死んだプロセスを生き返らせるプロセス(supervisor)でツリー構造を作る
    - 要するにエラー時にrestartする仕組み
        - だいたいの問題はtransientなのでrestartは多くの場合良い戦略
    - 未知のバグ・ネットワーク障害などにほぼ対応できる
- Supervision tree関連の仕組みはOTPがまるっと提供してくれている

---

### おまけ1: [Let it crash ... Except When You Shouldn't](https://www.google.co.jp/url?sa=t&rct=j&q=&esrc=s&source=web&cd=2&cad=rja&uact=8&ved=0CCcQFjABahUKEwjlmYbiwPTIAhXh6KYKHUI-Bo4&url=http%3A%2F%2Fqconlondon.com%2Flondon-2011%2Fqconlondon.com%2Fdl%2Fqcon-london-2011%2Fslides%2FSteveVinoski_LetItCrashExceptWhenYouShouldnt.pdf&usg=AFQjCNGlHodIUTr7_7yfOgGvMhhpTSYd4Q&sig2=v4-RWzmF9QLnIrFHdZfqOQ)

- "Let it crash"は勘違いされてる場合も多い
    - (最近では"let it fail"とも言うらしい)
- "Handle what you can, and let someone else handle the rest."
    - 自分のコンテキストでhandleすべきエラーをsupervisor任せにして無視してはならない

---

### Supervision treeでの初期化処理

- supervisorが子プロセスを立ち上げ(`start_link`)て初期化(`init/1`)を待つ部分は"同期的"
    - "まともな状態"の子プロセスを開始することを保証
    - リトライの起点になる"まともな状態"についてちゃんと考える必要がある
- リトライで対処する類のエラーが起きうる処理を`init/1`に含めるべきではない
    - 例: (たいていは後者にすべき)
        - remote DBとの接続まで`init/1`でやる
        - DB接続は切れることがあるので`init/1`では接続せず後回し

---

### おまけ2: Avoid distributed supervision tree

- remote actorをtransparentに扱えると聞いて、「remote actorを含むsupervision treeを構成しよう」と思うかもしれないが、オススメできない
    - remote actorと通信できなくなったsupervisorはrestartを実行できない可能性がある
    - 要するに、初期化処理の実行がきちんとできないケースがあるので良くない
- それぞれのnodeでOTP applicationごとにsupervision treeを作るべき

---

### OTP application

- moduleの集まり(+α)にメタデータ(バージョンなど)がついたもの
    - library application: [application behaviour](http://www.erlang.org/doc/man/application.html)を実装していない (supervision treeを持たない)
    - regular application: [application behaviour](http://www.erlang.org/doc/man/application.html)を実装している (supervision treeを持つ)
- [rebar](https://github.com/rebar/rebar)や[mix](http://elixir-lang.org/getting-started/mix-otp/introduction-to-mix.html)といったビルドツールがOTP application単位で動作するので、普通に作業していればだいたいわかる

---

### OTP release

- 複数のOTP applicationをパッケージングしたもの
    - ErlangVMのバイナリも含まれる
    - release単位でシステムとして稼働させる
    - hot code upgradeなどの仕組みも備わっている
- 設定項目が山ほどあって大変なので、手間を軽減するツール群がある
    - [systools](http://www.erlang.org/doc/man/systools.html), [reltool](http://www.erlang.org/doc/man/reltool.html), [relx](https://github.com/erlware/relx), [exrm](https://github.com/bitwalker/exrm)
    - Elixirではexrm経由でrelxを使うのが普通
- いろいろ実験しないとよくわからない(してもよくわからない)

***
***

### Chapter 3: Planning for Overload

- 多くの障害はメモリ不足
    - 特にmessageが溜まりすぎてしまう事態
- 過負荷への対処
    - back-pressureで入ってくるデータを制限
    - 入ってきたデータを捨てる
- ここでのscopeは、ボトルネック最適化・計算機資源の増強・等々をやってもいつかは発生する問題としての「過負荷」

---

### Restricting Input


- システムの奥底のレイヤーから順にback-pressureを適用していく必要がある
    - 例: DBアクセス層のback-pressureを定義、それをWebAPI層に適用してHTTPレスポンスに反映
- fairnessについても要検討
    - 特定ユーザが大量にリクエストのせいで、少ししか使ってないユーザのリクエストがブロックされてもOK?
- この手の用途に使えるライブラリ群:
    - [safetyvalue](https://github.com/jlouis/safetyvalve), [breaky](https://github.com/mmzeeman/breaky), [fuse](https://github.com/jlouis/fuse), [circuit_breaker](https://github.com/klarna/circuit_breaker)

---

### Discarding Data

- 扱いきれない分をランダムに選んで捨てる
- 一時的な過負荷: bufferを使う
    - queue buffer: 順序を維持
    - stack buffer: latencyについてのQoSを満たせないリクエストをごそっと捨てる
- 恒久的な過負荷: 実作業を行うworker processを増やす(VM内でload balancing)

***
***

### Chapter 4: Connecting to Remote Nodes

- relxを使っていればスクリプトからerlang shellに接続できるので、割愛

***
***

### Chapter 5: Runtime Metrics

- [recon](https://github.com/ferd/recon)の説明
    - だいたいErlang標準のmetrics取得APIのwrapper
    - 稼働中システムの動作を妨げない範囲で、便利なインターフェースになっている
    - remote_consoleから動かして情報取得をカンタンにする

***
***

### Chapter 6: Reading Crash Dumps

- reconの中に入っているスクリプトの説明
    - そのままだとわかりづらいerl_crash.dumpファイルの解析

***
***

### Chapter 7: Memory Leaks

- crashdumpやruntime metricsから以下のような兆候を見つけたらメモリリークが疑われる
    - メモリ割り当てエラー
    - 一定期間おきのcrash
    - 負荷ピーク以外で突然ダウン(ピーク時のダウンはおそらくchapter3の話)
- `erlang:memory/0`などでカテゴリ別のメモリ使用量がわかる

---

### Atom

- atomはglobalなテーブルに居残り続ける(GCされない)
- 動的な文字列をatomに変換してはならない
- `xmerl`など一部のライブラリで勝手にatomに変換してしまうものがあるので注意

---

### Code

- (基本的にはそうそう問題にはならないはず)
- Hot code upgradeでは、モジュールごとに最大2バージョンまで保持する
- [HiPE](http://www.erlang.org/doc/man/HiPE_app.html)コンパイルされたモジュールは残り続けるので要注意
    - あんまり頻繁に更新されるモジュールをHiPEコンパイルするのは避けるべき

---

### ETS

- ErlangVMに備わっているインメモリDB的なもの(dets, mnesiaもある)
- GCの対象ではないので、領域の開放にはレコードを明示的に削除
- どのテーブルが問題か`ets:i/0`で確認可能

---

### プロセス関連

- (多岐にわたるので代表的なチェック項目)
- どのプロセスともlinkされてない浮いてるプロセス
- プロセスごとのメモリ使用量、mailboxサイズ
- GC実行時間(`erlang:system_monitor/2`を使う)

---

### Binaries (1)

- Erlangのbinaryの実体には2タイプある
    - プロセスのヒープに配置: 64バイト以下のもの。他のデータ同様にGCされる
    - globalなヒープ領域に配置: サイズが大きいか、他のglobalなbinaryのsub-binary。プロセスのメモリ領域には参照だけが置かれる。参照カウント方式でGCされる

---

### Binaries (2)

- 問題(1): 小さいsub-binaryのせいで本体が解放されない
- 対応策
    - 明示的にsub-binaryをコピーで作れば別の実体ができて避けられる

---

### Binaries (3)

- 問題(2): プロセスのヒープが逼迫してGCが走ってはじめて参照が破棄される。参照1コのサイズは小さいので、プロセスのGCの契機になりづらい。例えば、binaryを受け渡すrouterプロセス
- 対応策
    - バイナリが経由するプロセスを減らす(メッセージを入れたプロセスと処理するプロセスとで直接やり取りさせる)
    - 適切なタイミングでGC起動するようにする

---

### Memory fragmentation

- OSから割り当てられたメモリ量と、ErlangVM内で有効活用できている量とで差異がある場合、fragmentationが疑われる
- ErlangVMには役割別に10種ほどのallocatorがあり、そのどれが問題かを調べる必要がある
- 簡単な対応策はないが
    - メモリ割り当てパターンを把握する
    - allocatorごとに("best fit"などの)戦略を選べるので、これをいじると良いかも

---

### その他のメモリリーク要因

- C driver
- NIF
- VMそのもの(可能性低)

***
***

### Chapter 8: CPU and Scheduler Hogs

- 多数のプロセスとschedulerがゴニョゴニョやっているので、CPU使用率について把握するのは難しい
- 各プロセスの"reduction count"が目安になる
    - "reduction"は処理の単位でだいたい関数呼び出し
    - 1000 reductionsを使い切ると別プロセスにCPUが回される
- schedulingに時間がかかったときに通知をもらう: `erlang:system_monitor/2`

***
***

### Chapter 9: Tracing

- いわゆるデバッガはあんまり役に立たない
    - あるプロセスの実行をbreakpointで止めても、他プロセスは動いてたりする
- ErlangVMとライブラリが提供するtracingを使うのがベター
    - とりあえず[`recon_trace`モジュール](http://ferd.github.io/recon/recon_trace.html)を使っとけばOK
