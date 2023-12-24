package scalapb_circe

import scalapb.GeneratedMessageCompanion

private[scalapb_circe] object ScalapbCirceCompat {
  def getClassFromMessageCompanion(x: GeneratedMessageCompanion[?]): Class[?] =
    x.getClass.getMethod("defaultInstance").getReturnType
}
