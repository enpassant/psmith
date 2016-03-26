package core

import enpassant.Supervisor

import akka.actor.{ActorSystem, Props}

case class Config(host: String = "0.0.0.0", port: Int = 9100,
  serviceHost: String = "0.0.0.0", servicePort: Int = 9101,
  router: Option[String] = None, mode: Option[String] = None,
  name: String = "services")

object Main extends App {
  implicit val actorSystem = ActorSystem("psmith")

  val parser = new scopt.OptionParser[Config]("psmith") {
    head("psmith", "1.0")
    opt[String]('h', "host") action { (x, c) =>
    c.copy(host = x) } text("host. Default: 0.0.0.0")
    opt[Int]('p', "port") action { (x, c) =>
    c.copy(port = x) } text("port number. Default: 9100")
    opt[String]('H', "service-host") action { (x, c) =>
    c.copy(serviceHost = x) } text("host name or address. Default: 0.0.0.0")
    opt[Int]('P', "service-port") action { (x, c) =>
    c.copy(servicePort = x) } text("port number. Default: 9101")
    opt[String]('r', "router") action { (x, c) =>
    c.copy(router = Some(x)) } text("router's host and port. e.g.: localhost:9101")
    opt[String]('m', "mode") action { (x, c) =>
    c.copy(mode = Some(x)) } text("running mode. e.g.: dev, test")
    opt[String]('n', "name") action { (x, c) =>
    c.copy(name = x) } text("name. Default: services")
  }

  // parser.parse returns Option[C]
  parser.parse(args, Config()) match {
    case Some(config) =>
      val superVisor = actorSystem.actorOf(Supervisor.props(config), Supervisor.name)

    case None =>
  }
}
