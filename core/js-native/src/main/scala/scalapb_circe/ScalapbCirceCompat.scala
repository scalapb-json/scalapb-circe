package scalapb_circe

import scalapb.GeneratedMessageCompanion

private[scalapb_circe] object ScalapbCirceCompat {
  def getClassFromMessageCompanion(x: GeneratedMessageCompanion[_]): Class[_] =
    x.defaultInstance.getClass
}
