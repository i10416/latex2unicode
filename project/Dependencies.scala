import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object V {
  val catparse = "0.3.8"
  val scalatest = "3.2.13"
}

object Deps {

  val catparse = Seq(
    ("org.typelevel", "cats-parse", V.catparse)
  )
  val shared = Def.setting {
    (catparse).map { case (org, lib, v) =>
      (org %%% lib % v).cross(CrossVersion.for3Use2_13)
    }
  }

  val testShared = Def.setting {
    (scalatest).map { case (org, lib, v) => org %%% lib % v }
  }

  val scalatest = Seq(
    ("org.scalatest", "scalatest", V.scalatest)
  )

}
