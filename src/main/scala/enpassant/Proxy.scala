package enpassant

import core.{Config, Instrumented, MicroService, Restart, TickActor}

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, PoisonPill, Props, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpHeader, StatusCodes, Uri}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.io.IO
import akka.io.Tcp
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import java.net.InetSocketAddress
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Success, Failure, Random}
import spray.caching._

class Proxy(val config: Config, val routerDefined: Boolean)
    extends Actor with ActorLogging
{
    import context.dispatcher
    val managedHeaders = List("Host", "Server", "Date", "Content-Type",
        "Content-Length", "Transfer-Encoding", "Timeout-Access")
    val readMethods = List(GET, HEAD, OPTIONS)

    val cache: Cache[Future[RouteResult]] = LruCache(timeToLive = 60 seconds, timeToIdle = 10 seconds)

    implicit val system = context.system
    implicit val materializer = ActorMaterializer()

    val model = context.actorSelection("../" + Model.name)
    val tickActor: Option[ActorSelection] =
        if (routerDefined) Some(context.actorSelection("../" + TickActor.name))
        else None

    private def stripHeaders(headers: Seq[HttpHeader]):
        Seq[HttpHeader] = headers.filterNot(h => managedHeaders.contains(h.name))

      val proxy = Route { context =>
        val request = context.request
        tickActor map { _ ! Restart }
        val runningMode = request.cookies.find(_.name == "runningMode").map(_.value)
        if (request.uri.path.length < 4) {
            context.complete((StatusCodes.BadGateway,
                s"No service for path ${request.uri.path}"))
        } else {
            val microServicePath = request.uri.path.tail.tail
            val microServices =
                Model.findServices(microServicePath.tail.head.toString, runningMode)
            if (microServices.list.isEmpty) {
                model ! Started(None)
                model ! Failed(None)
                context.complete((StatusCodes.BadGateway,
                    s"No service for path ${request.uri.path}"))
            } else {
                val (microService, pipeline) = microServices.list(Random.nextInt(microServices.list.size))
                model ! Started(Some(microService))
                val start = System.currentTimeMillis
                def serviceFn = {
                    val updatedUri = request.uri
                        .withHost(microService.host)
                        .withPort(microService.port)
                        .withPath(microServicePath)
                    val updatedRequest = request.copy(uri = updatedUri,
                        headers = stripHeaders(request.headers))

                    val handler = Source.single(updatedRequest)
                      //.map(r => r.withHeaders(RawHeader("x-authenticated", "someone")))
                      .via(pipeline.flow)
                      .runWith(Sink.head)
                      .flatMap(context.complete(_))
                    handler
                }
                val futureResponse: Future[RouteResult] = if (readMethods contains request.method) {
                    val cacheKey = microServicePath + "_" + runningMode.toString
                    (cache(cacheKey) {
                        log.info("Run serviceFn")
                        serviceFn
                    }) flatMap identity
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
                futureResponse.onFailure {
                    case exn =>
                        model ! DeleteService(microService.uuid)
                        model ! Failed(Some(microService))
                        log.warning(s"Service for path ${request.uri.path} failed with ${exn}")
                }
                futureResponse
            }
        }
      }

      val binding = Http().bindAndHandle(handler = proxy, interface = "localhost", port = 9000)

    def receive = {
        case msg =>
            log.info(s"ConnectionHandler: $msg")

    }
}

object Proxy {
    def props(config: Config, routerDefined: Boolean) =
        Props(new Proxy(config, routerDefined))
    def name = "proxy"
}
// vim: set ts=4 sw=4 et:
