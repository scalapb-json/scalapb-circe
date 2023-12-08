package scalapb_circe

import scalapb.TypeMapper

case class ErrorIfZero(value: Int) {
  assert(value != 0, "should not zero")
}

object ErrorIfZero {
  implicit def instance: TypeMapper[Int, ErrorIfZero] =
    new TypeMapper[Int, ErrorIfZero] {
      override def toCustom(x: Int): ErrorIfZero = ErrorIfZero(x)
      override def toBase(x: ErrorIfZero): Int = x.value
    }
}
