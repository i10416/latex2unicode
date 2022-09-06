package dev.i10416.latex2unicode
import dev.i10416.latex2unicode.ast._
import org.scalatest._
import matchers.should.Matchers._
import org.scalatest.matchers
import org.scalatest.funsuite.AnyFunSuite

class L2UTest extends AnyFunSuite {
  test("spaceCountNewLines counts new lines inside spaces") {
    val Right((_, a)) =
      LatexParser.spacesCountNewLines.parse("    \n\n \r \n  ")
    a shouldBe 4
  }
  test("not takes 1 argument") {
    LatexParser.argCnt("\\not") shouldBe 1
  }
  test("frac takes 2 arguments") {
    LatexParser.argCnt("\\frac") shouldBe 2
  }
  test("$ is standalone expr") {
    LatexParser.standalone.parse("$").isRight shouldBe true
  }
  test("dashes is standalone expr") {
    LatexParser.standalone.parse("-").isRight shouldBe true
  }
  test("~ is standalone") {
    LatexParser.standalone.parse("~").isRight shouldBe true
  }
  test("~ is standalone expr") {
    val Right((_, a)) = LatexParser.standaloneExpr.parse("~")
    a shouldBe Str(" ")
  }
  test("$~ are standalone exprs") {
    LatexParser.standalone.parse("$~").isRight shouldBe true
  }
  test("parser ignores $") {
    val Right((a, _)) = LatexParser.parse("$")
    a shouldBe ""
  }
  test("Empty string is valid latex input") {
    val Right((_, empty)) = LatexParser.parse("")
    empty shouldBe ""
  }
  test("single space is a block") {
    val Right((_, space)) = LatexParser.parse(" ")
    space shouldBe " "
  }
  test("a literal") {
    val Right((_, r)) = LatexParser.parse("a")
    r shouldBe "a"
  }
  test("a digit") {
    val Right((_, r)) = LatexParser.parse("1")
    r shouldBe "1"
  }
  test("literal") {
    val Right((_, r)) = LatexParser.parse("This is a test")
    r shouldBe "This is a test"
  }
  test("literal with curlybraces") {
    val Right((_, r)) =
      LatexParser.parse("{This {{i}{s}}{ a }test}")
    r shouldBe "This is a test"
  }
  test("dashes: at most 3 dashes are rendered as –(U+2013)") {
    val Right((_, r)) = LatexParser.parse(
      "5-1 is between 1--10---obviously. ----anonymous"
    )
    r shouldBe "5−1 is between 1–10—obviously. ----anonymous"
  }
  test("dashes: 4 or more dashes are kept as is") {
    val Right((_, r)) = LatexParser.parse(
      "----four-----five------six"
    )
    r shouldBe "----four-----five------six"
  }
  test("\\S") {
    val Right((_, r)) = LatexParser.parse(
      "\\S"
    )
    r shouldBe "§"
  }
  test("\\S\\P") {
    val Right((_, r)) = LatexParser.parse(
      "\\S\\P"
    )
    r shouldBe "§¶"
  }
  test("\\{t") {
    val Right((_, r)) = LatexParser.parse(
      "\\{t"
    )
    r shouldBe "{t"
  }
  test("Latex escape characters render as unicode symbols") {
    val Right((_, r)) = LatexParser.parse("""\S\{this is ~$\alpha$~ test\}""")
    r shouldBe "§{this is  α  test}"
  }
  test(
    "plain string surrounded by unescaped `brackets` becomes a block, but does not change render result"
  ) {
    val Right((_, r)) = LatexParser.parse("Thi{s} is a test")
    r shouldBe "This is a test"
  }

  test("unescaped brackets are not rendered") {
    val Right((_, r)) = LatexParser.parse(
      "{this {{i}{s}}{ a \n\n} test}"
    )

    r shouldBe "this is a\n\n test"
  }

  test("\\not") {
    val Right((_, r)) = LatexParser.parse("""\not 1""")
    r shouldBe "1̸"
    val Right((_, a)) = LatexParser.parse("""\not{123}""")
    a shouldBe "1̸23"
    val Right((_, not123)) = LatexParser.parse("""\not{ 123 }""")
    not123 shouldBe "1̸23"
    val Right((_, notin)) = LatexParser.parse("""\not \in""")
    notin shouldBe "∉"
    //L2U.parse2("""\not=""").toOption.get._2 shouldBe "≠"
    val Right((_, notbrace)) = LatexParser.parse("""\not{}""")
    notbrace shouldBe " ̸"
  }

