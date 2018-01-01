package scalapb_argonaut

import com.google.protobuf.ByteString
import jsontest.test3._
import argonaut._
import org.scalatest.{FlatSpec, MustMatchers}

class PrimitiveWrappersSpec extends FlatSpec with MustMatchers {

  private[this] def render[A](a: A)(implicit A: EncodeJson[A]): Json =
    A.apply(a)

  "Empty object" should "give empty json for Wrapper" in {
    JsonFormat.toJson(Wrapper()) must be(render(Map.empty[String, Json]))
  }

  "primitive values" should "serialize properly" in {
    JsonFormat.toJson(Wrapper(wBool = Some(false))) must be(
      render(Map("wBool" -> Json.jBool(false))))
    JsonFormat.toJson(Wrapper(wBool = Some(true))) must be(render(Map("wBool" -> Json.jBool(true))))
    JsonFormat.toJson(Wrapper(wDouble = Some(3.1))) must be(render(Map("wDouble" -> Json.jNumber(3.1))))
    JsonFormat.toJson(Wrapper(wFloat = Some(3.0f))) must be(render(Map("wFloat" -> Json.jNumber(3.0))))
    JsonFormat.toJson(Wrapper(wInt32 = Some(35544))) must be(
      render(Map("wInt32" -> Json.jNumber(35544))))
    JsonFormat.toJson(Wrapper(wInt32 = Some(0))) must be(render(Map("wInt32" -> Json.jNumber(0))))
    JsonFormat.toJson(Wrapper(wInt64 = Some(125))) must be(render(Map("wInt64" -> Json.jString("125"))))
    JsonFormat.toJson(Wrapper(wUint32 = Some(125))) must be(render(Map("wUint32" -> Json.jNumber(125))))
    JsonFormat.toJson(Wrapper(wUint64 = Some(125))) must be(
      render(Map("wUint64" -> Json.jString("125"))))
    JsonFormat.toJson(Wrapper(wString = Some("bar"))) must be(
      render(Map("wString" -> Json.jString("bar"))))
    JsonFormat.toJson(Wrapper(wString = Some(""))) must be(render(Map("wString" -> Json.jString(""))))
    JsonFormat.toJson(Wrapper(wBytes = Some(ByteString.copyFrom(Array[Byte](3, 5, 4))))) must be(
      render(Map("wBytes" -> Json.jString("AwUE"))))
    JsonFormat.toJson(Wrapper(wBytes = Some(ByteString.EMPTY))) must be(
      render(Map("wBytes" -> Json.jString(""))))
  }

  "primitive values" should "parse properly" in {
    JsonFormat.fromJson[Wrapper](render(Map("wBool" -> Json.jBool(false)))) must be(
      Wrapper(wBool = Some(false)))
    JsonFormat.fromJson[Wrapper](render(Map("wBool" -> Json.jBool(true)))) must be(
      Wrapper(wBool = Some(true)))
    JsonFormat.fromJson[Wrapper](render(Map("wDouble" -> Json.jNumber(3.1)))) must be(
      Wrapper(wDouble = Some(3.1)))
    JsonFormat.fromJson[Wrapper](render(Map("wFloat" -> Json.jNumber(3.0)))) must be(
      Wrapper(wFloat = Some(3.0f)))
    JsonFormat.fromJson[Wrapper](render(Map("wInt32" -> Json.jNumber(35544)))) must be(
      Wrapper(wInt32 = Some(35544)))
    JsonFormat.fromJson[Wrapper](render(Map("wInt32" -> Json.jNumber(0)))) must be(
      Wrapper(wInt32 = Some(0)))
    JsonFormat.fromJson[Wrapper](render(Map("wInt64" -> Json.jString("125")))) must be(
      Wrapper(wInt64 = Some(125)))
    JsonFormat.fromJson[Wrapper](render(Map("wUint32" -> Json.jNumber(125)))) must be(
      Wrapper(wUint32 = Some(125)))
    JsonFormat.fromJson[Wrapper](render(Map("wUint64" -> Json.jString("125")))) must be(
      Wrapper(wUint64 = Some(125)))
    JsonFormat.fromJson[Wrapper](render(Map("wString" -> Json.jString("bar")))) must be(
      Wrapper(wString = Some("bar")))
    JsonFormat.fromJson[Wrapper](render(Map("wString" -> Json.jString("")))) must be(
      Wrapper(wString = Some("")))
    JsonFormat.fromJson[Wrapper](render(Map("wBytes" -> Json.jString("AwUE")))) must be(
      Wrapper(wBytes = Some(ByteString.copyFrom(Array[Byte](3, 5, 4)))))
    JsonFormat.fromJson[Wrapper](render(Map("wBytes" -> Json.jString("")))) must be(
      Wrapper(wBytes = Some(ByteString.EMPTY)))
  }

}
