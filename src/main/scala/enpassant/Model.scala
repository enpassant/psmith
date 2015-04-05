package enpassant

import core.{Instrumented, Metrics, MicroService}

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Future
import spray.can.Http
import spray.http._
import spray.client.pipelining._
import spray.routing.{HttpServiceActor, Route, ValidationRejection}
import spray.client.pipelining._

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

    def receive = {
        case GetServices =>
            sender ! Model.services.values.flatMap(_.list.map(_._1))

        case GetService(serviceId) =>
            sender ! Model.findServiceById(serviceId)

        case PutService(serviceId, microService) =>
            Model.deleteServices(microService)

            val service = Model.findServiceById(serviceId)
            if (service == None) {
                implicit val timeout = Timeout(3.seconds)
                implicit val system = context.system

                def pipeline: Future[SendReceive] =
                    for (
                        Http.HostConnectorInfo(connector, _) <-
                            IO(Http) ? Http.HostConnectorSetup(microService.host, port = microService.port)
                ) yield sendReceive(connector)
                val key = Model.name(microService.path, microService.runningMode)
                if (Model.services contains key) {
                    Model.services(key) = Pipelines((microService, Pipeline(pipeline)) :: Model.services(key).list)
                } else {
                    Model.services(key) = Pipelines(List((microService, Pipeline(pipeline))))
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
                    val pipeline = pipelines.list.find(_._1.uuid == service.get.uuid)
                    pipeline map { _._2.startedCounter.inc() }
                case _ =>
            }

        case Failed(service) =>
            Model.failedCounter.inc()
            service match {
                case Some(ms) =>
                    val pipelines = Model.findServices(ms.path, ms.runningMode)
                    pipelines.failedCounter.inc()
                    val pipeline = pipelines.list.find(_._1.uuid == service.get.uuid)
                    pipeline map { _._2.failedCounter.inc() }
                case _ =>
            }

        case Latency(time, service) =>
            Model.requestLatency.update(time, TimeUnit.MILLISECONDS)
            service match {
                case Some(ms) =>
                    val pipelines = Model.findServices(ms.path, ms.runningMode)
                    pipelines.requestLatency.update(time, TimeUnit.MILLISECONDS)
                    val pipeline = pipelines.list.find(_._1.uuid == service.get.uuid)
                    pipeline map { _._2.requestLatency.update(time, TimeUnit.MILLISECONDS) }
                case _ =>
            }

        case GetMetrics =>
            sender ! Metrics(Model.requestLatency,
                Model.startedCounter, Model.failedCounter)
    }
}

case class Pipeline(value: Future[SendReceive]) extends Instrumented {
    val startedCounter = metrics.counter("startedCounter")
    val failedCounter = metrics.counter("failedCounter")
    val requestLatency = metrics.timer("requestLatency")
}

case class Pipelines(list: List[(MicroService, Pipeline)]) extends Instrumented {
    val startedCounter = metrics.counter("startedCounter")
    val failedCounter = metrics.counter("failedCounter")
    val requestLatency = metrics.timer("requestLatency")
}

object Model extends Instrumented {
    type Collection = scala.collection.mutable.Map[String, Pipelines]

    private var services: Model.Collection = scala.collection.mutable.Map.empty[String, Pipelines]

    private val startedCounter = metrics.counter("startedCounter")
    private val failedCounter = metrics.counter("failedCounter")
    private val requestLatency = metrics.timer("requestLatency")

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
            Pipelines(List())
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
            else services(key) = Pipelines(pipelines)
        }
    }
}
// vim: set ts=4 sw=4 et:
