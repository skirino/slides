## TLA+入門

桐野 俊輔 ([skirino](https://github.com/skirino)) @ [ACCESS](http://jp.access-company.com/)

2018/11/28

***

### [TLA+](https://lamport.azurewebsites.net/tla/tla.html)とは

- [wikipedia](https://en.wikipedia.org/wiki/TLA%2B)より:
    - a formal specification language
    - by [Leslie Lamport](https://en.wikipedia.org/wiki/Leslie_Lamport)

***
***

### 個人的なモチベーション

- ([Raftの論文](https://ramcloud.stanford.edu/~ongaro/thesis.pdf)などは末尾にTLA+ specが記載されており、認識だけしていた)
- 「設計」という作業の成果物はどうあるべきか、よくわからない状態が続いていた
- TLA+の適用事例を最近見つけて、うまく活用できれば理想的に思えた

---

### 事例: AWS

- [Use of Formal Methods at AWS](https://lamport.azurewebsites.net/tla/formal-methods-amazon.pdf)
    - S3, DynamoDBなど、2014年時点で10プロジェクトで使用
    - "In every case TLA+ has added significant value"
        - 他の手法で見つからないバグ発見、設計変更が適切かの検証

---

### 事例: Azure Cosmos DB

- https://azure.microsoft.com/en-us/blog/a-technical-overview-of-azure-cosmos-db/
- https://www.youtube.com/watch?v=L_PPKyAsR3w
- (他にもいっぱい事例はあるはず)

***
***

### Benefits (1)

- 曖昧さの無いspecを書くことで
    - 「システムが何をすべきか・何をすべきでないか」を深く理解でき、また正確に伝達できる
        - 副次的な効果として、コード上で有効なassertを書けるようになる

---

### Benefits (2)

- TLC (model checker)を使ってspecを検証できる
    - 特にconcurrent/distributedなbugは一般的に厄介だが、網羅的にチェックできる
    - specの問題はコードレベルでは取り戻せない

---

### FAQ (1)

- TLA+ specと実コードとの対応は?
    - コードを生成するツールがあるわけではない。対応関係を維持するのは人間
    - TLA+はより抽象的なレベルで考えるためのツールという位置づけ
- model checkingは状態空間が膨大になるのでは?
    - これはTLCでも同様のはず。上記の事例だと複数マシンをぶん回している模様

---

### FAQ (2)

- うまくいくのか?
    - コードのような発想で書くとすぐに複雑になってしまう
    - システムの本質を捉えられる範囲内で不要な複雑性を削ぎ落として頑張る

***
***

### TLA+ specの概要(1)

- プログラミング言語よりも数学に近い
- ASCIIで表記するがLaTeX経由で見やすくできる
- モジュール化の仕組みが用意されている

---

### TLA+ specの概要(2)

- "state": 対象システムとまわりの環境を表現
- "step": stateを変更するイベント
    - ユーザ操作・伝送路上での故障などに対応

---

### TLA+ specの概要(3)

- stateを構成する"variables"を宣言する
    - 各プロセスの持っている情報
    - 伝送路上にある転送中の情報
    - etc.

---

### TLA+ specの概要(4)

- 以下2つを定義する
    - $Init$ : 初期状態が満たすべき条件
    - $Next$ : 各stepのbefore/afterの満たすべき条件
        - 複数種類のstepを$\vee$でつなぐ

---

### TLA+ specの概要(5)

- $Init$と$Next$が決まったら、(大抵の場合)システムのspecは以下
  $Spec \mathrel{\smash{{\stackrel{\scriptscriptstyle\Delta}{=}}}} Init \wedge \Box [Next]_{vars}$

---

### TLA+ specの概要(6)

- $Spec$に基づいて動作するシステムで満たされるはずのinvariantを以下のように書く
  **THEOREM** $Spec \Rightarrow \Box Inv$
- たいていは各variableの取りうる値をチェックする

***
***

### TLCの概要

- $Init$, $Next$, $Inv$を指定して実行
- $Init$, $Next$を元に状態空間を探索
- 各状態が$Inv$を満たすことを確かめる

***
***

### Demo using [TLA toolbox](http://lamport.azurewebsites.net/tla/toolbox.html)

***
***

### Learning materials

- [The TLA+ Video Course](http://lamport.azurewebsites.net/video/videos.html)がおすすめ
    - 丁寧、教育的、[toolbox](http://lamport.azurewebsites.net/tla/toolbox.html)に即した内容
    - Lamport先生がおちゃめ
- 本: [Specifying Systems](http://lamport.azurewebsites.net/tla/book.html)
    - はっきり言って難しい
    - Liveness checkingおよびreal-time propertiesのため腰を据えて読む予定...

---

### 関連情報・ツールの紹介

- [Temporal Logic of Actions](https://en.wikipedia.org/wiki/Temporal_logic_of_actions)
    - Temporal logic(時相論理)を元にした理論
- [PlusCal](http://lamport.azurewebsites.net/tla/pluscal.html)
    - TLA+にtranspileされる別言語。アルゴリズム記述向け
- [TLAPS](http://tla.msr-inria.inria.fr/tlaps/content/Home.html)
    - 定理証明系

***
***

### まとめ・雑感(1)

- 何か役立てられそうか検討中。進展があったら別途発表する
- 教材・ツールが揃っていて思ったよりは取っつきやすい
    - Safety propertiesだけのチェックでも十分役に立つケースがありそう

---

### まとめ・雑感(2)

- Property-based testingと近い部分もあるが、うまく橋渡しできるだろうか?
- Erlangなどのactor system(や、その延長線上にある[dist_agent](https://github.com/skirino/dist_agent))はatomicな操作が明確になっており、TLA+ specとコードの対応関係がつけやすく相性が良いかもしれない
