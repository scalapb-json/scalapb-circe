package scalapb_circe

import scala.language.implicitConversions

class EitherOps[A](val self: Either[Throwable, A]) extends AnyVal {
  def getOrError: A = self match {
    case Right(a) => a
    case Left(a) => throw a
  }
}

object EitherOps {
  implicit def toEitherOps[A](self: Either[Throwable, A]): EitherOps[A] =
    new EitherOps[A](self)
}
