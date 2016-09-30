defmodule DialyzerPlayground.Mixfile do
  use Mix.Project

  def project do
    [app: :dialyzer_playground,
     version: "0.1.0",
     elixir: "~> 1.3",
     build_embedded: Mix.env == :prod,
     start_permanent: Mix.env == :prod,
     deps: deps()]
  end

  defp deps do
    [
      {:croma, "0.5.0"},
      {:dialyze, "0.2.1"},
    ]
  end
end
