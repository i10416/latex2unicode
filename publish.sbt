val userId = "i10416"
val githubRepo = s"https://github.com/$userId/latex2unicode"

ThisBuild / versionScheme := Some("early-semver")

ThisBuild / organization := "dev.i10416"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url(githubRepo),
    s"scm:git@github.com:$userId/latex2unicode.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = userId,
    name = "Yoichiro Ito",
    email = "contact.110416+l2u@gmail.com",
    url = url(s"https://github.com/$userId")
  )
)

ThisBuild / description := "Convert LaTeX markup to Unicode."

ThisBuild / licenses := List(
  "Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
)

ThisBuild / homepage := Some(url(githubRepo))

ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / Test / publishArtifact := false

ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