  test("\\frac") {
    LatexParser.parse("\\frac{}{}").toOption.get._2 shouldBe ""
    LatexParser.parse("\\frac34").toOption.get._2 shouldBe "¾"
    LatexParser.parse("\\frac{3}4").toOption.get._2 shouldBe "¾"
    LatexParser.parse("\\frac3{4}").toOption.get._2 shouldBe "¾"
    LatexParser.parse("\\frac 3 {4}").toOption.get._2 shouldBe "¾"
    LatexParser.parse("\\frac 34").toOption.get._2 shouldBe "¾"
    LatexParser.parse("\\frac{1}{}").toOption.get._2 shouldBe "(1/)"
    LatexParser.parse("\\frac{}{1}").toOption.get._2 shouldBe "(/1)"
    LatexParser.parse("\\frac{a+b}{c}").toOption.get._2 shouldBe "((a+b)/c)"
    LatexParser.parse("\\frac{a}{b+c}").toOption.get._2 shouldBe "(a/(b+c))"
    LatexParser
      .parse("\\frac{a+b}{c+d}")
      .toOption
      .get
      ._2 shouldBe "((a+b)/(c+d))"
    LatexParser.parse("\\frac~{4}").toOption.get._2 shouldBe "(/4)"
    LatexParser.parse("\\frac${4}").toOption.get._2 shouldBe "(/4)"

    /*
    LaTeX2Unicode.convert("\\frac\n34") shouldBe "¾"
    LaTeX2Unicode.convert(
      "\\frac{\\hat\\alpha_1^2}{test\\_test}"
    ) shouldBe "(α̂₁²/test_test)"
     */
  }

  test("\\sqrt") {
    LatexParser.expr.parse("\\sqrt1").toOption.get._2 shouldBe "√1̅"
    LatexParser.parse("\\sqrt{}").toOption.get._2 shouldBe "√"
    LatexParser.parse("\\sqrt x").toOption.get._2 shouldBe "√x̅"
    LatexParser.parse("\\sqrt \\alpha").toOption.get._2 shouldBe "√α̅"
    //LatexParser.parse("\\sqrt\nx").toOption.get._2 shouldBe "√x̅"
    LatexParser.parse("\\sqrt[]x").toOption.get._2 shouldBe "√x̅"
    LatexParser.parse("\\sqrt[]\nx").toOption.get._2 shouldBe "√x̅"
    LatexParser.parse("\\sqrt{x+1}").toOption.get._2 shouldBe "√x̅+̅1̅"
    LatexParser.parse("\\sqrt2").toOption.get._2 shouldBe "√2̅"
    LatexParser.parse("\\sqrt1+1").toOption.get._2 shouldBe "√1̅+1"
    LatexParser.parse("\\sqrt[2]x").toOption.get._2 shouldBe "√x̅"
    LatexParser.parse("\\sqrt[3]{x}").toOption.get._2 shouldBe "∛x̅"
    LatexParser.parse("\\sqrt[3]{}").toOption.get._2 shouldBe "∛"
    //LatexParser.parse("\\sqrt[\\alpha+1]x").toOption.get._2 shouldBe "ᵅ⁺¹√x̅"
    //LatexParser.parse("\\sqrt[\\alpha+1]\nx").toOption.get._2 shouldBe "ᵅ⁺¹√x̅"
    LatexParser.parse("\\sqrt[q]{x}").toOption.get._2 shouldBe "(q)√x̅"
  }

  test("Subscript") {
    LatexParser.parse("i_{}").toOption.get._2 shouldBe "i"
    LatexParser.parse("i_123").toOption.get._2 shouldBe "i₁23"
    LatexParser.parse("{}_1").toOption.get._2 shouldBe "₁"
    LatexParser.parse("i_{123}").toOption.get._2 shouldBe "i₁₂₃"
    LatexParser.parse("i_  {123}").toOption.get._2 shouldBe "i₁₂₃"
    LatexParser.parse("i_\n  {123}").toOption.get._2 shouldBe "i₁₂₃"
    LatexParser.parse("i\\textsubscript{123}").toOption.get._2 shouldBe "i₁₂₃"
    LatexParser.parse("i\\textsubscript 123").toOption.get._2 shouldBe "i₁23"
    LatexParser.parse("i_{i_{123 }}").toOption.get._2 shouldBe "i_(i₁₂₃)"
    LatexParser.parse("i_{i_{1~2~3 }}").toOption.get._2 shouldBe "i_(i₁ ₂ ₃)"
    LatexParser
      .parse(
        "i\\textsubscript{i\\textsubscript{123 }}"
      )
      .toOption
      .get
      ._2 shouldBe "i_(i₁₂₃)"
    /*LaTeX2Unicode.convert("i\\textsubscript\n  { 123 }") shouldBe "i₁₂₃"*/
  }

