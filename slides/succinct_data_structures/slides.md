## Succinct data structures

桐野 俊輔 ([skirino](https://github.com/skirino)) @ [ACCESS](http://jp.access-company.com/)

2019/02/20

***

### 今回の範囲
- [「簡潔データ構造」](https://www.amazon.co.jp/簡潔データ構造-アルゴリズム・サイエンスシリーズ―数理技法編-定兼-邦彦/dp/4320121740)のまとめ
    - ごく冒頭のエッセンスだけ。良い本だと思うので買おう
- 基礎となるbitvectorについてしゃべる
    - たいていのsuccinct data structureではbitvectorが部品になってる
    - メモリ量と処理のオーダーを理解してもらう

***
***

### 復習: big-O notation

- $O(f(n))$      : $n$が大きいときに$f(n)$の定数倍以下
- $\Omega(f(n))$ : $n$が大きいときに$f(n)$の定数倍以上
- $\Theta(f(n))$ : $n$が大きいときに$f(n)$の定数倍
- $o(f(n))$      : $n$が大きいときに$f(n)$よりも十分小さい
    - たとえば$\lg n = o(n)$

***
***

### 例題
- $[0, 255]$の範囲内のintegerが$n=10^9$個並んだ列がある。以下のようなクエリをどう処理する?
    1. $777777$回目に登場した$7$のindexは?
    2. 先頭から$5000000$番目までの間に$42$は何回出現したか?

---

### うまくやると
- 前処理: O(n)
- メモリ量: n + o(n) bytes
- 問い合わせ: O(1) (!)
    - 他の数値に対するクエリもサポートしている

***
***

### succinct data structure
- Information Theoretic Lower Boundに漸近的に近いデータ量でデータを保持
    - $o(ITLB)$の索引により効率のよいクエリ操作をサポート
- データ量が大きいときに特に効いてくる
- 最近になって新しいデータ構造が考案されていて一部でホット(らしい)

***
***

### bitvectorのサポートする処理
- $access(B, i)$  : $i$番目のビットを返す
- $rank0(B, i)$   : $[1, i)$の$0$の数を返す
- $rank1(B, i)$
- $select0(B, j)$ : $j$回目の$0$のindexを返す
- $select1(B, j)$

---

### 単なる配列だと
- メモリ量: $n$ bits
- access: $O(1)$
- rank, select: $O(n)$
    - 何らかのデータ構造が欲しい

***
***

### rank1の高速化(1)
- ビット列をblockに分け、block内の集計を終わらせておけば、呼び出し時にはblock内部を処理すれば済む
- が、block sizeは$n$に比例しており$O(1)$にはならない
- 同様に、blockの階層を作っても$O(1)$にはならない

---

### word-RAMモデル
- 単純なRAMモデルから多少現実的にした計算モデル
- 計算量の評価に使う
- アドレス空間は$2^w$
- word size: $w$
    - word単位のメモリアクセス・演算は1単位時間
    - 現実では$w=32$もしくは$64$

---

### rank1の高速化(2)
- 大ブロック: $l = \lg^2 n$
- 小ブロック: $s = \frac{1}{2} \lg n$
- 以下の整数配列を用意
    - $R_L[j] =$ ($j-1$までの大ブロック内の$1$の数)
    - $R_S[k] =$ ($k-1$までの小ブロック内の$1$の数)

---

### rank1の高速化(3)
- 以下の3つを合計する
    - $i$が属する大ブロックまでの合計: $R_L[\lceil i/l \rceil]$
    - $i$が属する小ブロックまでの合計: $R_S[\lceil i/s \rceil]$
    - 小ブロック内部のrank1計算は表引き(もしくはbitshift)により$O(1)$

---

### rank1の索引のサイズ
- $R_L$: $O((n / l)\lg n) = O(n / \lg n)$
    - 要素数: $n / l$
    - 各要素のサイズ: $\lg(n + 1) ビット$
- $R_S$: $O((n / s)\lg l) = O(n \lg \lg n / \lg n)$
    - 要素数: $n / s$
    - 各要素のサイズ: $\lg l$ビット
- 合わせても$o(n)$ビット

---

### rank0の計算
- 以下で済む。同じ索引を使い回せる
$rank0(B, i) = i - rank1(B, i)$

***
***

### select1の高速化(1)
- 大ブロック: $l = \lg^2 n$個の$1$を持つように分割
    - (rankのときは大ブロックの大きさは一定で$1$の数が可変だった。今回は大きさは可変で$1$の数が固定)
- 大ブロックが疎かどうかで内部表現を2パターンにする

---

### select1の高速化(2)

- 大ブロックが$\lg^4 n$よりも長く$1$がほとんどない => $1$の位置をそのまま格納
    - 位置を表す$\lg n$までの整数が$\lg^2 n$個
    - すべて疎だったとすると$n / \lg^4 n$個の大ブロックがある。全部で$O(n / \lg n)$

---

### select1の高速化(3)

- 密な場合(大ブロックの長さが$\lg^4 n$より短い)
    - 小ブロック: $s = \frac{1}{2} \lg n$の長さで分割
    - 少ブロックを葉にもつ$\sqrt{\lg n}$分木を構築
        - 小ブロックの数は$2 \lg^3 n$個以下 => 木の高さは定数
    - 各nodeで配下の$1$の数を幅優先で格納
        - 各要素は$l$までの整数。$O(\lg \lg n)$ビット
    - nodeが$O(n / s)$個。木は$O(n \lg \lg n / \lg n)$ビット

---

### select1の高速化(4)
- 加えて、各大ブロックのデータ構造へのポインタ
    - $O(n \lg n / l) = O(n / \lg n)$ビット

---

### select1(B, i)の計算(2)
- まずiの属する大ブロックを(割り算で)求める
- 疎ならばそのまま配列に答えがある
- 密なら
    - 木の根からi番目がいる葉を探索

---

### select1(B, i)の計算(3)
- 1つのnodeから次の子を$\sqrt{\lg n}$個の子のなかから探すときは、table lookupが使える
    - 子の数: $\sqrt{\lg n}$
    - 子のサイズ: $\lg \lg n$
    - 合計で $O(\sqrt{\lg n} \lg \lg n) = o(\lg n)$なので、$O(1)$で取得して次の子を表引きできる
- 木の高さも定数なので、定数回の操作で葉にたどり着く
- 葉の中のselect1もtable lookupで$O(1)$

---

### select0の計算
- select1の索引の反転バージョンを別途作っておき使う

***
***

### 疎なビットベクトル
- $0$と$1$のビットの出現頻度に偏りがあればより圧縮できる
- いろんなエンコーディング方式とaccess, rank, selectを両立させる手法が考案されている

---

### 実装上の話題
- selectの方が複雑な処理になるので、rankだけ$O(1)$にしてselectは二分探索にする実装も多い
- table lookupは不要な場合もあって、shiftしてbitcountすればOKだったりもする