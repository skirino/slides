// Copyright(c) 2013-2015 ACCESS CO., LTD. All rights reserved.

import sbt._
import Keys._
import com.sksamuel.scapegoat.sbt.ScapegoatSbtPlugin.autoImport._
import scoverage.ScoverageSbtPlugin._

object ApplicationBuild extends Build
{
  val name = "PFDSTest"

  val appDependencies = Seq(
    "org.scalacheck" %% "scalacheck" % "1.12.3" % "test"
  )
  val repositories    = Seq.empty
  val basicSettings   = Seq(
    version              := "0.1",
    scalaVersion         := "2.11.6",
    libraryDependencies ++= appDependencies,
    resolvers           ++= repositories
  )

  val scapegoatSettings = Seq(
    scapegoatIgnoredFiles        := Seq.empty,
    scapegoatDisabledInspections := Seq.empty
  )

  val scoverageSettings = Seq(
    ScoverageKeys.coverageHighlighting     := true,
    ScoverageKeys.coverageExcludedPackages := """"""
  )

  val compileOptions = Seq(
    "-feature", "-Xfatal-warnings", "-Xlint:_",
    "-Ywarn-dead-code", "-Ywarn-numeric-widen",
    "-Ywarn-unused", "-Ywarn-unused-import",
    "-Ywarn-value-discard"
  )
  val compileOptionsRemovedInTest = Set(
    "-Ywarn-value-discard"
  )
  val compileOptionsAddedInTest = Seq.empty[String]
  val compileOptionSettings = Seq(
    scalaSource in Compile := baseDirectory.value / "src",
    scalaSource in Test    := baseDirectory.value / "test",

    scalacOptions         ++= compileOptions,
    scalacOptions in Test ++= (scalacOptions in (Compile, compile)).value,
    scalacOptions in Test  ~= { opts => opts.filterNot { compileOptionsRemovedInTest.contains(_) } },
    scalacOptions in Test ++= compileOptionsAddedInTest
  )

  val commandAliasSettings = (
    addCommandAlias("c" , "compile"       ) ++
    addCommandAlias("tc", "test:compile"  ) ++
    addCommandAlias("t" , "test"          ) ++
    addCommandAlias("st", "scoverage:test")
  )

  val proxyProperties = Seq("http.proxyHost", "http.proxyPort", "http.nonProxyHosts", "https.proxyHost", "https.proxyPort")
    .map { name => Option(System.getProperty(name)).map { value => s"-D${name}=${value}" } }.flatten
  val javaProperties = Seq("-Dfile.encoding=UTF-8") ++ proxyProperties

  val javaMemoryOptions = sys.process.javaVmArguments.filter { arg => Seq("-Xmx", "-Xms", "-XX").exists(arg.startsWith) }
  val runtimeSettings = Seq(
    javaOptions ++= javaProperties ++ javaMemoryOptions// ++ Seq("-XX:+DisableExplicitGC", "-verbose:gc")
  )

  val projectSettings = basicSettings ++ scapegoatSettings ++ scoverageSettings ++ compileOptionSettings ++ commandAliasSettings ++ runtimeSettings

  val main = Project(name, file("."), settings = projectSettings)
  override def rootProject = Some(main)
}
