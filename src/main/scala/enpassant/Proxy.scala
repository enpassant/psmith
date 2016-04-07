package enpassant

import core.{Config, Instrumented, MicroService, Restart, TickActor}

import akka.actor.{Actor, ActorLogging, ActorSelection, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.pattern.{ask, pipe}
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

  val cache: Cache[RouteResult] = LruCache(timeToLive = 60 seconds,
    timeToIdle = 10 seconds)

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  val model = context.actorSelection("../" + Model.name)

  private def stripHeaders(headers: Seq[HttpHeader]):
    Seq[HttpHeader] = headers.filterNot(h => managedHeaders.contains(h.name))

  private def mapLinkHeaders(prefix: String)(headers: Seq[HttpHeader]):
    Seq[HttpHeader] = headers.map { header =>
      if (header.name != "Link") header
      else addPrefixTo(prefix)(header)
  }

  private def addPrefixTo(prefix: String)(header: HttpHeader): HttpHeader = {
    val values = header.value.split(",")
    val regexp = "<([^>]+)>(.*)".r
    val prefixedValues = values map { value =>
      regexp.replaceAllIn(value, "<" + prefix + "$1>$2")
    }
    val prefixed = prefixedValues mkString ","
    RawHeader("Link", prefixed)
  }

  val proxy = Route {
    restartTick {
      path("") {
        head {
          cacheResult { context =>
            val microServicePath = context.request.uri.path
            collectHeads(context, microServicePath)
          }
        }
      } ~ {
        cacheResult {
          context =>
            val request = context.request
            val runningMode = request.cookies.find(_.name == "runningMode").map(_.value)
            if (request.uri.path.tail.isEmpty || request.uri.path.tail.head != config.name) {
              val microServicePath = context.request.uri.path
              proxyToMicroService(context, microServicePath, "")
            } else if (request.uri.path.length <= config.name.length) {
              val msg = s"Wrong path '${request.uri.path}'"
              log.warning(msg)
              context.complete((StatusCodes.BadGateway, msg))
            } else {
              val microServicePath = context.request.uri.path.tail.tail
              proxyToMicroService(context, microServicePath, microServicePath.tail.head.toString)
            }
        }
      }
    }
  }

  def cacheResult(route: Route): Route = {
    def futureResponse(context: RequestContext): Future[RouteResult] = {
      val request = context.request
      val authTokenHeader = request.headers.find(h => h.is("x-auth-token")) map { _.value }
      if (readMethods contains request.method) {
        val cacheKeySuffix = "_" + authTokenHeader.toString + "_" +
          request.uri.rawQueryString.toString + "_" + request.method.toString
        val cacheKey = request.uri.path.toString + cacheKeySuffix
        cache(cacheKey)(route(context))
      } else {
        @annotation.tailrec
        def removeKey(prefix: String, path: Uri.Path): Unit = {
          val cacheKeyPrefix = prefix + path.head + "_"
          cache.keys.foreach { case key: String =>
            if (key.startsWith(cacheKeyPrefix)) cache.remove(key)
          }
          if (!path.tail.isEmpty) removeKey(prefix + path.head, path.tail)
        }
        removeKey("", request.uri.path)
        route(context)
      }
    }
    context => futureResponse(context)
  }

  def proxyToMicroService(context: RequestContext, microServicePath: Uri.Path,
    msName: String): Future[RouteResult] =
  {
    val request = context.request
    val runningMode = request.cookies.find(_.name == "runningMode").map(_.value)
    val microServices = Model.findServices(msName, runningMode)

    if (microServices.list.isEmpty) {
      model ! Started(None)
      model ! Failed(None)

      val msg = s"No service for path ${request.uri.path}"
      log.warning(msg)
      context.complete((StatusCodes.BadGateway, msg))
    } else {
      val (microService, pipeline) = microServices.list(
        Random.nextInt(microServices.list.size))
      model ! Started(Some(microService))

      val updatedUri = request.uri
        .withHost(microService.host)
        .withPort(microService.port)
        .withPath(microServicePath)
      val updatedRequest = request.copy(uri = updatedUri,
        headers = stripHeaders(request.headers))

      val updatedContext = context.withRequest(updatedRequest)
      sendRequestTo(microService, pipeline, updatedContext, msName)
    }
  }

  val collectHeads = (context: RequestContext, microServicePath: Uri.Path) =>
  {
    val responses = Model.getServices map { case (microService, pipeline) =>
      sendRequestTo(microService, pipeline, context, microService.path)
    }
    Future.sequence(responses) map { results =>
      val headers = results.flatMap {
        case RouteResult.Complete(response) => response.headers
        case _ => List()
      }.to[Seq]
      RouteResult.Complete(HttpResponse(headers = headers))
    }
  }

  def sendRequestTo(
    microService: MicroService,
    pipeline: Pipeline,
    context: RequestContext,
    msName: String): Future[RouteResult] =
  {
    val start = System.currentTimeMillis
    val request = context.request
    val runningMode = request.cookies.find(_.name == "runningMode").map(_.value)
    def serviceFn = {
      val handler = Source.single((request, start))
        .via(pipeline.flow.completionTimeout(2.second))
        .runWith(Sink.head)
        .flatMap {
          case (Success(response), _) =>
            if (msName == "") context.complete(response)
            else {
              val mappedResponse = response.mapHeaders(mapLinkHeaders("/" + config.name))
              context.complete(mappedResponse)
            }
          case (Failure(exception), _) =>
            model ! DeleteService(microService.uuid)
            model ! Failed(Some(microService))
            log.warning(s"Service for path '${request.uri.path}' failed with '$exception'")
            context.reject()
        }
      handler
    }

    serviceFn
  }

  def restartTick(route: Route): Route = {
    if (routerDefined) {
      val tickActor = context.actorSelection("../" + TickActor.name)
      requestContext =>
        tickActor ! Restart
        route(requestContext)
    } else route
  }

  val binding = Http().bindAndHandle(handler = proxy,
    interface = config.host, port = config.port)

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
