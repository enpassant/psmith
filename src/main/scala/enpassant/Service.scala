package enpassant

import core.{Config, Metrics, MetricsFormats, MetricsStat, MicroService, ServiceFormats}

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.io.IO
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.pattern.ask
import de.heikoseeberger.akkahttpjson4s._

class Service(val config: Config, val model: ActorRef)
    extends Actor
    with ServiceDirectives
    with ActorLogging
    with ServiceFormats
    with MetricsFormats
    with Dev
{
    import Json4sSupport._

    import context.dispatcher
    implicit val system = context.system
    implicit val materializer = ActorMaterializer()

    //implicit val timeout = Timeout(10.seconds)
    //import scala.concurrent.ExecutionContext.Implicits.global

    val bindingFuture = Http().bindAndHandle(route, config.serviceHost, config.servicePort)

    println(s"Server online at http://${config.serviceHost}:${config.servicePort}/")

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
            }
            //pathPrefix("services") {
                //(pathEnd compose get) {
                    //respondWithJson { ctx =>
                        //(model ? GetServices) map {
                            //case response: List[MicroService @unchecked] => ctx.complete(response)
                            //case _ => ctx.reject()
                        //}
                    //}
                //} ~
                //path(Segment) { serviceId =>
                    //get {
                        //respondWithJson { ctx =>
                            //(model ? GetService(serviceId)) map {
                                //case Some(response: MicroService) => ctx.complete(response)
                                //case _ => ctx.reject()
                            //}
                        //}
                    //} ~
                    //put {
                        //entity(as[MicroService]) { entity => ctx =>
                            //val microService = entity.copy(uuid = serviceId)
                            //(model ? PutService(serviceId, microService)) map {
                                //case response: MicroService => ctx.complete(response)
                                //case _ => ctx.reject()
                            //}
                        //}
                    //} ~
                    //delete { ctx =>
                        //(model ? DeleteService(serviceId)) map {
                            //case response: String => ctx.complete(response)
                            //case _ => ctx.reject()
                        //}
                    //}
                //}
            //}
        }
    }

    def receive = {
        case _ =>
    }
}
// vim: set ts=4 sw=4 et:
