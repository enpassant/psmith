package enpassant

import core.{Config, Metrics, MetricsFormats, MetricsStat, MicroService, ServiceFormats}

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.io.IO
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.pattern.ask
//import spray.can.Http
//import spray.routing.{ HttpServiceActor, Route, ValidationRejection }

class Service(val config: Config, val model: ActorRef)
    extends Actor
    //extends HttpServiceActor
    //with ServiceDirectives
    with ActorLogging
    //with ServiceFormats with MetricsFormats
    with Dev
{
    import context.dispatcher
    implicit val system = context.system
    implicit val materializer = ActorMaterializer()

    val bindingFuture = Http().bindAndHandle(route, config.serviceHost, config.servicePort)

    println(s"Server online at http://localhost:8080/")

    def route = {
        debug {
            path("") {
                //serviceLinks { headComplete }
                complete {
                    "Links"
                }
            }
            //~ pathPrefix("metrics") {
                //(pathEnd compose get) {
                    //respondWithJson { ctx =>
                        //(model ? GetMetrics) map {
                            //case metrics: MetricsStat => ctx.complete(metrics)
                            //case _ => ctx.reject()
                        //}
                    //}
                //}
            //} ~
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
