import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Success, Failure, Random}
import spray.can.Http
import spray.http.HttpHeaders._
import spray.http._
import spray.client.pipelining._
import akka.io.IO

//class ProxyService(val config: Config) extends Actor with ActorLogging with Dev {
//    implicit val system = context.system
//
//    IO(Http) ! Http.Bind(self, interface = config.host, port = config.port)
//
//    override def receive: Receive = {
//        case Http.Connected(_, _) =>
//            sender ! Http.Register(context.actorOf(Props(new Proxy(config, sender))))
//    }
//}

//class Proxy(val config: Config, connection: ActorRef)
class Proxy(val config: Config)
    extends Actor with ActorLogging
{
    import context.dispatcher
    implicit val timeout = Timeout(3.seconds)
    implicit val system = context.system
    private val pipeline = sendReceive
    val managedHeaders = List("Host", "Server", "Date", "Content-Type",
        "Content-Length", "Transfer-Encoding")

    IO(Http) ! Http.Bind(self, interface = config.host, port = config.port)

    private def stripHeaders(headers: List[HttpHeader]):
        List[HttpHeader] = headers.filterNot(h => managedHeaders.contains(h.name))

    private def findServices(services: List[MicroService],
        path: String, runningMode: Option[String]): List[MicroService] =
    {
        val modeServices = services.filter(s =>
            s.path == path && s.runningMode == runningMode)
        if (modeServices.isEmpty)
            services.filter(s => s.path == path)
        else modeServices
    }

    def receive = process(List())

    private def process(services: List[MicroService]): Receive = {
        case Http.Connected(_, _) =>
            sender ! Http.Register(self)
        case SetServices(newServices) =>
            log.info(newServices.toString)
            context.become(process(newServices))
        case request: HttpRequest =>
            val sndr = sender
            val microServices: List[MicroService] =
                findServices(services, request.uri.path.tail.head.toString, None)
            if (microServices.isEmpty) {
                sndr ! HttpResponse(
                    status = StatusCodes.BadGateway,
                    entity = HttpEntity(s"No service for path ${request.uri.path}"))
            } else {
                val microService = microServices(Random.nextInt(microServices.size))
                val microServicePath = request.uri.path
                val updatedUri = request.uri
                    .withHost(microService.host)
                    .withPort(microService.port)
                    .withPath(microServicePath)
                val updatedRequest =
                    request.copy(uri = updatedUri,
                    headers = Host(microService.host, microService.port) ::
                        stripHeaders(request.headers))

                val futureResponse = pipeline(updatedRequest)
                futureResponse.onComplete {
                    case Success(response) =>
                        sndr ! response.copy(headers = stripHeaders(response.headers))
                    case Failure(exn) =>
//                            model ! DeleteService(microService.uuid)
                        sndr ! HttpResponse(
                            status = StatusCodes.BadGateway,
                            entity = HttpEntity(s"Service for path ${request.uri.path} failed with ${exn}"))
                }
//                Await.result(futureResponse, 10 seconds)
            }
        case msg =>
            log.debug(msg.toString)

    }
}
// vim: set ts=4 sw=4 et:
