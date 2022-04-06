package dev.i10416.latex2unicode

import dev.i10416.latex2unicode.helper._
import org.scalatest._
import matchers.should.Matchers._
import org.scalatest.matchers
import org.scalatest.funsuite.AnyFunSuite

class DataTest extends AnyFunSuite {

  test("Command names should be trimmed") {
    for (
      names <- List(
        Escape.names,
        Unary.names,
        Style.names,
        UnaryWithOption.names,
        Binary.names
      );
      name <- names
    ) {
      name.trim should equal(name)
    }
  }

}
