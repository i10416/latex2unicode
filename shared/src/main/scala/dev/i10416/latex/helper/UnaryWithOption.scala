package dev.i10416.latex2unicode.helper

import dev.i10416.latex2unicode.commands.Sqrt

object UnaryWithOption {
  def handleNoOpt(param: String): PartialFunction[String, String] =
    Sqrt.handleNoOpt.andThen(_.apply(param))
  def handleWithOpt(param: String): PartialFunction[(String, String), String] =
    Sqrt.handleWithOpt.andThen(_.apply(param))

  object HasDefault {
    val cmds = Map(Sqrt.name -> "")
    val names = cmds.keySet

  }

  val names = HasDefault.names

  def translate(command: String, option: String, param: String): String = {
    handleWithOpt(param).apply((command, option))
  }
}
