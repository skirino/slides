use Croma

defmodule DialyzerPlayground do
  #
  # Success typing paper:
  # https://it.uu.se/research/group/hipe/papers/succ_types.pdf
  # Dialyzer warnings below are generated using Erlang/OTP 19.1, Elixir 1.3.3, dialyze 0.2.1
  #

  # Invalid type specification for function 'Elixir.DialyzerPlayground':simple_error/0. The success typing is () -> <<_:216>>
  defun simple_type_mismatch :: integer do
    "String.t instead of integer"
  end

  # Invalid type specification for function 'Elixir.DialyzerPlayground':warn_spec_of_this_function_instead_of_plus_operand/1. The success typing is (number()) -> number()
  defun warn_spec_of_this_function_instead_of_plus_operands(a :: String.t) :: integer do
    a + 1
  end

  # The call erlang:'+'(s@1::binary(),1) will never return since it differs in the 1st argument from the success typing arguments: (number(),number())
  defun guard_imposes_constraint_on_argument_type(s :: g[String.t]) :: integer do
    s + 1
  end

  # The call 'Elixir.String':split(x@1::[any()],<<_:8>>) will never return since the success typing is (binary(),binary() | [binary()] | binary:cp() | #{'__struct__':='Elixir.Regex', 'opts':=binary(), 're_pattern':=_, 'source':=binary()}) -> [binary()] and the contract is (t(),pattern() | 'Elixir.Regex':t()) -> [t()]
  def spec_is_not_necessary_for_dialyzer_to_analyze_function_body(x, y) do
    z = length(x) + y
    h = hd(String.split(x, "\n"))
    IO.puts("h=#{h} z=#{z}")
  end

  # no warning
  defun optimistic_in_return_type :: String.t do
    if :rand.uniform(10) < 5 do
      :a
    else
      "a"
    end
  end

  # no warning
  def optimistic_in_return_type_no_typespec do
    if :rand.uniform(10) < 5 do
      :a
    else
      "a"
    end
  end

  # Guard test is_atom(a@1::<<_:8>>) can never succeed
  # The variable _l@1 can never match since previous clauses completely covered the type <<_:8>>
  defun when_calling_function_its_typespec_is_used :: :atom do
    case optimistic_in_return_type do
      a when is_atom(a)   -> :atom
      b when is_binary(b) -> :binary
      l when is_list(l)   -> :list
    end
  end

  # The variable _l@1 can never match since previous clauses completely covered the type 'a' | <<_:8>>
  defun if_typespec_is_not_available_it_is_inferred :: :atom do
    case optimistic_in_return_type_no_typespec do
      a when is_atom(a)   -> :atom
      b when is_binary(b) -> :binary
      l when is_list(l)   -> :list
    end
  end

  # no warning
  defun cannot_generate_correct_return_value_but_dialyzer_is_not_smart_enough(x :: integer) :: %{a: :x, b: :y} do
    a = if x <  5, do: :x, else: :y
    b = if x < 10, do: :x, else: :y
    %{a: a, b: b}
  end

  # The call 'Elixir.DialyzerPlayground':union_type(42) breaks the contract (prime_upto_43()) -> prime_upto_43()
  def call_identity_for_prime_upto_43 do
    identity_for_prime_upto_43(42)
  end
  @type prime_upto_43 :: 2 | 3 | 5 | 7 | 11 | 13 | 17 | 19 | 23 | 29 | 31 | 37 | 43
  defun identity_for_prime_upto_43(a :: prime_upto_43) :: prime_upto_43 do
    a
  end

  # no warning
  def too_large_union_type_is_approximated_by_generic_type do
    identity_for_prime_upto_47(42)
  end
  @type prime_upto_47 :: 2 | 3 | 5 | 7 | 11 | 13 | 17 | 19 | 23 | 29 | 31 | 37 | 43 | 47
  defun identity_for_prime_upto_47(a :: prime_upto_47) :: prime_upto_47 do
    a
  end

  # no warning
  defun generous_in_too_generic_typespec(s :: any) :: any do
    String.split(s, "\n")
  end

  # The call 'Elixir.DialyzerPlayground':generous_in_too_generic_typespec(100) will never return since it differs in the 1st argument from the success typing arguments: (binary())
  defun more_specific_typespec_obtained_by_analysis_is_used :: integer do
    length(generous_in_too_generic_typespec(100))
  end

  defun head_plus_one(l :: [pos_integer]) :: pos_integer do
    hd(l) + 1
  end

  # The call 'Elixir.DialyzerPlayground':precise_spec([]) will never return since it differs in the 1st argument from the success typing arguments: (nonempty_maybe_improper_list())
  defun empty_list_is_specially_treated :: pos_integer do
    head_plus_one([])
  end

  # The call 'Elixir.DialyzerPlayground':head_plus_one([-1,...]) breaks the contract ([pos_integer()]) -> pos_integer()
  defun type_of_list_element_matters :: pos_integer do
    head_plus_one([-1])
  end

  # Function just_raise/0 has no local return
  defun just_raise :: :ok do
    raise "foobar"
    :ok
  end
end
