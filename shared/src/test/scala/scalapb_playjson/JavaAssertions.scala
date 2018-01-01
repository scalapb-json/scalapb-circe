package scalapb_playjson

import JsonFormat.GenericCompanion
import com.trueaccord.scalapb.GeneratedMessageCompanion
import org.scalatest.MustMatchers

trait JavaAssertions extends JavaAssertionsPlatform {
  self: MustMatchers =>

  def registeredCompanions: Seq[GeneratedMessageCompanion[_]] = Seq.empty

  val ScalaTypeRegistry = registeredCompanions.foldLeft(TypeRegistry.empty)((r, c) =>
    r.addMessageByCompanion(c.asInstanceOf[GenericCompanion]))
  val ScalaJsonParser = new Parser(typeRegistry = ScalaTypeRegistry)
  val ScalaJsonPrinter = new Printer(typeRegistry = ScalaTypeRegistry)

}
