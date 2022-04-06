package dev.i10416.latex2unicode.ast

import cats.data.NonEmptyList
import scala.annotation.meta.param
import dev.i10416.latex2unicode.helper.Binary

trait Expr {
  def cBrace: Option[CBraceScope] = this match {
    case cb @ CBraceScope(_) => Some(cb)
    case _                   => None
  }
  def spaces: Option[Spaces.type] = this match {
    case sp @ Spaces => Some(Spaces)
    case _           => None
  }
  def postfix: Option[Postfix] = this match {
    case pf @ Postfix(_, _, _) => Some(pf)
    case _                     => None
  }
  def digit: Option[Digit] = this match {
    case d @ Digit(_) => Some(d)
    case _            => None
  }
  def str: Option[Str] = this match {
    case s @ Str(_) => Some(s)
    case _          => None
  }
  def standalone: Option[Standalone] = this match {
    case s @ Standalone(_) => Some(s)
    case _                 => None
  }
  def esc: Option[Esc] = this match {
    case e @ Esc(_) => Some(e)
    case _          => None
  }
  def func: Option[Func] = this match {
    case f @ Func(_, _, _) => Some(f)
    case _                 => None
  }
}
case class CBraceScope(content: List[Expr]) extends Expr
case object Spaces extends Expr
case object LineBreak extends Expr
case object Ignore extends Expr
case class Postfix(base: Expr, op: String, content: Either[Str, CBraceScope])
    extends Expr
case class Digit(value: String) extends Expr
case class Str(value: String) extends Expr
case class Standalone(value: String) extends Expr
sealed trait Cmd extends Expr

case class Esc(c: String) extends Cmd
case class Func(name: String, opt: Option[String], params: NonEmptyList[String])
    extends Cmd
