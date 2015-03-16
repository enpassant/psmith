import akka.actor.{ ActorLogging, ActorRef }
import spray.routing.{ HttpServiceActor, Route, ValidationRejection }
import org.joda.time.DateTime
import java.util.UUID

class Service(val mode: String) extends HttpServiceActor
    with ServiceDirectives with ActorLogging  with ServiceFormats {
    import context.dispatcher

    def receive = process(List())

    private def process(services: List[MicroService]): Receive = runRoute {
        debug {
            path("") {
                serviceLinks { headComplete }
            } ~
            pathPrefix("services") {
                (pathEnd compose get) {
                    completeJson {
                        services
                    }
                } ~
                path(Segment) { serviceId =>
                    get {
                        completeJson {
                            services.find(_.uuid == serviceId)
                        }
                    } ~
                    put {
                        entity(as[MicroService]) { entity =>
                            val microService = entity.copy(uuid = serviceId)
                            val index = services.indexWhere(_.uuid == serviceId)
                            complete {
                                if (index >= 0) {
                                    context.become(process(
                                        services.updated(index, microService)))
                                } else {
                                    context.become(process(
                                        microService :: services))
                                }
                                microService
                            }
                        }
                    } ~
                    delete {
                        complete {
                            context.become(
                                process(services.filter(_.uuid != serviceId)))
                            ""
                        }
                    }
                }
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
