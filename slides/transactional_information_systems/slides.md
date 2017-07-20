## Transactional Information Systems

桐野 俊輔([skirino](https://github.com/skirino))

***

## 長く険しい戦いであった

[<img src="images/cover.jpg" width="40%" />](https://www.amazon.co.jp/gp/product/1558605088/ref=oh_aui_detailpage_o03_s00?ie=UTF8&psc=1)

---

## 読もうと思ったモチベーション

- ビジネス上の重要性の割にDBの理解が足りてない
    - 意識しなくてもそれなりに仕事になるのはスバラシイという言い方もできる
    - が、"それなり"止まり
- 中でもトランザクション関係は(個人的に)謎に包まれている

---

## おことわり

- わかってないところも(当然、いっぱい)ある。ツッコミ歓迎
- このスライドをself-containedにするつもりはほぼなくて、話の材料のリンク集程度
- [一人トランザクション技術 by @kumagiさん](http://qiita.com/advent-calendar/2016/transaction)が驚異的にまとまってる。そっち見たほうがいい

***
***

## [トランザクションとは](http://qiita.com/kumagi/items/5262ff4551ba19943b70)

- データに対する1つ以上の処理をまとめたもの
- トランザクションが"いい感じに"実行されてほしい
    - 他の処理と"かち合わない"、操作が失われない、... => ACID

---

## [ACID](http://qiita.com/kumagi/items/e93a57535db04e66368f) : Atomic

- トランザクション実行の**結果**がall or nothingになること
    - 一部のデータだけ更新されたりしない

---

## [ACID](http://qiita.com/kumagi/items/e93a57535db04e66368f) : Consistent

- 予め定められたconstraintsを逸脱するトランザクションはコミットされない
    - NOT NULL等。製品によって課せる制約にかなりバラつきがある
    - CAP定理のCとは別物

---

## [ACID](http://qiita.com/kumagi/items/e93a57535db04e66368f) : Isolated

- トランザクション実行の**過程**が他に影響を与えない
    - 書き込みがコミットされる前に読めてはマズい、等
- 実際は[isolation level](http://qiita.com/kumagi/items/1dc1a91ec007365ac694)に応じて調整可能
    - ある程度緩いレベルで良しとしている場合が多い

---

## [ACID](http://qiita.com/kumagi/items/e93a57535db04e66368f) : Durable

- Durable: 成功したトランザクションの結果が失われない
    - 不揮発性のディスクにデータがflushされてから"成功"にする
        - (正確にはメディアリカバリの話もあるが、ここでは無視)

---

## ちなみに、意味ないけど

- パフォーマンスを気にしなければ簡単。たとえば
    - 1度に1クエリしか受け付けず
    - すべての書き込みをログに書いて
    - すべての読み込みでログをフルスキャン
- 「好ましい性質を保ちつつパフォーマンスを改善する」ための努力が重ねられている

***
***

## 本の構成・話の流れ

1. concurrency control
    - recovery不要の状況でのconcurrencyを議論
        1. 読み書き処理のinterleavingの仕方の分類
        2. 望ましいinterleavingを生み出すscheduling algorithmを考える
2. recovery
    - abortを含む場合のconcurrency control
    - crash後の復旧のためのlogging, checkpointing
3. distributed transaction
    - distributed concurrency control, recovery
    - 2-phase commitでがんばれ

***
***

## Page model

- ファイルI/O操作の単位としてのblockがベース
    - より高レベルの操作も結局blockの出し入れに帰着する
- 実際、各DB製品ごとに内部で"page"という固定長区画の連なりとしてデータを配置して管理していることが多い
    - この単位だと思ってもいい
- とは言っても、"連続領域"や"固定長"という要素を使わないので、key-value的に捉えておいてOK

---

## 操作のnotation

- $r_i(x)$ : $i$で識別されるトランザクションが$x$で識別されるページをread
- $w_i(x)$ : $i$で識別されるトランザクションが$x$で識別されるページをwrite
- $c_i$    : $i$で識別されるトランザクションがcommit
- $a_i$    : $i$で識別されるトランザクションがabort

---

## トランザクションのnotation

- readとwriteの列
    - 並列実行が許される部分の順序付けは不要なので半順序

---

## Historyとschedule

- history:
    - トランザクション$t_1, ..., t_n$の操作をinterleaveした結果で、
    - 各トランザクション操作の後ろにcommitかabortどちらか1つを含むもの
    - e.g. $r_1(x) r_2(x) r_1(y) w_1(x) w_2(y) c_1 c_2$
- schedule:
    - historyのprefix
    - e.g. $r_1(x) r_2(x) r_1(y)$

---

## Serial history

- トランザクションが何らかの順で逐次実行された形
    - e.g. $r_1(x) r_1(y) w_1(x) c_1 r_2(x) w_2(y) c_2$
- serial historyと"等価" <=> serializable

---

## Herbrand semantics

- $r_i(x)$は最後に$x$に書かれた値を読む
- $w_i(x)$は、$t_i$がそれまでに読んだすべての値を引数に取る関数の適用結果を書き込む

***
***

## Anomalies

- historyを分類して、OK/NGを判定したい
    - つまり"等価"とはどういうことか検討したい
- それにあたりどういうhistoryがNGかを考えておく

---

## [Dirty read](http://qiita.com/kumagi/items/5ef5e404546736ebac49#dirty-read-anomaly)

- commitされていない値をread
- $r_1(x) w_1(x) r_2(x) a_1 w_2(x) c_2$
- abortされない場合でも、計算途中の値を読むのもアウト

---

## [Read skew](http://qiita.com/kumagi/items/5ef5e404546736ebac49#read-skew-anomaly)

- read操作の間に変更commitが入ってしまった
- $r_1(x) w_2(x) w_2(y) c_2 r_1(y) c_1$

---

## [Lost update](http://qiita.com/kumagi/items/5ef5e404546736ebac49#lost-update)

- 2つの更新のうち1つしか適用されない
- $r_1(x) r_2(x) w_1(x) c_1 w_2(x) c_2$

---

## [Inconsistent read](http://qiita.com/kumagi/items/5ef5e404546736ebac49#inconsistent-read-anomaly)

- 同じ値を2回読んだら、間の変更commitによって書き換わってしまった
- $r_1(x) w_2(x) c_2 r_1(x) c_1$

---

## [Write skew](http://qiita.com/kumagi/items/5ef5e404546736ebac49#write-skew-anomaly)

- $r_1(x) r_2(y) w_1(y) w_2(x) c_1 c_2$
- たとえば以下だとすると、$x = y = 0$が$x = y = 1$になってしまう。serialに実行したら$(1, 2), (2, 1)$のどちらかになるはず
    - $t_1$ : $y = x + 1$
    - $t_2$ : $x = y + 1$

---

## [Read only anomaly](http://qiita.com/kumagi/items/5ef5e404546736ebac49#read-only-anomaly)

- $r_3(x) r_3(y) r_2(x) w_2(x) c_2 r_1(x) r_2(y) c_1 w_3(y) c_3$
    - $t_2, t_3$による書き込みの間にreadonlyな$t_1$が挟まる
    - $t_3$による$y$への書き込みは初期状態の$x$, $y$を使っているので、$t_3$ => $t_2$の順と等価
    - が、実際のcommit順は$c_2$ => $c_3$
    - このときに$t_1$が読む値はcommit順のとおり$t_2$で$x$が変更された値で、一意なserial historyと等価にならない

---

## (おまけ)[External consistency](https://www.cockroachlabs.com/blog/living-without-atomic-clocks/)

- page $x$はnode Aに、page $y$はnode Bにいるとする
- node Bのclockは正確、node Aのclockは100ms進んでいる
- $t=0$にトランザクション1が$x$に書いてcommit。node Aは$t=100ms$だと思っている
- $t=50ms$にトランザクション2が$y$に書いてcommit。node Bは$t=50ms$だと思っている(正しい)
- $x$, $y$の更新timestampがズレる

***
***

## [Final state serializable](http://qiita.com/kumagi/items/695f1641407fd726d180)

- 2つのscheduleの結果のHerbrand semanticsが同一であればfinal state equivalent
- 何らかのserial historyとfinal state equivalentなhistoryは$\in FSR$
- read skew等のanomalyを防げない、FSRに含まれるかの判定に時間がかかる => 実用的でない

---

## [View serializable](http://qiita.com/kumagi/items/ec498950f8d0a96012f5)

- 最後だけではなくすべてのread, writeでHerbrand semanticsが一致であればview equivalent
- 何らかのserial historyとview equivalentなhistoryは$\in VSR$
- $VSR \subset FSR$
- VSRに含まれるかの判定に時間がかかる => 実用的でない

---

## [Conflict serializable](http://qiita.com/kumagi/items/2cc25050d7eedb7d5c8d) (1)

- 同じpageに対する{r, w}, {w, r}, {w, w} : conflict
    - これらのペアの順序を入れ替えるとsemanticsが変わる
- それ以外のペアは入れ替えても等価(conflict equivalent)とみなせる

---

## [Conflict serializable](http://qiita.com/kumagi/items/2cc25050d7eedb7d5c8d) (2)

- 何らかのserial historyとconflict equivalentなhistoryは$\in CSR$
- e.g.
    - $w_1(x) r_2(x) w_1(y) w_1(z) r_3(z) w_2(y) w_3(y) w_3(z)$
    - $\Rightarrow w_1(x) w_1(y) r_2(x) w_1(z) w_2(y) r_3(z) w_3(y) w_3(z)$
    - $\Rightarrow w_1(x) w_1(y) w_1(z) r_2(x) w_2(y) r_3(z) w_3(y) w_3(z)$
    - $\equiv t_1 t_2 t_3$
- $CSR \subset VSR$

---

## [Conflict serializable](http://qiita.com/kumagi/items/2cc25050d7eedb7d5c8d) (3)

- "serializability theorem"
- CSRに属するかは有向グラフで表現できる
- scheduleから"conflict graph"を以下のように描く
    - 関与するトランザクションがvertex
    - conflictにある操作のペアがedge
- e.g.
    - $r_1(x) r_2(x) w_1(x) r_3(x) w_3(x) w_2(y) c_3 c_2 w_1(y) c_1$
    - $t_1$ => $t_3$, $t_2$ => $t_1$, $t_2$ => $t_3$

---

## [Conflict serializable](http://qiita.com/kumagi/items/2cc25050d7eedb7d5c8d) (3)

- $s \in CSR$ iff conflict graph is asyclic
    - conflictしている部分は入れ替えられないので、その順序でconflict equivalentなserial historyが決まる

***
***

## Scheduling algorithms

- "scheduler"
    - transactionの各操作が順次入力として入ってくる
    - serializableなscheduleを生成する
        - ここではCSRなscheduleを生成することを考える

---

## Scheduler classification

- "locking"
    - lockを使うもの
    - 必然的にpessimistic concurrency control
- "non-locking"
    - lockは使わないもの
    - pessimisticでもoptimisticでもありうる

***
***

## Locking schedulers

- lockを使って共通で使われるデータへのアクセスをsynchronizeする
- 操作の前にlockを取得
    - 取得できなければ待つ
- 操作の後にlockを解放

---

## Notation

- scheduleにlock/unlockを含めて考える
    - read lock    : $rl_i(x)$
    - write lock   : $wl_i(x)$
    - read unlock  : $ru_i(x)$
    - write unlock : $wu_i(x)$

---

## Example schedule

- $s = rl_1(x) r_1(x) wl_1(y) w_1(y) ru_1(x) wu_1(y) c_1$
  $wl_2(x) w_2(x) wl_2(y) w_2(y) wu_2(x) wu_2(y) c_2$

---

## Rules for well-formed locking

- $r$/$w$の前に$rl$/$wl$がある
- $r$/$w$の後に$ru$/$wu$がある
- 1つのtransactionは多重にlockをかけない
- $t_i$がすでにlockしている$x$を$t_j$がlockする場合
    - $rl_i(x) <_s rl_j(x)$ : OK
    - $rl_i(x) <_s wl_j(x)$ : NG
    - $wl_i(x) <_s rl_j(x)$ : NG
    - $wl_i(x) <_s wl_j(x)$ : NG
- (これらを満たさないscheduleは違法として考えない)

---

## Two-phase locking (2PL)

- (Two-phase commitと混同しないように)
- 定義: 2PLなscheduleでは
    - すべてのlockがすべてのunlockよりも先にくる
        - 言い換えると、lockの"growing phase"と"shrinking phase"に分かれている。shrinking phaseになるまではlockを1つも手放さない

---

## Example

- Input:
    - $w_1(x) w_1(y) c_1$
    - $r_2(x) w_2(y) c_2$
    - $r_3(z) w_3(y) w_3(z) c_3$
- Output (一例、他もありうる):
    - $wl_1(x) w_1(x) wl_1(y) w_1(y) wl_1(z) w_1(z) wu_1(x) rl_2(x) r_2(x)$
      $wu_1(y) wu_1(z) c_1 rl_3(z) r_3(z) wl_2(y) w_2(y) wu_2(y) ru_2(x) c_2$
      $wl_3(y) w_3(y) wl_3(z) w_3(z) wu_3(z) wu_3(y) c_3$

---

## CSR-safety (1)

- 大雑把には
    - 個々のpageに対するconflictはlockが順序を決める
    - 1つのtransactionの中のlockとunlockに順序がある
    - 両者合わせて全体として整合的な順序が作れる

---

## CSR-safety (2)

- 2PLな$s$とそのconflict graph $G$を考える
- Lemma 1-a
    - $(t_i, t_j)$ is an edge in $G$ $\Rightarrow$ $pu_i(x) < ql_j(x)$
        - $p, q$は$r$か$w$、どちらも$r$は除外
- Proof
    1. $G$の定義より$p_i(x) < q_j(x)$
    2. lockのルールによりunlockが先に来ないとlockできない

---

## CSR-safety (3)

- Lemma 2
    - $G$はacyclic
- Proof
    - cycleがあると仮定、そのedgeを$(t_1, t_2), ..., (t_n, t_1)$と書く
    - Lemma 1より、各edgeにつき$pu_1(x) < ql_2(x), ..., $が成り立つ
    - つまり「shrinking phaseが始まるのは$t_1$の方が$t_2$より先」、...
    - 回りまわって大小関係に矛盾

---

## CSR-safety (4)

- ただちに$Gen(2PL) \subseteq CSR$となる
- 実際はもっとrefineできて$Gen(2PL) \subset OCSR \subset CSR$
    - Order-preserving CSR

---

## Deadlock handling (1)

- ここまでの話だけではdeadlockが避けられない。対処が必要
- deadlock detection
    - どのtransactionがどのtransactionを待っているかをtrackingして検出
        - graphがcyclicになったらdeadlock
    - 検出したらcycleを壊すためどれかをabortさせる
        - どれをabortさせるかの戦略がいくつか

---

## Deadlock handling (2)

- deadlock prevention
    - 何らかの優先順位に応じてlockをかけ、優先順位に合わない場合はabort
        1. lock済みtxに比べ、自分の方が先にスタートしたtxであれば待つ、そうでなければabort
        2. lock済みtxに比べ、自分の方が先にスタートしたtxであれば相手をabort、そうでなければ待つ
        3. lock済みtxがlock獲得待ちならabortさせ、そうでなければ自分が待つ(activeな方優先)
        4. ...
    - 実際にdeadlockが発生しなくてもabortが起きうる点に注意

---

## Variants of 2PL (1)

- Conservative 2PL
    - 必要なlockを一度に獲得する(growing phaseが1瞬で終わる)
    - deadlockが避けられる、始まったらblockされない
    - が、実際は必要なlockが必ずしもすべてわかっているわけではない

---

## Variants of 2PL (2)

- Strict 2PL
    - すべてのwrite lockを一度に解放する
    - recoveryの点でも良い性質がある(class ST)
    - 実際の製品でよく使われている

---

## Variants of 2PL (3)

- Strong strict 2PL
    - すべてのlockを一度に解放する(shrinking phaseが1瞬で終わる)
    - recoveryの点でも良い性質がある(class RG)
    - $Gen(SS2PL) \subset Gen(S2PL) \subset Gen(2PL)$
    - $Gen(SS2PL) \subset COCSR$
        - Commit Order-preserving CSR

---

## ANSI isolationレベルとS2PL

- read uncommitted : writeのみS2PL、read lockなし
- read committed : writeのみS2PL、read lockはありだがいつでも取得/解放
- repeatable read : read/writeでS2PL
- serializable : read/writeでS2PLに加えてphantom対策のpredicate locking

***
***

## Nonlocking schedulers

- Timestamp ordering
- Serialization graph testing

---

## Timestamp ordering (1)

- ルール
    - $t_i$にtimestampとして$ts(t_i)$を振る(e.g. 狭義単調増加なtx ID)
    - $p_i(x), q_j(x)$がconflictしているとき、$p_i(x) < q_j(x)$ iff $ts(t_i) < ts(t_j)$
- $Gen(TO) \subseteq CSR$
    - conflictのあるtx間にはtimestampによる順序ができcycleにならない

---

## Timestamp ordering (2)

- 実装上は
    - 最後にread/writeしたtimestampをdata itemごとに覚えておく
    - すでに後続txが処理を行ってしまっていたらabort、新しいtimestampでやり直す

---

## Serialization graph testing

- serialization graphを保持
- txに応じてnodeを作る
- conflictしている操作を行うときにはedgeを追加、cycleができたらabort
- $Gen(SGT) = CSR$
- graph保持・cycle有無チェックのコストが高くあまり使われない

***
***

## Optimistic protocols (1)

- conflictの頻度が少なければ、lock操作の負担を減らしたくなる
- 逐一関門(lock)を設けるのではなく、read/write処理はどんどん進めてしまい、最後にvalidateする方式
- マルチコアな流れもあって最近よく採用されている模様

---

## Optimistic protocols (2)

- 以下の3 phaseに分けてtxを実行する
    1. Read phase : 読み出し時にバージョンも取る。書き込みは本体には反映せず"write set"として蓄積していく
    2. Validation phase : バージョンが変わっていないことを検証する
    3. Write phase : write setの中身を本体に適用する

---

## Optimistic protocols (3)

- 実装上は複数の処理をatomicに適用する必要があり、限定的にlockする形になる
    1. validation phaseの前にwrite setすべてにlockをかける
    2. read setのバージョンを確認
    3. バージョンが一致したらwrite setを反映、変わっていたらabort
    4. unlock
