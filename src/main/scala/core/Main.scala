package core

//import enpassant._

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Sink, Source}

case class Config(host: String = "localhost", port: Int = 9100,
    serviceHost: String = "localhost", servicePort: Int = 9101,
    router: Option[String] = None, mode: Option[String] = None,
    name: String = "api")

object Main extends App {
    implicit val actorSystem = ActorSystem("psmith")
    implicit val materializer = ActorMaterializer()
    implicit val ec = actorSystem.dispatcher

    val parser = new scopt.OptionParser[Config]("psmith") {
        head("psmith", "1.0")
        opt[String]('h', "host") action { (x, c) =>
            c.copy(host = x) } text("host. Default: localhost")
        opt[Int]('p', "port") action { (x, c) =>
            c.copy(port = x) } text("port number. Default: 9100")
        opt[String]('H', "service-host") action { (x, c) =>
            c.copy(serviceHost = x) } text("host name or address. Default: localhost")
        opt[Int]('P', "service-port") action { (x, c) =>
            c.copy(servicePort = x) } text("port number. Default: 9101")
        opt[String]('r', "router") action { (x, c) =>
            c.copy(router = Some(x)) } text("router's host and port. e.g.: localhost:9101")
        opt[String]('m', "mode") action { (x, c) =>
            c.copy(mode = Some(x)) } text("running mode. e.g.: dev, test")
        opt[String]('n', "name") action { (x, c) =>
            c.copy(name = x) } text("name. Default: api")
     }

    val managedHeaders = List("Host", "Server", "Date", "Content-Type",
        "Content-Length", "Transfer-Encoding", "Timeout-Access")

    private def stripHeaders(headers: scala.collection.immutable.Seq[HttpHeader]):
        scala.collection.immutable.Seq[HttpHeader] = headers.filterNot(h => managedHeaders.contains(h.name))

    // parser.parse returns Option[C]
    parser.parse(args, Config()) match {
        case Some(config) =>
            //val tickActor = config.router map {
                //_ => actorSystem.actorOf(Props(new TickActor(config)))
            //}
            //tickActor.map(_ ! Tick)

            //val model = actorSystem.actorOf(Props(new Model(config.mode)))
            //val proxy = actorSystem.actorOf(Props(new Proxy(config, model, tickActor)))
            //val service = actorSystem.actorOf(Props(new Service(config, model)))

            val proxy = Route { context =>
                val request = context.request
                val updatedUri = request.uri
                    .withHost(config.host)
                    .withPort(config.port)
                    .withPath(Path./)
                val updatedRequest = request.copy(uri = updatedUri,
                    headers = stripHeaders(request.headers))
                    //println("Opening connection to " + request.uri.authority.host.address)
                    println("Opening connection to " + config.host + ":" + config.port)
                val flow = Http(actorSystem).outgoingConnection(config.host, config.port)
                val handler = Source.single(updatedRequest)
                  //.map(r => r.withHeaders(RawHeader("x-authenticated", "someone")))
                  .via(flow)
                  .runWith(Sink.head)
                  .flatMap(context.complete(_))
                handler
            }

            val binding = Http(actorSystem).bindAndHandle(handler = proxy, interface = "localhost", port = 9000)

        case None =>
    }
}
// vim: set ts=4 sw=4 et:
