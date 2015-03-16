import akka.actor.{ ActorLogging, ActorRef }
import akka.pattern.ask
import spray.routing.{ HttpServiceActor, Route, ValidationRejection }

class Service(val mode: String, model: ActorRef) extends HttpServiceActor
    with ServiceDirectives with ActorLogging  with ServiceFormats {
    import context.dispatcher

    def receive = runRoute {
        debug {
            path("") {
                serviceLinks { headComplete }
            } ~
            pathPrefix("services") {
                (pathEnd compose get) {
                    respondWithJson { ctx =>
                        (model ? GetServices) map {
                            case response: List[MicroService] => ctx.complete(response)
                            case _ => ctx.reject()
                        }
                    }
                } ~
                path(Segment) { serviceId =>
                    get {
                        respondWithJson { ctx =>
                            (model ? GetService(serviceId)) map {
                                case response: MicroService => ctx.complete(response)
                                case _ => ctx.reject()
                            }
                        }
                    } ~
                    put {
                        entity(as[MicroService]) { entity => ctx =>
                            val microService = entity.copy(uuid = serviceId)
                            (model ? PutService(serviceId, microService)) map {
                                case response: MicroService => ctx.complete(response)
                                case _ => ctx.reject()
                            }
                        }
                    } ~
                    delete { ctx =>
                        (model ? DeleteService(serviceId)) map {
                            case response: String => ctx.complete(response)
                            case _ => ctx.reject()
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
