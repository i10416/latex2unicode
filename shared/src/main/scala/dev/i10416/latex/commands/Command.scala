package dev.i10416.latex2unicode.commands

trait UnaryCmd {
  val name: String
  def handleNoOpt: PartialFunction[String, String => String]
  def handleWithOpt: PartialFunction[(String, String), String => String]
}

trait BinaryCmd {
  val name: String
  def handleNoOpt: PartialFunction[String, (String, String) => String]
  def handleWithOpt
      : PartialFunction[(String, String), (String, String) => String]
}
