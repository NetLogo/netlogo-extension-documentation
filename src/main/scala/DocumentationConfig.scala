package org.nlogo.build

case class DocumentationConfig(
  markdownTemplate: String,
  primTemplate: String,
  tableOfContents: Map[String, String])
