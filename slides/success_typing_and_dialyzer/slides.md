## Success Typing and Dialyzer

桐野 俊輔([skirino](https://github.com/skirino))

***

### Success typing

- 型付けの方法の1つ
- [論文](https://it.uu.se/research/group/hipe/papers/succ_types.pdf)
- [dialyzer](http://erlang.org/doc/man/dialyzer.html)で使われている
- [LYSEの説明](http://learnyousomeerlang.com/dialyzer)

---

### Motivation

- Erlangにafterthoughtとして型チェックを追加したい
- 既存のコードを書き直したくない
    - annotationを追加しなくてもそれなりに動いてほしい
    - false positiveを出したくない

***
***

### [Dialyzer](http://erlang.org/doc/man/dialyzer.html)

- Erlangの静的解析ツール
- コンパイル後のbeamファイルを解析
    - Elixirなどでも使える

---

### Dialyzer

- Success typingは通常の(C, Javaなどの)静的型チェックと根本的に異なる
- もっとも大きな違いは"bottom up"であること

---

### 使用例

- [Elixirでの使用例](https://github.com/skirino/slides/tree/master/slides/success_typing_and_dialyzer/dialyzer_playground/lib/dialyzer_playground.ex)
- 関連リンク
    - [Elixir文法cheetsheet](https://media.pragprog.com/titles/elixir/ElixirCheat.pdf)
    - [Erlang types and specifications](http://erlang.org/doc/reference_manual/typespec.html)
    - [Elixir typespecs](http://elixir-lang.org/docs/stable/elixir/typespecs.html)

***
***

### 感想

- メジャーな静的言語のような型チェックを期待してはならない
- とはいえけっこう便利なときもあるので使おう
