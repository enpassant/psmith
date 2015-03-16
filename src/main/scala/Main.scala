import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import spray.can.Http
import akka.io.IO

object Main extends App {
    implicit val actorSystem = ActorSystem("psmith")

    case class Config(host: String = "localhost", port: Int = 9000,
        serviceHost: String = "localhost", servicePort: Int = 9001, mode: String = "dev")

    val parser = new scopt.OptionParser[Config]("psmith") {
        head("psmith", "1.0")
        opt[String]('h', "host") action { (x, c) =>
            c.copy(host = x) } text("host name or address. Default: localhost")
        opt[Int]('p', "port") action { (x, c) =>
            c.copy(port = x) } text("port number. Default: 9100")
        opt[String]('H', "service-host") action { (x, c) =>
            c.copy(host = x) } text("host name or address. Default: localhost")
        opt[Int]('P', "service-port") action { (x, c) =>
            c.copy(port = x) } text("port number. Default: 9101")
        opt[String]('m', "mode") action { (x, c) =>
            c.copy(mode = x) } text("running mode. e.g.: dev, test, prod. Default: dev")
    }

    // parser.parse returns Option[C]
    parser.parse(args, Config()) match {
        case Some(config) =>
            class BaseActor extends Actor {
                def receive: Receive = {
                    case "start" =>
                        val proxy = actorSystem.actorOf(Props(new Proxy(config.mode)))
                        IO(Http) ! Http.Bind(proxy, interface = config.host,
                            port = config.port)

                        val model = actorSystem.actorOf(Props(new Model(config.mode)))
                        val service = actorSystem.actorOf(Props(new Service(config.mode, model)))
                        IO(Http) ! Http.Bind(service, interface = config.serviceHost,
                            port = config.servicePort)
                }
            }

            val myApp: ActorRef = actorSystem.actorOf(Props(new BaseActor))
            myApp ! "start"

        case None =>
    }
}
// vim: set ts=4 sw=4 et:
