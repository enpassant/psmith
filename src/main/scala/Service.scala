import akka.actor.ActorRef
import spray.routing.{ HttpServiceActor, Route, ValidationRejection }
import org.joda.time.DateTime
import java.util.UUID

class Service(val mode: String) extends HttpServiceActor with ServiceDirectives {
    import context.dispatcher

    def receive = runRoute {
        log {
            path("") {
                serviceLinks { headComplete }
            }
        }
    }

    def log(route: Route): Route = {
        if (mode == "dev") {
            ctx =>
                val start = System.currentTimeMillis
                println(ctx)
                route(ctx)
                val runningTime = System.currentTimeMillis - start
                println(s"Running time is ${runningTime} ms")
        } else route
    }
}
// vim: set ts=4 sw=4 et:
