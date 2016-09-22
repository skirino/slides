## わかった気になるElixir macro

桐野 俊輔([skirino](https://github.com/skirino))

***

### [Elixir](http://elixir-lang.org/)とは
- [Erlang](http://www.erlang.org/)のプログラミングモデルとVM
- とっつきやすいsyntaxとツール群
- まともな(整数のリストでない)String
- **メタプログラミング**

***
***

### Elixir基礎文法
- See also [Elixir cheetsheet](https://media.pragprog.com/titles/elixir/ElixirCheat.pdf)

---

### 関数適用、リスト、無名関数

```
iex> Enum.map([1, 2, 3], fn i -> i * 2 end)
[2, 4, 6]
```

---

### tuple, map

```
iex> tuple = {"foo", :bar, 10}
{"foo", :bar, 10}
iex> elem(tuple, 1)
:bar
```

```
iex> m = %{a: 1, b: 2}
%{a: 1, b: 2}
iex> m[:a]
1
iex> Map.put(m, :a, 10)
%{a: 10, b: 2}
```

---

### モジュール、関数定義

```
iex> defmodule M do
...>   def f(x) do
...>     IO.puts(x)
...>   end
...> end

iex> M.f("hello")
hello
:ok
```

***
***

### Elixir macro
- compile時の[AST](https://en.wikipedia.org/wiki/Abstract_syntax_tree)変換
    - Cのプリプロセッサのような字句変換ではない
    - compilerはErlang/Elixir製
    - compile時とはいえ普通に自前Elixirコードを動かせる

---

### [homoiconicity](http://elixir-lang.org/)
- コード(のAST)を言語のデータ構造で表現できる
    - Lispなどと同じ
- 例:

```
iex> quote do: elem(tuple, 1)
{:elem, [context: Elixir, import: Kernel], [{:tuple, [], Elixir}, 1]}
```

```
iex> quote do
...>   String.split("hello", "e")
...> end
{{:., [], [{:__aliases__, [alias: false], [:String]}, :split]},
 [], ["hello", "e"]}
```

---

### ASTの内部表現
- 3要素tupleの連なり
    1. ASTノードを表すatom (関数・演算子の名前)
    2. environment (どのモジュールがimportされてるかとか)
    3. 引数(子ノード)のリスト or 変数の属するモジュール(atom)

---

### Macroのやること
- 呼び出し元から渡ってくる引数はquoted expression
- 普通の関数のように変換処理を書く
- quoted expressionを返す
- => 呼び出し位置にASTが挿入される

---

### Macro定義・展開の例

```
iex> defmodule M do
...>   defmacro m(quoted) do
...>     IO.inspect quoted
...>     quoted
...>   end
...> end

iex> require M # macroを有効化
iex> M.m(IO.puts("hello")) # macro起動
{{:., [line: 6], [{:__aliases__, [counter: 0, line: 6], [:IO]}, :puts]},
 [line: 6], ["hello"]}
hello
:ok
```

***
***

### quoted expressionの構築方法
- 普通のElixirコードをquoteする
    - 読み書きしやすい、小回りが効かない
    - 展開後の構造が決まっている場合
- 直接データ構造を作る
    - 難しい、柔軟
    - 展開後の構造が確定しない場合

---

### quoteによる構築

```
iex> defmodule M do
...>   defmacro m do
...>     quote do
...>       IO.puts("hello")
...>     end
...>   end
...> end

iex> require M
iex> M.m # macro起動、putsするコードに差し替わる
hello
:ok
```

---

### quoteを使わない方法

```
iex> defmodule M do
...>   defmacro m do
...>     {{:., [], [{:__aliases__, [], [:IO]}, :puts]}, [], ["hello"]}
...>   end
...> end

iex> require M
iex> M.m # 同様にputsするコードに差し替わる
hello
:ok
```

***
***

### quote内部への値の埋め込み

```
iex> defmodule M do
...>   defmacro m(x) do
...>     quote do
...>       IO.inspect(x)
...>     end
...>   end
...> end

iex> require M
iex> M.m("hello") # IO.inspect(x) となりxが見つからない
** (RuntimeError) undefined function: x/0
```

---

### `unquote`

```
iex> defmodule M do
...>   defmacro m(x) do
...>     quote do
...>       IO.inspect(unquote(x))
...>     end
...>   end
...> end

iex> require M
iex> M.m("hello") # IO.inspect("hello") に展開
"hello"
```

---

### 例: `Kernel.if/2`

```
defmacro if(condition, clauses) do
  do_clause   = Keyword.get(clauses, :do  , nil)
  else_clause = Keyword.get(clauses, :else, nil)

  optimize_boolean(quote do
    case unquote(condition) do
      x when x in [false, nil] -> unquote(else_clause)
      _                        -> unquote(do_clause)
    end
  end)
end
```

***
***

### unquote fragment

```
iex> defmodule M do
...>   kv = [foo: 1, bar: 2]
...>   # コンパイル時変数をコードに埋め込む。以下に展開される
...>   # `def foo(), do: 1`
...>   # `def bar(), do: 2`
...>   Enum.each(kv, fn {k, v} ->
...>     def unquote(k)(), do: unquote(v)
...>   end)
...> end

iex> M.foo
1
iex> M.bar
2
```

***
***

### Hooks
- `__using__(args)`
    - `use M`で起動されるmacro(require不要)
    - 複数moduleで共通のコードを入れ込む
- `__before_compile__(env)`
    - defmoduleの中身を処理し終わって今からコンパイル、のときに起動
    - "中身の処理"の間に溜めこんだ情報を使ってdefするためのもの

---

```
iex> defmodule M1 do
...>   defmacro __using__(_) do
...>     quote do
...>       def f(x), do: x + 1
...>     end
...>   end
...> end

iex> defmodule M2 do
...>   use M1
...> end

iex> M2.f(3)
4
```

---

```
iex> defmodule M1 do
...>   defmacro __before_compile__(env) do
...>     ns = Module.get_attribute(env.module, :numbers)
...>     quote bind_quoted: [ns: ns] do
...>       Enum.each(ns, fn n ->
...>         def f(unquote(n)), do: to_string(unquote(n))
...>       end)
...>     end
...>   end
...> end
iex> defmodule M2 do
...>   @before_compile M1
...>   @numbers [1, 2, 3]
...> end
iex> M2.f(1) # => "1"
iex> M2.f(0) # => FunctionClauseError
```

***
***

### 例1: method的なsyntax
- [Croma](https://github.com/skirino/croma)より: [source](https://github.com/skirino/croma/blob/master/lib/croma/struct_call_syntax.ex), [API doc](http://hexdocs.pm/croma/Croma.StructCallSyntax.html)

```
iex> defmodule S do
...>   defstruct [:a, :b]
...>   def f(s, i) do
...>     s.a + s.b + i
...>   end
...> end

iex> import Croma.StructCallSyntax # `~>`演算子を見えるようにする
nil
iex> s = %S{a: 1, b: 2}
%S{a: 1, b: 2}
iex> s~>f(3) # 新しいsyntax!
6
```

---

### 例1: method的なsyntax
- 展開後
    - 内部データになっているmoduleを取り出してから関数呼び出し
    - "レシーバ"は第一引数にする

```
iex> s.__struct__.f(s, 3)
6
```

***
***

### 例2: Haskellのdo記法的なもの
- [Croma](https://github.com/skirino/croma)より: [source](https://github.com/skirino/croma/blob/master/lib/croma/monad.ex), [API doc](http://hexdocs.pm/croma/Croma.Monad.html)

```
iex> use Croma
nil
iex> Croma.Result.m do # それっぽいsyntax!
...>   x <- {:ok, 1}
...>   y <- {:ok, 2}
...>   pure x + y
...> end
{:ok, 3}
```

---

### 例2: Haskellのdo記法的なもの
- 展開後
    - `bind/2`呼び出しの連鎖に置き換え

```
iex> Croma.Result.bind({:ok, 1}, fn x ->
...>   Croma.Result.bind({:ok, 2}, fn y ->
...>     Croma.Result.pure(x + y)
...>   end)
...> end)
{:ok, 3}
```

***
***

### tips: quoted expressionの構築
- 作りたいコードをquoteしてみる
- 出力結果のデータ構造を作るコードを書く
- デバッグ
    - AST同士の比較
    - `Macro.to_string`でコードに変換して比較

---

### tips: マクロ処理中の状態の保持
- 連番を振る、メモ化、etc.
    - globalな状態の保持はErlang的に苦手
- [module attribute](http://elixir-lang.org/getting-started/module-attributes.html)を使う
- 困ったらプロセスを作る
    - [Agent](http://elixir-lang.org/docs/v1.0/elixir/Agent.html)で状態をwrap

***
***

### "With great power comes great responsibility"
- macroコードは最小限にする
    - 不要なmacroは本番コードに入れない
    - 処理はなるべくヘルパー関数に移譲する
