package enpassant

//import core.{Config, Instrumented, MicroService, Restart, TickActor}

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import akka.io.IO
import akka.io.Tcp
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import java.net.InetSocketAddress
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Success, Failure, Random}
//import spray.caching._
//import spray.can.Http
//import spray.http.HttpHeaders._
//import spray.http.HttpMethods._
//import spray.http._
//import spray.client.pipelining._

//class Proxy(val config: Config, val model: ActorRef, val tickActor: Option[ActorRef])
    //extends Actor with ActorLogging
//{
    //import context.dispatcher
    //implicit val timeout = Timeout(3.seconds)
    //implicit val system = context.system
    //private val pipeline = sendReceive
    //val managedHeaders = List("Host", "Server", "Date", "Content-Type",
        //"Content-Length", "Transfer-Encoding")
    //val readMethods = List(GET, HEAD, OPTIONS)
    ////val readMethods = List()

    //val cache: Cache[HttpResponse] = LruCache(timeToLive = 60 seconds, timeToIdle = 10 seconds)

    //IO(Http) ! Http.Bind(self, interface = config.host, port = config.port)

    //private def stripHeaders(headers: List[HttpHeader]):
        //List[HttpHeader] = headers.filterNot(h => managedHeaders.contains(h.name))

    //def receive = {
        //case Tcp.Connected(remote, _) =>
            //log.debug(s"Connected($remote)")
            //sender ! Tcp.Register(self)

        //case request: HttpRequest =>
            //val selfActor = self
            //val sndr = sender
            //tickActor map { _ ! Restart }
            //val runningMode = request.cookies.find(_.name == "runningMode").map(_.content)
            //val microServicePath = request.uri.path.tail.tail
            //val microServices =
                //Model.findServices(microServicePath.tail.head.toString, runningMode)
            //if (microServices.list.isEmpty) {
                //model ! Started(None)
                //model ! Failed(None)
                //sndr ! HttpResponse(
                    //status = StatusCodes.BadGateway,
                    //entity = HttpEntity(s"No service for path ${request.uri.path}"))
            //} else {
                //val (microService, pipeline) = microServices.list(Random.nextInt(microServices.list.size))
                //model ! Started(Some(microService))
                //val start = System.currentTimeMillis
                //def serviceFn = {
                    //val updatedUri = request.uri
                        //.withHost(microService.host)
                        //.withPort(microService.port)
                        //.withPath(microServicePath)
                    //val updatedRequest = request.copy(uri = updatedUri,
                        //headers = stripHeaders(request.headers))

                    //pipeline.value.flatMap(_(updatedRequest))
                //}
                //val futureResponse = if (readMethods contains request.method) {
                    //val cacheKey = microServicePath + "_" + runningMode.toString
                    //cache(cacheKey) {
                        //serviceFn
                    //}
                //} else {
                    //@annotation.tailrec
                    //def removeKey(prefix: String, path: Uri.Path): Unit = {
                        //val cacheKey = prefix + path.head + "_" + runningMode.toString
                        //cache.remove(cacheKey)
                        //if (!path.tail.isEmpty) removeKey(prefix + path.head, path.tail)
                    //}
                    //removeKey("", microServicePath)
                    //serviceFn
                //}
                //futureResponse.onComplete {
                    //case Success(response) =>
////                        val end = System.currentTimeMillis
////                        requestLatency.update(end - start, TimeUnit.MILLISECONDS)
////                        sndr ! response.copy(headers = stripHeaders(response.headers))
                        //selfActor ! ((start, sndr, response, microService))
                    //case Failure(exn) =>
                        //model ! DeleteService(microService.uuid)
                        //model ! Failed(Some(microService))
                        //log.warning(s"Service for path ${request.uri.path} failed with ${exn}")
                        //selfActor.tell(request, sndr)
                //}
            //}

        //case (start: Long, sndr: ActorRef, response: HttpResponse, microService: MicroService) =>
            //val end = System.currentTimeMillis
            //model ! Latency(end - start, Some(microService))
            //sndr ! response.copy(headers = stripHeaders(response.headers))

        //case c: Tcp.ConnectionClosed =>
            //log.debug("Connection for remote address closed ({})", c)

        //case msg =>
            //log.info("ConnectionHandler: {}", msg)

    //}
//}
// vim: set ts=4 sw=4 et:
