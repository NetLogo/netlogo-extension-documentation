package org.nlogo.build

import scala.collection.JavaConverters._

case class DocumentationConfig(
  markdownTemplate: String,
  primTemplate: String,
  tableOfContents: Map[String, String],
  additionalConfig: Map[String, Object] = Map()) {

    def this(
      markdownTemplate: String,
      primTemplate: String,
      tableOfContents: Map[String, String],
      javaMap: java.util.Map[String, Object]) =
        this(markdownTemplate, primTemplate, tableOfContents, Map(javaMap.asScala.toSeq: _*))
  }
