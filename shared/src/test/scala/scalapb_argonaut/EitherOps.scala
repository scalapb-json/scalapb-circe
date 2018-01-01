package scalapb_argonaut

import scala.language.implicitConversions

class EitherOps[A](val self: Either[String, A]) extends AnyVal {
  def getOrError: A = self match {
    case Right(a) => a
    case Left(a) => sys.error(a)
  }
}

object EitherOps {
  implicit def toEitherOps[A](self: Either[String, A]): EitherOps[A] =
    new EitherOps[A](self)
}
