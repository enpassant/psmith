package enpassant

import core.{Config, Metrics, MetricsFormats, MetricsStat, MicroService, ServiceFormats}

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.pattern.ask

class Service(val config: Config)
    extends Actor
    with ServiceDirectives
    with ActorLogging
    with ServiceFormats
    with MetricsFormats
    with Dev
{
    import context.dispatcher
    implicit val system = context.system
    implicit val materializer = ActorMaterializer()

    val model = context.actorSelection("../" + Model.name)

    val bindingFuture = Http().bindAndHandle(route, config.serviceHost, config.servicePort)

    log.info(s"Server online at http://${config.serviceHost}:${config.servicePort}/")

    def route = {
        debug {
            path("") {
                serviceLinks { headComplete }
            } ~
            pathPrefix("metrics") {
                pathEnd {
                    get {
                        ctx =>
                            (model ? GetMetrics) flatMap {
                                case metrics: MetricsStat => ctx.complete(metrics)
                                case _ => ctx.reject()
                        }
                    }
                }
            } ~
            pathPrefix("services") {
                pathEnd {
                    get { ctx =>
                        (model ? GetServices) flatMap {
                            case response: List[MicroService @unchecked] => ctx.complete(response)
                            case _ => ctx.reject()
                        }
                    }
                } ~
                path(Segment) { serviceId =>
                    get {
                        { ctx =>
                            (model ? GetService(serviceId)) flatMap {
                                case Some(response: MicroService) => ctx.complete(response)
                                case _ => ctx.reject()
                            }
                        }
                    } ~
                    put {
                        entity(as[MicroService]) { entity => ctx =>
                            val microService = entity.copy(uuid = serviceId)
                            (model ? PutService(serviceId, microService)) flatMap {
                                case response: MicroService => ctx.complete(response)
                                case _ => ctx.reject()
                            }
                        }
                    } ~
                    delete { ctx =>
                        (model ? DeleteService(serviceId)) flatMap {
                            case response: String => ctx.complete(response)
                            case _ => ctx.reject()
                        }
                    }
                }
            }
        }
    }

    def receive = {
        case _ =>
    }
}

object Service {
    def props(config: Config) = Props(new Service(config))
    def name = "service"
}
// vim: set ts=4 sw=4 et:
