package enpassant

import core.MicroService

import akka.actor.{ ActorLogging, Actor, ActorRef }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import spray.can.Http
import spray.http._
import spray.client.pipelining._
import spray.routing.{ HttpServiceActor, Route, ValidationRejection }
import spray.client.pipelining._

case class GetServices()
case class GetService(serviceId: String)
case class PutService(serviceId: String, service: MicroService)
case class DeleteService(serviceId: String)
case class SetServices(services: List[MicroService])

class Model(val mode: Option[String]) extends Actor with ActorLogging {
    import context.dispatcher

    def receive = {
        case GetServices =>
            sender ! Model.servicesById.values.toList

        case GetService(serviceId) =>
            sender ! Model.servicesById(serviceId)

        case PutService(serviceId, microService) =>
            Model.deleteServices(microService)

            val service = Model.servicesById.get(serviceId)
            if (service == None) {
                Model.servicesById(serviceId) = microService

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
            Model.servicesById.remove(serviceId) match {
                case Some(service) =>
                    Model.deleteServices(service)
                case None =>
            }
            sender ! ""
    }
}

object Model {
    type Pipelines = List[(MicroService, Future[SendReceive])]
    type Collection = scala.collection.mutable.Map[String, Pipelines]

    var servicesById = scala.collection.mutable.Map.empty[String, MicroService]
    var services: Model.Collection = scala.collection.mutable.Map.empty[String, Model.Pipelines]

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
