import akka.actor.{ ActorLogging, Actor, ActorRef }
import spray.routing.{ HttpServiceActor, Route, ValidationRejection }

case class GetServices()
case class GetService(serviceId: String)
case class PutService(serviceId: String, service: MicroService)
case class DeleteService(serviceId: String)
case class SetServices(services: List[MicroService])

class Model(val mode: String, proxy: ActorRef) extends Actor with ActorLogging {
    import context.dispatcher

    def receive = process(List())

    private def process(services: List[MicroService]): Receive = {
        case GetServices =>
            sender ! services
        case GetService(serviceId) =>
            sender ! services.find(_.uuid == serviceId)
        case PutService(serviceId, microService) =>
            sender ! microService
            val index = services.indexWhere(_.uuid == serviceId)
            if (index < 0) {
                val newServices = microService :: services
                proxy ! PutService(serviceId, microService)
                context.become(process(newServices))
            }
        case DeleteService(serviceId) =>
            sender ! ""
            val newServices = services.filter(_.uuid != serviceId)
            proxy ! SetServices(newServices)
            context.become(process(newServices))
    }
}
// vim: set ts=4 sw=4 et:
