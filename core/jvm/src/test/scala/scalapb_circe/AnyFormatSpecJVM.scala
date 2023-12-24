package scalapb_circe

import com.google.protobuf.any.{Any => PBAny}
import jsontest.anytests.AnyTest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import scalapb.GeneratedMessageCompanion

class AnyFormatSpecJVM extends AnyFlatSpec with Matchers with JavaAssertions {

  override def registeredCompanions: Seq[GeneratedMessageCompanion[?]] = Seq(AnyTest)

  "Any" should "be serialized the same as in Java (and parsed back to original)" in {
    val RawExample = AnyTest("test")
    val AnyExample = PBAny.pack(RawExample)
    assertJsonIsSameAsJava(AnyExample)
  }
}
