import akka.actor.{ ActorLogging, ActorRef }
import spray.routing.{ HttpServiceActor, Route, ValidationRejection }
import org.joda.time.DateTime
import java.util.UUID

class Service(val mode: String) extends HttpServiceActor with ServiceDirectives with ActorLogging {
    import context.dispatcher

    def receive = runRoute {
        debug {
            path("") {
                serviceLinks { headComplete }
            }
        }
    }

    def debug(route: Route): Route = {
        if (mode == "dev") {
            ctx =>
                val start = System.currentTimeMillis
                log.debug(ctx.toString)
                route(ctx)
                val runningTime = System.currentTimeMillis - start
                log.debug(s"Running time is ${runningTime} ms")
        } else route
    }
}
// vim: set ts=4 sw=4 et:
