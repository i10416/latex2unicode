package dev.i10416.latex2unicode
import dev.i10416.latex2unicode.ast._
import dev.i10416.latex2unicode.helper._
import cats.data.NonEmptyList
import cats.implicits._

import cats.parse.{Parser0 => P0, Parser => P, Numbers}
import cats.parse.Rfc5234._
object LatexParser {

  def isLiteralChar(c: Char): Boolean =
    !c.isWhitespace && c != '{' /*not start cbrace scope*/ && c != '\\' /*not start command*/ && c != '-' && c != '~' /*not space*/ && c != '$' && c != '^' /*not superscript*/ && c != '_' /*not subscript*/ && c != '}' && c != '%'

  def spacesCountNewLines =
    P.charsWhile(_.isWhitespace).map(_.count(c => c == '\n' || c == '\r'))

  // ---
  // ----

  val tryDashLike = P
    .charIn('-')
    .rep(1, 3)
    .map(l => helper.Escape.translate("-" * l.length))
    .soft <* P.not(P.charIn('-'))

  val tryDashLikeExpr = tryDashLike.map(Standalone(_))
  val dashLike = tryDashLike | P.charIn('-').rep.string
  val dashLikeExpr =
    tryDashLikeExpr | P.charIn('-').rep.string.map(Standalone(_))

  final val ignorable = P.charIn('$').void.as("")
  final val ignorableExpr: P[Expr] = P.charIn('$').void.as(Ignore)
  final val mapToWs = P.charIn('~').as(" ")
  final val mapToWsExpr = mapToWs.as(Str(" "))
  val standalone = dashLike | ignorable | mapToWs

  val standaloneExpr: P[Expr] = dashLikeExpr | ignorableExpr | mapToWsExpr

  def ignorableSpaces = spacesCountNewLines.?.flatMap {
    case None              => P.unit
    case Some(n) if n <= 1 => P.unit
    case _                 => P.fail
  }
  val literalCharsBlockInOption: P[String] =
    P.charsWhile(c => c != ']' && isLiteralChar(c))

  val opt = literalCharsBlockInOption.rep0.with1
    .between(P.char('['), P.char(']'))
    .map(_.mkString)

  /** space block must contains at least one space.
    */
  def spacesBlock = spacesCountNewLines.map {
    case noneOrOne if noneOrOne <= 1 => " "
    case _                           => "\n\n"
  }
  def spacesBlockExpr: P[Expr] = spacesCountNewLines.map {
    case noneOrOne if noneOrOne <= 1 => Spaces
    case _                           => LineBreak
  }

  def argCnt(name: String) = name match {
    case unary
        if helper.Unary.names.contains(unary) || helper.UnaryWithOption.names
          .contains(unary) =>
      1
    case binary if helper.Binary.names.contains(binary) => 2
    case _                                              => -1
  }
  val specialCharSet = Set(
    "\\$",
    "\\%",
    "\\&",
    "\\~",
    "\\_",
    "\\-",
    "\\^",
    "\\\\",
    "\\{",
    "\\}",
    "\\#"
  )

  val specialChars =
    P.charIn('$', '%', '&', '~', '_', '-', '^', '\\', '{', '}', '#')

  val singleLiteral = P.charWhere(isLiteralChar).string
  final val openCBrace = P.char('{')
  final val closeCBrace = P.char('}')
  final val startBlock = P.string("\\begin")
  final val endBlock = P.string("\\end")

  /** {{{
    * TOP ::= (exp)*
    *
    * (exp) ::= (curlybrace) | (block) | (command) | (text)
    *
    * (curlybrace) ::= { (exp)* }
    *
    * (block) ::= \\begin{ (blockname) } (exp)* \\end{ $(blockname) }
    *
    * (command) ::= \\ (name) (curlybrace)
    *
    * (name) ::= [\w_]+ \*?
    * }}}
    */
  val startCmdBlock = startBlock *> P.anyChar.rep.string
    .surroundedBy(P.charsWhile0((_.isWhitespace)))
    .between(openCBrace, closeCBrace)

  val startEscOrCmd =
    (P.charIn('\\') ~ (specialChars | P.charWhere(_.isLetter).rep)).string
  def escOrCmd(cbrace: P[String]) = P.recursive[String] { _escOrCmd =>
    startEscOrCmd.flatMap {
      case escapeSpecialChar if specialCharSet.contains(escapeSpecialChar) =>
        P.pure(escapeSpecialChar)
      case str =>
        P.pure(
          str
        ) <* (digit | opt | cbrace | specialChars | _escOrCmd | sp | P.end).peek
    }
  }
  val optionalWS = P.charsWhile0((_.isWhitespace))

  val spAndOpt = sp.void.rep.? ~ opt.?

  def expr = P.recursive[String] { ex =>
    val cbraceScope = ex.rep0
      .map(_.mkString)
      .with1
      .between(openCBrace, closeCBrace)

    val escOrCmdExpr = P.recursive[Cmd] { _escOrCmdExpr =>
      val arg0 =
        digit.string | standalone | cbraceScope | _escOrCmdExpr.map(render)
      val argN = (singleLiteral | arg0).surroundedBy(ignorableSpaces)
      escOrCmd(cbraceScope).flatMap {
        case esc if helper.Escape.names.contains(esc) =>
          P.pure(Esc(esc))
        case cmd =>
          val cnt = argCnt(cmd)
          spAndOpt.flatMap {
            case (None, Some(opt)) =>
              argN.rep(cnt, cnt).map(args => Func(cmd, Some(opt), args))
            case (None, None) =>
              (arg0 ~ argN.surroundedBy(ignorableSpaces).rep0(cnt - 1, cnt - 1))
                .map { case (a0, as) =>
                  Func(cmd, None, NonEmptyList.apply(a0, as))
                }
            case (Some(_), Some(opt)) =>
              argN.rep(cnt, cnt).map(args => Func(cmd, Some(opt), args))
            case (Some(_), None) =>
              argN.rep(cnt, cnt).map(args => Func(cmd, None, args))
          }
      }
    }

    val cmdExpr = escOrCmdExpr.map(render)
    //val cmdBlock = startCmdBlock.flatMap { name =>
    // (ResolveEnv(name,expr:P[Expr]): P[Expr]) <* end
    val cmdBlock = startCmdBlock.flatMap { name =>
      val end = endBlock *> P
        .string(name)
        .surroundedBy(optionalWS)
        .between(openCBrace, closeCBrace)
      ex.rep0.map(_.mkString) <* end
    }

    val postfix = (((singleLiteral | cbraceScope).soft ~ P
      .charIn('_', '^')
      .string) ~ (ignorableSpaces *> (singleLiteral | cbraceScope))).map {
      case ((base, "_"), content) =>
        base + Unary.makeSubscript(content)
      case ((base, "^"), content) =>
        base + Unary.makeSuperScript(content)
      case ((base, op), content) =>
        s"($base)$op{$content}"
    }

    postfix | standalone | cmdBlock | cmdExpr | cbraceScope | spacesBlock | singleLiteral
  }

  def parse(latex: String) = expr.rep0.map(_.mkString).parse(latex)

  def render(bs: Cmd) = bs match {
    case Esc(name) => Escape.translate(name)
    case Func(name, None, NonEmptyList(first, second :: Nil)) =>
      Binary.translate(name, first, second)
    case Func(name, None, params) => Unary.translate(name, params.head)
    case Func(name, Some(opt), params) =>
      UnaryWithOption.translate(name, opt, params.head)
  }
}
