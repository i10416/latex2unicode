package dev.i10416.latex2unicode.commands

import dev.i10416.latex2unicode.helper.Unary

object Sqrt extends UnaryCmd {
  val name: String = "\\sqrt"
  def handleNoOpt: PartialFunction[String, String => String] = { case `name` =>
    param => render(param)
  }
  def handleWithOpt: PartialFunction[(String, String), String => String] = {
    case (`name`, "" | "2") => param => render(param)
    case (`name`, "3")      => param => render(param, "∛")
    case (`name`, "4")      => param => render(param, "∜")
    case (`name`, radix) =>
      param =>
        Unary.tryMakeSuperScript(radix).getOrElse(s"($radix)") + render(
          param
        )
  }
  def render(radicand: String, radix: String = "√"): String =
    radix + Unary.translateCombining("\\overline", radicand)

}
