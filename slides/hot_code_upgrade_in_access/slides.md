## Hot code upgrade in ACCESS

桐野 俊輔 ([skirino](https://github.com/skirino)) @ [ACCESS](http://jp.access-company.com/)

[Tokyo.ex #8](https://beam-lang.connpass.com/event/70973/) (2017/11/26)

***

1. Hot code upgradeの仕組み
    a. ErlangVMの提供する機能
    b. OTPの提供する機能
2. ACCESSでの運用状況・設計判断について紹介

***
***

## Hot code upgradeの仕組み

---

### Hot code upgradeとは

- ErlangVMの実行を止めずにコードを更新できる
- Erlang/OTPの強みの1つとされることが多い
- が、あまり情報が出回ってない
    - 実運用してる人はわりと少ないようだ
- 「やめておけ」と言われることが多い([ここ](http://learnyousomeerlang.com/relups#the-ninth-circle-of-erl)とか)

---

### Pros

- OSプロセスが生き続けるので、メモリ上のデータ・TCPコネクションなどを維持できる
- Load balancerの向き先などを変更しなくてよい
- ErlangVM再起動などの手段よりも速い
- (なんだかかっこいい)

---

### Cons

- 正しくやるのは難しい
    - コード変更内容に応じて更新時の処理が違う
    - 特にテストをきっちりやるのは困難
        - 変更内容に応じて毎回状況が違う
        - prod環境のプロセス内部状態(厳密には負荷状況も)を再現しなければならない

---

### Module loading

- ErlangVMはmodule単位でコードを管理する
- コンパイラは各moduleのソースコードからbeamファイルを作る
- ErlangVM起動後、beamファイルを読み込んで実行
    - beamファイルの読み込み操作などのAPIが提供されているので、Erlang/Elixirから挙動をプログラミングできる
        - [:code](http://erlang.org/doc/man/code.html), [Code](https://hexdocs.pm/elixir/Code.html), [IEx.Helpers](https://hexdocs.pm/iex/IEx.Helpers.html)

---

### How hot code upgrade works (1)

- ErlangVMは各moduleの中身を2バージョンまで保持する
    - `"current"`と`"old"`
- `current`と`old`のコードはどちらも実行可能
- "新規の"関数呼び出しは`current`を使う
    - モジュール名を指定せずに同一モジュールの関数を呼ぶ場合(local call)は"新規"ではない

---

### How hot code upgrade works (2)

- 新しいバージョンのmoduleをロードしたとき
    - ロードしたバージョンを`current`に
    - `current`を`old`にする
    - `old`を捨てる
        - その際`old`を実行中のプロセスはbrutal kill
            - stacktraceが残ってれば"実行中"

---

### 注意点

- killされないよう、`old`から`current`に切り替わるようにしなければならない
    - 普通に`GenServer`などを書いてればだいたい大丈夫
- 一部の操作は`old`を消して`current`だけにしようとする
    - やはり`old`実行中プロセスがkillされる
    - たとえば[`:release_handler.make_permanent/1`](http://erlang.org/doc/man/release_handler.html#make_permanent-1)をテキトーに呼ぶと危険

***
***

## Demo

---

### Demo (1)

- 次のスライドのコードを`loop.ex`などとして保存
- iexから以下でループ開始
    - `> c("loop.ex")`
    - `> spawn_link(fn -> Loop.loop(0) end)`
- コードを書き換えて、再度 `> c("loop.ex")`を試す

---

```ex
defmodule Loop do
  def loop(n) do
    Enum.each(1..20, fn i ->
      :timer.sleep(1_000)
      IO.inspect({n, i, str(), __MODULE__.str()}) # local and remote call
    end)
    __MODULE__.loop(n + 1) # remote and tail-recursive call
  end

  def str() do
    "str/0 version1" # change this and reload!
  end
end
```

---

### Demo (2)

- 一度新バージョンのコードを実行しても、callstackが残っていると移行しきれない
- 旧バージョンが完全になくなるのは、`loop/1`の末尾再帰のとき
    - 末尾再帰がremote callでない場合、oldのままで更新されず、次バージョンのload時にkill
- 移行しきる前に2回loadするとkill

***
***

## Hot code upgrade and OTP

---

### OTP special processes (`GenServer`など)

- module差し替えだけで済まない場合がある
    - プロセスの持つデータの移行
- [code_changeの仕組み](http://erlang.org/doc/design_principles/release_handling.html#instr)
    - 更新対象のcallback moduleを使っている全プロセスで処理を中断させる
    - [`code_change/3` callback](http://erlang.org/doc/man/gen_server.html#Module:code_change-3)でデータ移行
    - 通常の処理を再開

---

### `code_change/3`のデメリット

- 一連の挙動はsystem messageのやり取りで実現されている
    - けっこう無駄なやり取りが多い
    - system messageへの応答が通常処理でブロックされうる。5秒タイムアウトで強制kill
    - 一部のライブラリが定義するプロセスでは、system messageをちゃんとhandleしてくれない場合がある
- suspendしている間は処理が滞る
    - 対象プロセス群の内部状態をatomicに更新するが、こんな重量級の仕組みはいらない場合も多い

---

### OTP application/release

- OTP application:
    - beamファイルと設定ファイルからなるパッケージングの単位
    - [appup](http://erlang.org/doc/man/appup.html)ファイル: バージョン更新処理を定義する
        - "`code_change/3`でデータ移行"もここで指定
- OTP release:
    - 複数のOTP applicationsをErlangVMとセットにしたもの
    - [relup](http://erlang.org/doc/man/relup.html)ファイル: `appup`ファイル群をまとめたもの
    - release単位で更新を適用する方法が定義されている([:release_handler](http://erlang.org/doc/man/release_handler.html))

---

### release一式を生成するツールたち

- [systools](http://erlang.org/doc/man/systools.html): Erlang同梱(詳細不明)
- [reltool](http://erlang.org/doc/man/reltool.html): Erlang同梱(詳細不明)
- [relx](https://github.com/erlware/relx): 起動スクリプトもセットで便利、systoolsに依存(使っている)
- [exrm](https://github.com/bitwalker/exrm): Elixir用、relxベース(一時使っていたが今は使ってない)
- [distillery](https://github.com/bitwalker/distillery): pure Elixir(使ってない)

***
***

## ACCESSにおける運用状況

---

### antikytheraについて

- OSSのPaaS framework
- AWSで稼働中
- 社内の各チームがそれぞれOTP applicationを作る
- antikythera coreチームがまとめて運用
- (いろいろしゃべりたいが、時間の都合で略)

---

### 自動デプロイ方針

- antikythera本体の更新はreleaseを作って[`:release_handler.install_release/2`](http://erlang.org/doc/man/release_handler.html#install_release-1)で取り込む(hot code upgrade)
- 各チームのOTP appの更新は[`:release_handler.upgrade_app/2`](http://erlang.org/doc/man/release_handler.html#upgrade_app-2)で取り込む(hot code upgrade)
- "ややこしい"更新は特別扱いして、EC2インスタンス入れ替えで適用する
- hot code upgradeの際には`code_change/3`しない
    - `update`ではなく`load_module`を使う単純なappupを生成するツールを自作

---

### "ややこしい"更新について

- 以下のような更新はgitコミットメッセージに固定文字列を入れておく
    - プロセス内部状態の変更
    - supervision treeの構造変更など、初期化処理の変更
- デプロイの際、変更分のコミットメッセージを見てデプロイ方式を切り替え

---

### `code_change/3`を使わない理由

- (デメリットについては前述)
- [cowboy websocketプロセスがsystem messageに応答しない](https://github.com/ninenines/cowboy/issues/1126)
    - suspendのためsystem messageが送られると、websocketプロセスがcrashして[`:init.restart/0`](http://erlang.org/doc/man/init.html#restart-0)になる
- 動作検証が割に合わない
    - 1回きりの`code_change/3`のためにテスト書きたくない
        - 更新内容に応じて状況を再現するのもしんどい
    - インスタンス追加なら更新内容に依らず、同じようにクラスタに参加するだけ

---

### `code_change/3`の代替案

- `code_change/3`に相当するコードを`handle_*`の最初に呼び出してデータ移行することもある
- 単にプロセス内部状態を切り替えるだけ(同じmoduleを使う全プロセスでsyncする必要が無い)ならこれで良い
- ただし以下は問題になりうる
    - 移行処理が何度も呼ばれる。idempotentにして、余計な処理はしない
    - 一度もmessageが来ないプロセスでは移行処理も走らない
- pros/consを比べて、インスタンス入れ替えと使い分ける

---

### hot code upgradeの意義

- インスタンス入れ替えだけでもまあまあOKだが、以下のためなるべくhot code upgradeを使いたい
    - 1日に何十回もデプロイしようと思うと、インスタンス入れ替えは遅すぎる
    - websocket connectionをそのままキープしたい
    - [raft_fleet](https://github.com/skirino/raft_fleet)のprocess rebalancingは比較的重いので避けたい

***
***

### Summary

- hot code upgradeそのものは有用だが、OTPにあまり頼り過ぎると良くない場合もある
    - `GenServer`ばかり書いていると`code_change/3`が目立つが、これが役に立つのは限られた状況のみ
- hot code upgradeだけでやろうとせず、インスタンス入れ替え方式にfallbackできるように用意しておくといい
