package dev.i10416.latex2unicode.helper

import dev.i10416.latex2unicode.commands.Frac

object Binary {

  def shouldParenthesizeStringWithChar(c: Char): Boolean = {
    !c.isLetterOrDigit && !Unary.isCombiningChar(c) && {
      val charType = c.getType
      charType != Character.OTHER_NUMBER && charType != Character.CONNECTOR_PUNCTUATION
    }
  }

  def maybeParenthesize(s: String): String = {
    if (!s.exists(shouldParenthesizeStringWithChar)) s
    else s"($s)"
  }

  def makeFraction(numerator: String, denominator: String): String = {
    val (n, d) = (numerator.trim, denominator.trim)
    if (n.isEmpty && d.isEmpty) ""
    else
      Frac.frac.get((numerator.trim, denominator.trim)) match {
        case Some(s) =>
          s
        case None =>
          s"(${maybeParenthesize(numerator)}/${maybeParenthesize(denominator)})"
      }
  }

  // Common helper interface

  val names = Set(Frac.name)

  def translate(command: String, param1: String, param2: String): String = {
    if (!names.contains(command)) {
      throw new IllegalArgumentException(s"Unknown command: $command")
    }

    assert(command == "\\frac")
    makeFraction(param1.trim, param2.trim)
  }
}
