package scalapb_playjson

import com.google.protobuf.any.{Any => PBAny}
import org.scalatest.{FlatSpec, MustMatchers}
import jsontest.anytests.AnyTest

class AnyFormatSpecJVM extends FlatSpec with MustMatchers with JavaAssertions {

  override def registeredCompanions = Seq(AnyTest)

  "Any" should "be serialized the same as in Java (and parsed back to original)" in {
    val RawExample = AnyTest("test")
    val AnyExample = PBAny.pack(RawExample)
    assertJsonIsSameAsJava(AnyExample)
  }
}
