package core

import nl.grons.metrics.scala.Timer
import java.util.UUID
import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.Serialization.{read, writePretty}
import spray.httpx.Json4sSupport
import spray.httpx.marshalling._
import spray.httpx.unmarshalling._
import spray.http.{ContentType, ContentTypeRange, HttpEntity, MediaType, MediaTypes}

case class Metrics(
    startedCount: Long,
    successCount: Long,
    failedCount: Long,
    min: Long,
    max: Long,
    mean: Double,
    stdDev: Double,
    oneMinuteRate: Double,
    load: Double)

trait MetricsFormats extends BaseFormats {
    lazy val `application/vnd.enpassant.metrics+json` =
        MediaTypes.register(MediaType.custom("application/vnd.enpassant.metrics+json"))

    implicit val MetricsUnmarshaller = Unmarshaller.oneOf(
        unmarshal[Metrics](`application/vnd.enpassant.metrics+json`),
        unmarshal[Metrics](MediaTypes.`application/json`))

    implicit val MetricsMarshaller = marshal[Metrics](
        `application/vnd.enpassant.metrics+json`,
        MediaTypes.`application/json`)
}

object Metrics {
    def apply(timer: Timer, startedCount: Long, failedCount: Long): Metrics = Metrics(
        startedCount,
        timer.count,
        failedCount,
        timer.min / 1000000,
        timer.max / 1000000,
        timer.mean / 1000000,
        timer.stdDev / 1000000,
        timer.oneMinuteRate,
        timer.max / 1000000 * (startedCount - timer.count - failedCount))
}
// vim: set ts=4 sw=4 et:
