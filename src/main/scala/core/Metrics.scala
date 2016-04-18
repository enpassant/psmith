package core

import nl.grons.metrics.scala.{Counter, Timer}
import java.util.UUID
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling._

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
case class MetricsStatItem(metrics: Metrics) extends MetricsStat
case class MetricsStatMap(map: Map[String, MetricsStat]) extends MetricsStat

trait MetricsFormats extends BaseFormats {
  lazy val `application/vnd.enpassant.metrics+json` =
    customMediaTypeUTF8("vnd.enpassant.metrics+json")

  lazy val `application/vnd.enpassant.metricstat+json`: MediaType.WithFixedCharset =
    MediaType.customWithFixedCharset(
      "application",
      "vnd.enpassant.metricstat+json",
      HttpCharsets.`UTF-8`
    )

  implicit val MetricsUnmarshaller = Unmarshaller.firstOf(
    unmarshaller[Metrics](`application/vnd.enpassant.metrics+json`),
    unmarshaller[Metrics](MediaTypes.`application/json`))

  implicit val MetricsMarshaller = Marshaller.oneOf(
    marshaller[Metrics](`application/vnd.enpassant.metrics+json`),
    marshaller[Metrics](MediaTypes.`application/json`))

  implicit val MetricsStatUnmarshaller = Unmarshaller.firstOf(
    unmarshaller[MetricsStat](`application/vnd.enpassant.metricstat+json`),
    unmarshaller[MetricsStat](MediaTypes.`application/json`))

  implicit val MetricsStatMarshaller = Marshaller.oneOf(
    marshaller[MetricsStat](`application/vnd.enpassant.metricstat+json`),
    marshaller[MetricsStat](MediaTypes.`application/json`))
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
