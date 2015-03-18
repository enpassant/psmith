import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.io.IO
import spray.can.Http

case class Config(host: String = "localhost", port: Int = 9100,
    serviceHost: String = "localhost", servicePort: Int = 9101, mode: String = "dev")

object Main extends App {
    implicit val actorSystem = ActorSystem("psmith")

    val parser = new scopt.OptionParser[Config]("psmith") {
        head("psmith", "1.0")
        opt[String]('h', "host") action { (x, c) =>
            c.copy(host = x) } text("host name or address. Default: localhost")
        opt[Int]('p', "port") action { (x, c) =>
            c.copy(port = x) } text("port number. Default: 9100")
        opt[String]('H', "service-host") action { (x, c) =>
            c.copy(serviceHost = x) } text("host name or address. Default: localhost")
        opt[Int]('P', "service-port") action { (x, c) =>
            c.copy(servicePort = x) } text("port number. Default: 9101")
        opt[String]('m', "mode") action { (x, c) =>
            c.copy(mode = x) } text("running mode. e.g.: dev, test, prod. Default: dev")
    }

    parser.parse(args, Config()) match {
        case Some(config) =>
            val model = actorSystem.actorOf(Props(new Model(config.mode)))
            val proxy = actorSystem.actorOf(Props(new ProxyService(config, model)))
            val service = actorSystem.actorOf(Props(new Service(config, model)))

        case None =>
    }
}
// vim: set ts=4 sw=4 et:
