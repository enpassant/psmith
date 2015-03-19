import akka.actor.ActorContext
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.can.Http
import spray.http._
import spray.client.pipelining._
import scala.concurrent.Future

object MicroServices {
    type Pipelines = List[(MicroService, Future[SendReceive])]
    type Collection = Map[String, Pipelines]

    def apply(msColl: Collection, service: MicroService)
        (implicit context: ActorContext): Collection =
    {
        import context.dispatcher
        implicit val timeout = Timeout(3.seconds)
        implicit val system = context.system

        def pipeline: Future[SendReceive] =
            for (
                Http.HostConnectorInfo(connector, _) <-
                    IO(Http) ? Http.HostConnectorSetup(service.host, port = service.port)
        ) yield sendReceive(connector)
        val key = name(service.path, service.runningMode)
        if (msColl contains key) {
            msColl + (key -> ((service, pipeline) :: msColl(key)))
        } else {
            msColl + (key -> List((service, pipeline)))
        }
    }

    def name(path: String, runningMode: Option[String]) = s"${path}-${runningMode}"
}
// vim: set ts=4 sw=4 et:
