organization := "com.github.mrpowers"
name := "bebe"

version := "0.0.1"
lazy val scala212 = "2.12.13"
lazy val scala211 = "2.11.12"
scalaVersion := scala212
crossScalaVersions := List(scala212, scala211)

libraryDependencies ++= {CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 12)) => List("org.apache.spark" %% "spark-sql" % "3.1.0" % "provided")
  case Some((2, 11)) => List("org.apache.spark" %% "spark-sql" % "2.4.7" % "provided")
  case _ => Nil
}}

libraryDependencies += "com.github.mrpowers" %% "spark-daria" % "0.38.2" % "test"
libraryDependencies += "com.github.mrpowers" %% "spark-fast-tests" % "0.21.3" % "test"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

// scaladoc settings
Compile / doc / scalacOptions ++= Seq("-groups")

// test suite settings
fork in Test := true
javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")
// Show runtime of tests
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

fork in Test := true

licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

homepage := Some(url("https://github.com/MrPowers/bebe"))
developers ++= List(
  Developer("MrPowers", "Matthew Powers", "@MrPowers", url("https://github.com/MrPowers"))
)
scmInfo := Some(ScmInfo(url("https://github.com/MrPowers/bebe"), "git@github.com:MrPowers/bebe.git"))

updateOptions := updateOptions.value.withLatestSnapshots(false)

publishMavenStyle := true

publishTo := sonatypePublishToBundle.value

Global/useGpgPinentry := true
