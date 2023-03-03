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
      def decodeError(x: String) = s"DecodingFailure at : Got value '${x}' with wrong type, expecting object"
      checkTypeError(""" struct"null" """, decodeError("null"))
      checkTypeError(""" struct"[3]" """, decodeError("[3]"))
      checkTypeError(""" struct"true" """, decodeError("true"))
      checkTypeError(""" struct"12345" """, decodeError("12345"))

      checkTypeError(""" struct" ] " """, "ParsingFailure: expected json value got '] ' (line 1, column 2)")
    }

    it("value") {
      checkTypeError(""" value" ] " """, "ParsingFailure: expected json value got '] ' (line 1, column 2)")
      checkTypeError(""" value" } " """, "ParsingFailure: expected json value got '} ' (line 1, column 2)")
    }
  }
}
