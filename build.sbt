// todo: build cats-parse against native platform.
val scala212Version = "2.12.15"
val scala213Version = "2.13.8"
val scala30Version = "3.0.2"
val scala31Version = "3.1.2-RC2"
inThisBuild(
  Seq(
    scalaVersion := scala213Version,
    version := "0.0.1",
    crossScalaVersions := Seq(
      scala212Version,
      scala213Version,
      scala30Version,
      scala31Version
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature"
    )
  )
)

lazy val lib = crossProject(JSPlatform, JVMPlatform /*, NativePlatform*/ )
  .in(file("."))
  .settings(
    name := "latex2unicode",
    libraryDependencies ++= Deps.shared.value,
    libraryDependencies ++= Deps.testShared.value.map(_ % Test)
  )
  .jsSettings()
  .jvmSettings()
//.nativeSettings()
