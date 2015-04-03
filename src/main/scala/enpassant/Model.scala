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

case class Started()
case class Failed()
case class Latency(time: Long)
case class GetMetrics()

class Model(val mode: Option[String]) extends Actor with ActorLogging {
    import context.dispatcher

    def receive = {
        case GetServices =>
            sender ! Model.services.values.flatMap(_.map(_._1))

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
                    Model.services(key) = (microService, pipeline) :: Model.services(key)
                } else {
                    Model.services(key) = List((microService, pipeline))
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

        case Started =>
            Model.startedCounter.inc()

        case Failed =>
            Model.failedCounter.inc()

        case Latency(time) =>
            Model.requestLatency.update(time, TimeUnit.MILLISECONDS)

        case GetMetrics =>
            sender ! Metrics(Model.requestLatency,
                Model.startedCounter, Model.failedCounter)
    }
}

object Model extends Instrumented {
    type Pipelines = List[(MicroService, Future[SendReceive])]
    type Collection = scala.collection.mutable.Map[String, Pipelines]

    private var services: Model.Collection = scala.collection.mutable.Map.empty[String, Model.Pipelines]

    private val startedCounter = metrics.counter("startedCounter")
    private val failedCounter = metrics.counter("failedCounter")
    private val requestLatency = metrics.timer("requestLatency")

    def getAllServices = {
        services.values.flatMap(_.map(_._1))
    }

    def findServiceById(serviceId: String) = {
        getAllServices.find(_.uuid == serviceId)
    }

    def findServices(path: String, runningMode: Option[String]): Model.Pipelines = {
        val key = name(path, runningMode)
        val keyNone = name(path, None)
        if (services contains key) {
            services(key)
        } else if (runningMode != None && services.contains(keyNone)) {
            services(keyNone)
        } else {
            List()
        }
    }

    def name(path: String, runningMode: Option[String]) = runningMode match {
        case Some(mode) => s"${path}-${mode}"
        case None => s"${path}"
    }

    private def deleteServices(service: MicroService) = {
        val key = name(service.path, service.runningMode)
        if (services contains key) {
            val pipelines = services(key) filterNot {
                case (s, p) => (service.uuid == s.uuid) ||
                    (service.host == s.host && service.port == s.port)
            }
            if (pipelines.isEmpty) services.remove(key)
            else services(key) = pipelines
        }
    }
}
// vim: set ts=4 sw=4 et:
