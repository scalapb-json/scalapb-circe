package scalapb_circe

import io.circe.Json
import scalapb_circe.ProtoMacrosCirce.*
import scalaprops.Gen
import scalaprops.Property
import scalaprops.Scalaprops
import scala.quoted.Expr
import scala.quoted.FromExpr
import scala.quoted.ToExpr
import scala.quoted.Quotes
import scala.quoted.staging.Compiler
import scala.quoted.staging.run
import scala.quoted.staging.withQuotes

object ProtoMacrosCommonTest extends Scalaprops {
  private[this] implicit val jsonGen: Gen[Json] = {
    val primitive: Gen[Json] = Gen.oneOf(
      Gen.value(Json.Null),
      Gen.value(Json.True),
      Gen.value(Json.False),
      Gen[Long].map(Json.fromLong),
      Gen[Double].map(Json.fromDoubleOrString),
      Gen[Float].map(Json.fromFloatOrString),
//      Gen[BigInt].map(Json.fromBigInt),
//      Gen[BigDecimal].map(Json.fromBigDecimal),
      Gen.alphaNumString.map(Json.fromString)
    )

    val list1: Gen[Json] = Gen.listOf(primitive).map(Json.arr)
    val map1: Gen[Json] = Gen.mapGen(Gen.alphaNumString, primitive).map(x => Json.obj(x.toList: _*))

    Gen.oneOf(
      primitive,
      list1,
      map1,
    )
  }

  val test: Property = {
    given Compiler = Compiler.make(getClass.getClassLoader)
    val x1: Quotes => Property = run(testImpl)
    val x2: Quotes ?=> Property = x1.apply(summon[Quotes])
    withQuotes(x2)
  }

  private[this] def testImpl(using Quotes): Expr[Quotes => Property] = '{ implicit q: Quotes =>
    {
      Property.forAll { (x1: Json) =>
        val x2 = summon[FromExpr[Json]].unapply(Expr(x1))
        assert(Some(x1) == x2, s"$x1 != $x2")
        true
      }
    }
  }
}
