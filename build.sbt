
val commonSettings = Seq(
  scalaVersion := "2.12.8",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-Ypartial-unification",
    "-Ywarn-unused:implicits",
    "-Ywarn-unused:imports",
    "-Ywarn-unused:locals",
    "-Ywarn-unused:params",
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:privates"
  )
)

lazy val root = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(
    name := "twitchapi4s",
    version := "1.0",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-mtl-core" % "0.4.0",
      "org.typelevel" %% "cats-effect" % "1.3.1",
      "com.softwaremill.sttp" %% "core" % "1.5.17",
      "io.circe" %% "circe-parser" % "0.10.0",
      "io.circe" %% "circe-generic" % "0.10.0",
    )
  )

lazy val zioimpl = (project in file("implementations/zio"))
  .settings(commonSettings: _*)
  .settings(
    name := "twitchapi4s-zio",
    version := "1.0",
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-zio" % "1.0-RC4",
      "org.scalaz" %% "scalaz-zio-interop-cats" % "1.0-RC4",
      "com.softwaremill.sttp" %% "async-http-client-backend-zio" % "1.5.17",
      "org.scalatest" %% "scalatest" % "3.0.5" % "test"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
  )
  .dependsOn(root)
