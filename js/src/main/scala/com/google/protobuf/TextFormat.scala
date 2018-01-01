package com.google.protobuf

object TextFormat {

  /**
   * [[https://github.com/google/protobuf/blob/v3.5.1/java/core/src/main/java/com/google/protobuf/TextFormat.java#L1111-L1136]]
   */
  class ParseException(
    line: Int,
    column: Int,
    message: String
  ) extends java.io.IOException(s"${line}:${column}: ${message}") {

    def this(message: String) {
      this(-1, -1, message)
    }
  }
}
