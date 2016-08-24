package org.nlogo.build

case class Warning(message: String, lineNumber: Int)
case class ParsingResult(primitives: Seq[Primitive], warnings: Seq[Warning])
