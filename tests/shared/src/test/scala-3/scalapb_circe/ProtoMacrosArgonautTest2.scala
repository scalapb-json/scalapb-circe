package scalapb_circe

import scala.compiletime.testing.{typeCheckErrors, ErrorKind}
import org.scalatest.funspec.AnyFunSpec
import scalapb_circe.ProtoMacrosCirce._

class ProtoMacrosCirceTest2 extends AnyFunSpec {

  describe("ProtoMacrosCirce scala 3 test") {
    inline def checkTypeError(
      src: String,
      expectMessage: String
    ) = {
      typeCheckErrors(src) match {
        case List(e) =>
          assert(e.kind == ErrorKind.Typer)
          assert(e.message == expectMessage)
        case other =>
          fail("unexpected " + other)
      }
    }

    it("struct") {
      val decodeError = "DecodingFailure at : JsonObject"
      checkTypeError(""" struct"null" """, decodeError)
      checkTypeError(""" struct"[3]" """, decodeError)
      checkTypeError(""" struct"true" """, decodeError)
      checkTypeError(""" struct"12345" """, decodeError)

      checkTypeError(""" struct" ] " """, "ParsingFailure: expected json value got '] ' (line 1, column 2)")
    }

    it("value") {
      checkTypeError(""" value" ] " """, "io.circe.ParsingFailure: expected json value got '] ' (line 1, column 2)")
      checkTypeError(""" value" } " """, "io.circe.ParsingFailure: expected json value got '} ' (line 1, column 2)")
    }
  }
}