  test("Superscript") {

    LatexParser.parse("i^{}").toOption.get._2 shouldBe "i"
    LatexParser.parse("i^{ }").toOption.get._2 shouldBe "i"
    LatexParser.parse("i^{  }").toOption.get._2 shouldBe "i"
    LatexParser.parse("i^{123}").toOption.get._2 shouldBe "i¹²³"
    LatexParser.parse("i^\n {123}").toOption.get._2 shouldBe "i¹²³"
    LatexParser.parse("i^{i^{1~2~3 }}").toOption.get._2 shouldBe "i^(i¹ ² ³)"
    LatexParser.parse("i\\textsuperscript 123").toOption.get._2 shouldBe "i¹23"
    LatexParser.parse("i\\textsuperscript{123}").toOption.get._2 shouldBe "i¹²³"
    LatexParser
      .parse(
        "i\\textsuperscript{i\\textsuperscript{123 }}"
      )
      .toOption
      .get
      ._2 shouldBe "i^(i¹²³)"
    /*
    LaTeX2Unicode.convert("i\\textsuperscript\n  { 123 }") shouldBe "i¹²³"*/
  }
  test("Combining") {
    LatexParser.parse("\\bar ab").toOption.get._2 shouldBe "a\u0304b"
    LatexParser.parse("\\bar{ab}").toOption.get._2 shouldBe "a\u0304b"

    LatexParser.parse("\\bar12").toOption.get._2 shouldBe "1\u03042"
    LatexParser.parse("\\bar{}").toOption.get._2 shouldBe " \u0304"

    /*   LatexParser.parse("\\=ab").toOption.get._2 shouldBe "a\u0304b"
    LatexParser.parse("\\=\nab").toOption.get._2 shouldBe "a\u0304b"
    LatexParser.parse("\\={}").toOption.get._2 shouldBe " \u0304"
    LatexParser.parse("\\={ab}").toOption.get._2 shouldBe "a\u0304b"
    LatexParser.parse(
      "\\=\\k\\underline\\overline{a\\=bc}"
    ) shouldBe "a\u0305\u0332\u0304b\u0304\u0305\u0332c\u0305\u0332\u0328"
     */
    LatexParser.parse("\\bar").isLeft shouldBe true
    LatexParser.parse("\\bar ").isLeft shouldBe true
    LatexParser.parse("\\bar \n\n").isLeft shouldBe true
    LatexParser.parse("\\=").isLeft shouldBe true
    LatexParser.parse("\\= ").isLeft shouldBe true
    LatexParser.parse("\\= \n\n").isLeft shouldBe true
    LatexParser.parse("\\= \n\nx").isLeft shouldBe true
  }

  test("Style") {
    val Right((_, empty)) = LatexParser.parse("\\mathbf{}")
    empty shouldBe ""
    val Right((_, abcabc)) = LatexParser
      .parse("\\mathbf ABC \\mathit ABC")
    abcabc shouldBe "𝐀BC 𝐴BC"
    /* LatexParser.parse(
      "\\mathbf {ABC} \\mathit {ABC}"
    ).toOption.get._2 shouldBe "𝐀𝐁𝐂 𝐴𝐵𝐶"
    LatexParser.parse("\\bf \\it ").toOption.get._2 shouldBe ""
    LatexParser.parse(
      "ABC {\\bf ABC} {\\it ABC} ABC"
    ) shouldBe "ABC 𝐀𝐁𝐂 𝐴𝐵𝐶 ABC"
    LatexParser.parse(
      "ABC \\bf ABC \\it ABC ABC"
    ).toOption.get._2 shouldBe "ABC 𝐀𝐁𝐂 𝐴𝐵𝐶 𝐴𝐵𝐶"
    LatexParser.parse("A\\bf\n\nB\n\nC").toOption.get._2 shouldBe "A\n\n𝐁\n\n𝐂"*/

    LatexParser.parse("\\mathbf").isLeft shouldBe true
    LatexParser.parse("\\mathbf ").isLeft shouldBe true
    LatexParser.parse("\\mathbf \n\n").isLeft shouldBe true
    LatexParser.parse("\\mathbf \n\nx").isLeft shouldBe true
  }

  /*
  test("Unknown commands") {
    LaTeX2Unicode.convert(
      "\\this \\is \\alpha test"
    ) shouldBe "\\this \\is α test"
    LaTeX2Unicode.convert("\\unknown command") shouldBe "\\unknown command"
    LaTeX2Unicode.convert(
      "\\unknown{} empty params"
    ) shouldBe "\\unknown{} empty params"
    LaTeX2Unicode.convert("\\unknown{cmd}") shouldBe "\\unknown{cmd}"
    LaTeX2Unicode.convert("\\unknown{1}{2}") shouldBe "\\unknown{1}{2}"
    LaTeX2Unicode.convert("\\unknown{1}{2}{3}") shouldBe "\\unknown{1}{2}{3}"
  }*/
}
