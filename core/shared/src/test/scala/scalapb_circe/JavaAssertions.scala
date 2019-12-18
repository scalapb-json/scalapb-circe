package scalapb_circe

import scalapb_json.ScalapbJsonCommon.GenericCompanion
import scalapb.GeneratedMessageCompanion
import org.scalatest.matchers.must.Matchers
import scalapb_json._

trait JavaAssertions extends JavaAssertionsPlatform {
  self: Matchers =>

  def registeredCompanions: Seq[GeneratedMessageCompanion[_]] = Seq.empty

  val ScalaTypeRegistry = registeredCompanions.foldLeft(TypeRegistry.empty)(
    (r, c) => r.addMessageByCompanion(c.asInstanceOf[GenericCompanion])
  )
  val ScalaJsonParser = new Parser(typeRegistry = ScalaTypeRegistry)
  val ScalaJsonPrinter = new Printer(typeRegistry = ScalaTypeRegistry)

}
