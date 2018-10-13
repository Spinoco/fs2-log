import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import sbt.Tests.{Group, SubProcess}
import microsites.ExtraMdFileConfig

val ReleaseTag = """^release/([\d\.]+a?)$""".r

lazy val contributors = Seq(
  "pchlupacek" -> "Pavel Chlupáček"
)

lazy val commonSettings = Seq(
  organization := "com.spinoco",
//  scalaVersion := "2.12.6",
  crossScalaVersions := Seq("2.11.8", "2.12.6"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps",
    "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-value-discard",
    "-Ywarn-unused-import"
  ),
  scalacOptions in (Compile, console) ~= {_.filterNot("-Ywarn-unused-import" == _)},
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
  scmInfo := Some(ScmInfo(url("https://github.com/Spinoco/fs2-log"), "git@github.com:Spinoco/fs2-log.git")),
  homepage := None,
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  initialCommands := s"""
    import fs2._
    import fs2.util._
    import spinoco.fs2.log._
  """
  , libraryDependencies ++= Seq(
    "co.fs2" %% "fs2-core" % "1.0.0"
    , "co.fs2" %% "fs2-io" % "1.0.0"
    , "com.lihaoyi" %% "sourcecode" % "0.1.5"
    , "org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
  )
  , addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
) ++ testSettings ++ scaladocSettings ++ publishingSettings ++ releaseSettings

lazy val testSettings = Seq(
  parallelExecution in Test := false,
  fork in Test := true,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
  testGrouping in Test := (definedTests in Test).map { tests =>
    // group tests individually to fork them in JVM.
    // essentially any CassandraIntegration_* id having its own group, all others share a group
    // this is necessary hence JavaDriver seems to share some sort of global state preventing to switch
    // different cluster versions correctly in single JVM
    tests.groupBy { td =>
      if (td.name.contains(".CassandraIntegration")) {
        td.name
      } else "default_group"
    }.map { case (groupName, tests) =>
      Group(
        name = groupName
        , tests = tests
        , runPolicy = Tests.SubProcess(ForkOptions())
      )
    }.toSeq
  }.value
)

lazy val scaladocSettings = Seq(
  scalacOptions in (Compile, doc) ++= Seq(
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-implicits",
    "-implicits-show-all"
  ),
  scalacOptions in (Compile, doc) ~= { _ filterNot { _ == "-Xfatal-warnings" } },
  autoAPIMappings := true
)

lazy val publishingSettings = Seq(
  publishArtifact in Test := false
  , publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  credentials ++= Seq(Credentials(Path.userHome / ".ivy2" / ".credentials.sonatype")) ++ (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq,
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  pomExtra := {
    <url>https://github.com/Spinoco/fs2-cassandra.git</url>
      <developers>
        {for ((username, name) <- contributors) yield
        <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>http://github.com/{username}</url>
        </developer>
        }
      </developers>
  }
  ,pomPostProcess := { node =>
    import scala.xml._
    import scala.xml.transform._
    def stripIf(f: Node => Boolean) = new RewriteRule {
      override def transform(n: Node) =
        if (f(n)) NodeSeq.Empty else n
    }
    val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
    new RuleTransformer(stripTestScope).transform(node)(0)
  }
)

lazy val releaseSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value
)

lazy val noPublish = Seq(
  publish := (()),
  publishLocal := (()),
  publishSigned := (()),
  publishArtifact := false
)

lazy val `fs2-log-core` =
  project.in(file("core"))
    .settings(commonSettings)
    .settings(
      name := "fs2-log-core"
    )

