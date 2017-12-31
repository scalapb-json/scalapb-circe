package scalapb_playjson

import com.google.protobuf.wrappers.UInt64Value
import jsontest.test3.{MyTest3, Test3Proto, Wrapper}
import org.scalatest.{FlatSpec, MustMatchers, OptionValues}

class TypeRegistrySpec extends FlatSpec with MustMatchers with OptionValues {
  "addFile" should "add all messages in the file" in {
    val reg = TypeRegistry().addFile(Test3Proto)

    reg.findType("type.googleapis.com/jsontest.MyTest3").value must be(MyTest3)
    reg.findType("type.googleapis.com/jsontest.Wrapper").value must be(Wrapper)
    reg.findType("type.googleapis.com/google.protobuf.UInt64Value").value must be(UInt64Value)
    reg.findType("type.googleapis.com/something.else") must be(None)
  }

  "addMessage" should "not add other messages from same file" in {
    val reg = TypeRegistry().addMessage[MyTest3]
    reg.findType("type.googleapis.com/jsontest.MyTest3").value must be(MyTest3)
    reg.findType("type.googleapis.com/jsontest.Wrapper") must be(None)
  }

}
