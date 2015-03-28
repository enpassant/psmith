package enpassant

import core.{MicroService, TickActor}
import core.Config
import core.Restart

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{Success, Failure, Random}
import spray.caching._
import spray.can.Http
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
import spray.http._
import spray.client.pipelining._
import akka.io.IO

//class Proxy(val config: Config, connection: ActorRef)
class Proxy(val config: Config, val tickActor: Option[ActorRef]) extends Actor with ActorLogging {
    import context.dispatcher
    implicit val timeout = Timeout(3.seconds)
    implicit val system = context.system
    private val pipeline = sendReceive
    val managedHeaders = List("Host", "Server", "Date", "Content-Type",
        "Content-Length", "Transfer-Encoding")
    val readMethods = List(GET, HEAD, OPTIONS)

    val cache: Cache[HttpResponse] = LruCache(timeToLive = 60 seconds, timeToIdle = 10 seconds)

    IO(Http) ! Http.Bind(self, interface = config.host, port = config.port)

    private def stripHeaders(headers: List[HttpHeader]):
        List[HttpHeader] = headers.filterNot(h => managedHeaders.contains(h.name))

    private def findServices(services: MicroServices.Collection,
        path: String, runningMode: Option[String]): MicroServices.Pipelines =
    {
        val key = MicroServices.name(path, runningMode)
        if (services contains key) {
            val modeServices = services(key)
            val keyNone = MicroServices.name(path, None)
            if (runningMode != None && modeServices.isEmpty && services.contains(keyNone))
                services(keyNone)
            else modeServices
        } else {
            List()
        }
    }

    def receive = process(self, Map())

    private def process(model: ActorRef, services: MicroServices.Collection): Receive = {
        case Http.Connected(_, _) =>
            sender ! Http.Register(self)
        case PutService(serviceId, newService) =>
            log.info(newService.toString)
            context.become(process(sender, MicroServices(services, newService)))
        case request: HttpRequest =>
            val selfActor = self
            val sndr = sender
            tickActor map { _ ! Restart }
            val runningMode = request.cookies.find(_.name == "runningMode").map(_.content)
            val microServicePath = request.uri.path.tail.tail
            val microServices =
                findServices(services, microServicePath.tail.head.toString, runningMode)
            if (microServices.isEmpty) {
                sndr ! HttpResponse(
                    status = StatusCodes.BadGateway,
                    entity = HttpEntity(s"No service for path ${request.uri.path}"))
            } else {
                val (microService, pipeline) = microServices(Random.nextInt(microServices.size))
                def serviceFn = {
                    val updatedUri = request.uri
                        .withHost(microService.host)
                        .withPort(microService.port)
                        .withPath(microServicePath)
                    val updatedRequest = request.copy(uri = updatedUri,
                        headers = stripHeaders(request.headers))

                    pipeline.flatMap(_(updatedRequest))
                }
                val futureResponse = if (readMethods contains request.method) {
                    val cacheKey = microServicePath + "_" + runningMode.toString
                    cache(cacheKey) {
                        serviceFn
                    }
                } else {
                    @annotation.tailrec
                    def removeKey(prefix: String, path: Uri.Path): Unit = {
                        val cacheKey = prefix + path.head + "_" + runningMode.toString
                        cache.remove(cacheKey)
                        if (!path.tail.isEmpty) removeKey(prefix + path.head, path.tail)
                    }
                    removeKey("", microServicePath)
                    serviceFn
                }
                futureResponse.onComplete {
                    case Success(response) =>
                        sndr ! response.copy(headers = stripHeaders(response.headers))
                    case Failure(exn) =>
                        model ! DeleteService(microService.uuid)
                        context.become(process(model, MicroServices.remove(services, microService)))
                        log.warning(s"Service for path ${request.uri.path} failed with ${exn}")
                        selfActor.tell(request, sndr)
                }
            }
        case msg =>
            log.debug(msg.toString)

    }
}
// vim: set ts=4 sw=4 et:
