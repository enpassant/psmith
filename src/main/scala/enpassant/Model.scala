package enpassant

import core.{Instrumented, Metrics, MetricsStatItem, MetricsStatMap, MicroService}

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.Future

case class GetServices()
case class GetService(serviceId: String)
case class PutService(serviceId: String, service: MicroService)
case class DeleteService(serviceId: String)
case class SetServices(services: List[MicroService])

case class Started(service: Option[MicroService])
case class Failed(service: Option[MicroService])
case class Latency(time: Long, service: Option[MicroService])
case class GetMetrics()

class Model(val mode: Option[String]) extends Actor with ActorLogging {
    import context.dispatcher

    val managedHeaders = List("Host", "Server", "Date", "Content-Type",
        "Content-Length", "Transfer-Encoding", "Timeout-Access")

    private def stripHeaders(headers: Seq[HttpHeader]):
        Seq[HttpHeader] = headers.filterNot(h => managedHeaders.contains(h.name))

    implicit val timeout = Timeout(3.seconds)
    implicit val system = context.system
    implicit val materializer = ActorMaterializer()

    def receive = {
        case GetServices =>
            sender ! Model.services.values.flatMap(_.list.map(_._1))

        case GetService(serviceId) =>
            sender ! Model.findServiceById(serviceId)

        case PutService(serviceId, microService) =>
            Model.deleteServices(microService)

            val service = Model.findServiceById(serviceId)
            if (service == None) {
                def pipeline: Route =
                  Route { context =>
                    val request = context.request
                    val updatedUri = request.uri
                      .withHost(microService.host)
                      .withPort(microService.port)
                      .withPath(Path./)
                    val updatedRequest = request.copy(uri = updatedUri,
                      headers = stripHeaders(request.headers))
                      //println("Opening connection to " + request.uri.authority.host.address)
                      println("Opening connection to " + microService.host + ":" + microService.port)
                    val flow = Http().outgoingConnection(microService.host, microService.port)
                    val handler = Source.single(updatedRequest)
                      //.map(r => r.withHeaders(RawHeader("x-authenticated", "someone")))
                      .via(flow)
                      .runWith(Sink.head)
                      .flatMap(context.complete(_))
                    handler
                  }
                val key = Model.name(microService.path, microService.runningMode)
                if (Model.services contains key) {
                    Model.services(key) = Pipelines(key, (microService, Pipeline(microService.uuid, pipeline)) ::
                        Model.services(key).list)
                } else {
                    Model.services(key) = Pipelines(key, List((microService, Pipeline(microService.uuid, pipeline))))
                }
            }
            sender ! microService

        case DeleteService(serviceId) =>
            Model.findServiceById(serviceId) match {
                case Some(service) =>
                    Model.deleteServices(service)
                case None =>
            }
            sender ! ""

        case Started(service) =>
            Model.startedCounter.inc()
            service match {
                case Some(ms) =>
                    val pipelines = Model.findServices(ms.path, ms.runningMode)
                    pipelines.startedCounter.inc()
                    val pipeline = pipelines.list.find(_._1.uuid == ms.uuid)
                    pipeline map { _._2.startedCounter.inc() }
                case _ =>
            }

        case Failed(service) =>
            Model.failedCounter.inc()
            service match {
                case Some(ms) =>
                    val pipelines = Model.findServices(ms.path, ms.runningMode)
                    pipelines.failedCounter.inc()
                    val pipeline = pipelines.list.find(_._1.uuid == ms.uuid)
                    pipeline map { _._2.failedCounter.inc() }
                case _ =>
            }

        case Latency(time, service) =>
            Model.requestLatency.update(time, TimeUnit.MILLISECONDS)
            service match {
                case Some(ms) =>
                    val pipelines = Model.findServices(ms.path, ms.runningMode)
                    pipelines.requestLatency.update(time, TimeUnit.MILLISECONDS)
                    val pipeline = pipelines.list.find(_._1.uuid == ms.uuid)
                    pipeline map { _._2.requestLatency.update(time, TimeUnit.MILLISECONDS) }
                case _ =>
            }

        case GetMetrics =>
            sender ! Model.getAllMetrics
    }
}

case class Pipeline(id: String, route: Route) extends Instrumented {
    val startedCounter = metrics.counter("startedCounter." + id)
    val failedCounter = metrics.counter("failedCounter." + id)
    val requestLatency = metrics.timer("requestLatency." + id)
}

case class Pipelines(id: String, list: List[(MicroService, Pipeline)]) extends Instrumented {
    val startedCounter = metrics.counter("startedCounter." + id)
    val failedCounter = metrics.counter("failedCounter." + id)
    val requestLatency = metrics.timer("requestLatency." + id)
}

object Model extends Instrumented {
    type Collection = scala.collection.mutable.Map[String, Pipelines]

    private var services: Model.Collection = scala.collection.mutable.Map.empty[String, Pipelines]

    private val startedCounter = metrics.counter("startedCounter")
    private val failedCounter = metrics.counter("failedCounter")
    private val requestLatency = metrics.timer("requestLatency")

    def props(mode: Option[String]) = Props(new Model(mode))
    def name = "model"

    def getAllMetrics: MetricsStatMap = {
        MetricsStatMap(Map(("system" ->
            MetricsStatItem(Metrics(requestLatency, startedCounter, failedCounter)))) ++
                services.map {
                    kv: (String, Pipelines) =>
                        val (k, v) = kv
                        (k -> MetricsStatMap(Map(("all" ->
                            MetricsStatItem(Metrics(v.requestLatency, v.startedCounter, v.failedCounter)))) ++
                            v.list.map {
                                case (ms, p) => (ms.uuid,
                                    MetricsStatItem(Metrics(p.requestLatency, p.startedCounter, p.failedCounter)))
                            }.toMap
                        ))
                })
    }

    def getAllServices = {
        services.values.flatMap(_.list.map(_._1))
    }

    def findServiceById(serviceId: String) = {
        getAllServices.find(_.uuid == serviceId)
    }

    def findServices(path: String, runningMode: Option[String]): Pipelines = {
        val key = name(path, runningMode)
        val keyNone = name(path, None)
        if (services contains key) {
            services(key)
        } else if (runningMode != None && services.contains(keyNone)) {
            services(keyNone)
        } else {
            Pipelines("", List())
        }
    }

    def name(path: String, runningMode: Option[String]) = runningMode match {
        case Some(mode) => s"${path}-${mode}"
        case None => s"${path}"
    }

    private def deleteServices(service: MicroService) = {
        val key = name(service.path, service.runningMode)
        if (services contains key) {
            val pipelines = services(key).list filterNot {
                case (s, p) => (service.uuid == s.uuid) ||
                    (service.host == s.host && service.port == s.port)
            }
            if (pipelines.isEmpty) services.remove(key)
            else services(key) = Pipelines(key, pipelines)
        }
    }
}
// vim: set ts=4 sw=4 et:
