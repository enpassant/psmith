import akka.actor.{ ActorLogging, ActorRef }
import spray.routing.{ HttpServiceActor, Route }

class Proxy(val mode: String) extends HttpServiceActor with ActorLogging with Dev {
    import context.dispatcher

    def receive = runRoute {
        debug {
            complete("")
        }
    }
}
// vim: set ts=4 sw=4 et:
