## Introduction to [croma](https://github.com/skirino/croma)

桐野 俊輔 ([skirino](https://github.com/skirino)) @ [ACCESS](http://jp.access-company.com/)

tokyo.ex #1
2016/4/19

***

[<img src="images/macro_madness.png" width="70%" />](http://toolbox.elixir.pm/#p-31)

***
***

### What?

- Elixir macro utilities
    - For extensive use of types
    - With less typing
- (This slides are based on croma v0.4.1)

---

### Motivation

- Even in Elixir types are good
    - to clearly express intention
- But
    - writing `@spec` is sometimes tedious
    - no type-checking is in place
- Let's fix that using macros

***
***

### `def` with `@spec`

```elixir
@spec foo(integer, atom) :: String.t
def foo(i, a) do
  "#{i} #{a}"
end
```

- `foo` appears twice; duplicated

---

### Let's make it DRY: `defun`

```elixir
use Croma
defun foo(i :: integer, a :: atom) :: String.t do
  "#{i} #{a}"
end
```

- Compiler expands this to the previous example
- Each argument and its type is placed side by side

***
***

### `def` with guard

```elixir
@spec foo(integer, atom) :: String.t
def foo(i, a) when is_integer(i) and is_atom(a) do
  "#{i} #{a}"
end
```

- `i`, `a` in both parameter list and guard
- Specifying argument types twice:
    - `integer` and `is_integer/1`
    - `atom` and `is_atom/1`

---

### Make it DRY again

```elixir
use Croma
defun foo(i :: g[integer], a :: g[atom]) :: String.t do
  "#{i} #{a}"
end
```

- Expanded to the previous example
    - No duplicated `foo`, `i`, `a`
    - Guards generated from `g[type]`

***
***

### Satisfied?

- Not at all
- Guards are fairly limited
    - Only handful of functions are allowed in guards by VM
- What we want:
    - More fine-grained data types
        - e.g. `String.t` => `UserName.t`
    - Checking values with arbitrary conditions
        - e.g. regex matching

***
***

### Building block: "Type module"

- Compared with Erlang, modules in Elixir are much lighter weight
    - So let's make many small modules
- We call modules that define the following as "type modules".
    - `@type t`
    -  `validate/1`

---

### Example (1)

```elixir
defmodule UserName do
  @type t :: String.t

  defun validate(value :: any) :: {:ok, t} | {:error, any} do
    if is_binary(value) and value =~ ~r/^\w+$/ do
      {:ok, value}
    else
      {:error, :invalid}
    end
  end
end
```

---

### Example (1')

- Typical type modules can be easily defined with helpers provided by croma:

```elixir
defmodule UserName do
  use Croma.SubtypeOfString, pattern: ~r/^\w+$/
end
```

---

### Example (2)

```elixir
defmodule User do
  use Croma.Struct, fields: [
    user_name: UserName,
    password:  Password,
  ]
end
```

- Using `UserName` and `Password`, it's converted to
    - `defstruct`
    - `@type t :: %__MODULE__{...}`
    - `validate/1`
    - (and more)

---

### Example (2')

```elixir
defmodule User do
  import Croma.TypeGen # for `nilable`

  use Croma.Struct, fields: [
    user_name:   UserName,
    password:    Password,
    description: Croma.String,                 # built-in type as type module
    age:         nilable(Croma.NonNegInteger), # ad-hoc generation of type module
  ]
end
```

- Useful for e.g. validating JSON data

***
***

### `defun` with type modules

```elixir
defun register(user_name :: v[UserName.t], password :: v[Password.t]) :: User.t do
  do_register(user_name, password)
end
```

- [Design by contract](https://en.wikipedia.org/wiki/Design_by_contract)
    - inside function body `user` is guaranteed to be valid

---

### Expansion of `v[Mod.t]`

```elixir
@spec register_user(UserName.t, Password.t) :: User.t
def register_user(user_name, password) do
  user_name =
    case UserName.validate(user_name) do
      {:ok, value}     -> value
      {:error, reason} -> raise "validation error for user_name: #{inspect(reason)}"
    end
  password =
    case Password.validate(password) do
      {:ok, value}     -> value
      {:error, reason} -> raise "validation error for password: #{inspect(reason)}"
    end
  do_register(user_name, password)
end
```

---

### Doesn't it make things slow?

- Yes but don't worry
    - You can disable arguments validation in production
    - Code generation is done by Elixir code and thus controllable

***
***

### Summary

- `croma` reduces boilerplate code and makes type-oriented programming more pleasant

- Feedbacks/comments/suggestions/bug reports/PRs are more than welcome

- Happy `defun`-ing!

- (BTW, my boss said that we are hiring!)
