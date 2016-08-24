package org.nlogo.extensions

sealed trait FormattedString {
  def toMarkdown: String
  def toText: String
}

case class TextString(s: String) extends FormattedString {
  def toMarkdown: String = s
  def toText: String = s
}
