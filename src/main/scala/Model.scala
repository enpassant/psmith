import akka.actor.{ ActorLogging, Actor }
import spray.routing.{ HttpServiceActor, Route, ValidationRejection }

case class GetServices()
case class GetService(serviceId: String)
case class PutService(serviceId: String, service: MicroService)
case class DeleteService(serviceId: String)

class Model(val mode: String) extends Actor with ActorLogging {
    import context.dispatcher

    def receive = process(List())

    private def process(services: List[MicroService]): Receive = {
        case GetServices =>
            sender ! services
        case GetService(serviceId) =>
            sender ! services.find(_.uuid == serviceId)
        case PutService(serviceId, microService) =>
            val index = services.indexWhere(_.uuid == serviceId)
            if (index >= 0) {
                context.become(process(
                    services.updated(index, microService)))
            } else {
                context.become(process(
                    microService :: services))
            }
            sender ! microService
        case DeleteService(serviceId) =>
            context.become(
                process(services.filter(_.uuid != serviceId)))
            sender ! ""
    }
}
// vim: set ts=4 sw=4 et:
