package scalapb_circe

import scalapb.e2e.repeatables.RepeatablesTest
import scalapb.e2e.repeatables.RepeatablesTest.Nested
import scalaprops.Gen
import scalaprops.Scalaprops
import scalaprops.Property.forAll

object RepeatablesSpec extends Scalaprops {
  private[this] implicit val doubleGen: Gen[Double] =
    Gen.genFiniteDouble

  private[this] implicit val stringGen: Gen[String] =
    Gen.alphaNumString

  private[this] implicit val nestedGen: Gen[Nested] =
    Gen.from1(Nested.of)

  private[this] implicit val enumGen: Gen[RepeatablesTest.Enum] =
    Gen.elements(RepeatablesTest.Enum.values.head, RepeatablesTest.Enum.values.tail: _*)

  private[this] implicit def seqGen[A: Gen]: Gen[Seq[A]] =
    Gen.gen(Gen[Vector[A]].f)

  private[this] implicit val repGen: Gen[RepeatablesTest] =
    Gen.from6(RepeatablesTest.of)

  val `fromJson invert toJson single` = forAll {
    val rep = RepeatablesTest(
      strings = Seq("s1", "s2"),
      ints = Seq(14, 19),
      doubles = Seq(3.14, 2.17),
      nesteds = Seq(Nested())
    )
    val j = JsonFormat.toJson(rep)
    JsonFormat.fromJson[RepeatablesTest](j) == rep
  }

  val `fromJson invert toJson` = forAll { rep: RepeatablesTest =>
    val j = JsonFormat.toJson(rep)
    JsonFormat.fromJson[RepeatablesTest](j) == rep
  }
}
