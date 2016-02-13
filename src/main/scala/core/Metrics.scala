package core

import nl.grons.metrics.scala.{Counter, Timer}
import java.util.UUID
import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.Serialization.{read, writePretty}
//import spray.httpx.Json4sSupport
//import spray.httpx.marshalling._
//import spray.httpx.unmarshalling._
//import spray.http.{ContentType, ContentTypeRange, HttpEntity, MediaType, MediaTypes}

case class Metrics(
    startedCount: Long,
    successCount: Long,
    failedCount: Long,
    min: Long,
    max: Long,
    mean: Double,
    stdDev: Double,
    oneMinuteRate: Double)
{
    def minLoad = min / 1000000 * (startedCount - successCount - failedCount)
    def meanLoad = mean / 1000000 * (startedCount - successCount - failedCount)
    def maxLoad = max / 1000000 * (startedCount - successCount - failedCount)
}

sealed trait MetricsStat
//case class MetricsStatItem(metrics: Metrics) extends MetricsStat
//case class MetricsStatMap(map: Map[String, MetricsStat]) extends MetricsStat

trait MetricsFormats extends BaseFormats {
    //lazy val `application/vnd.enpassant.metrics+json` =
        //MediaTypes.register(MediaType.custom("application/vnd.enpassant.metrics+json"))

    //lazy val `application/vnd.enpassant.metricstat+json` =
        //MediaTypes.register(MediaType.custom("application/vnd.enpassant.metricstat+json"))

    //implicit val MetricsUnmarshaller = Unmarshaller.oneOf(
        //unmarshal[Metrics](`application/vnd.enpassant.metrics+json`),
        //unmarshal[Metrics](MediaTypes.`application/json`))

    //implicit val MetricsMarshaller = marshal[Metrics](
        //`application/vnd.enpassant.metrics+json`,
        //MediaTypes.`application/json`)

    //implicit val MetricsStatUnmarshaller = Unmarshaller.oneOf(
        //unmarshal[MetricsStat](`application/vnd.enpassant.metricstat+json`),
        //unmarshal[MetricsStat](MediaTypes.`application/json`))

    //implicit val MetricsStatMarshaller = marshal[MetricsStat](
        //`application/vnd.enpassant.metricstat+json`,
        //MediaTypes.`application/json`)
}

object Metrics {
    def apply(timer: Timer, startedCounter: Counter, failedCounter: Counter): Metrics = Metrics(
        startedCounter.count,
        timer.count,
        failedCounter.count,
        timer.min / 1000000,
        timer.max / 1000000,
        timer.mean / 1000000,
        timer.stdDev / 1000000,
        timer.oneMinuteRate)
}
// vim: set ts=4 sw=4 et:
