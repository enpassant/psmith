package core

import enpassant._

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import spray.can.Http
import akka.io.IO

case class Config(host: String = "localhost", port: Int = 9100,
    serviceHost: String = "localhost", servicePort: Int = 9101,
    router: Option[String] = None, mode: Option[String] = None,
    name: String = "api")

object Main extends App {
    implicit val actorSystem = ActorSystem("psmith")

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

    // parser.parse returns Option[C]
    parser.parse(args, Config()) match {
        case Some(config) =>
            val tickActor = config.router map {
                _ => actorSystem.actorOf(Props(new TickActor(config)))
            }
            tickActor.map(_ ! Tick)

            val model = actorSystem.actorOf(Props(new Model(config.mode)))
            val proxy = actorSystem.actorOf(Props(new Proxy(config, model, tickActor)))
            val service = actorSystem.actorOf(Props(new Service(config, model)))

        case None =>
    }
}
// vim: set ts=4 sw=4 et:
