import akka.actor.{ ActorLogging, ActorRef }
import akka.io.IO
import akka.pattern.ask
import spray.can.Http
import spray.routing.{ HttpServiceActor, Route, ValidationRejection }

class Service(val config: Config, model: ActorRef) extends HttpServiceActor
    with ServiceDirectives with ActorLogging  with ServiceFormats with Dev {
    import context.dispatcher

    implicit val system = context.system

    IO(Http) ! Http.Bind(self, interface = config.serviceHost, port = config.servicePort)

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
                                case Some(response: MicroService) => ctx.complete(response)
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
}
// vim: set ts=4 sw=4 et:
