package enpassant

import core.Config

import akka.event.LoggingAdapter
import akka.http.scaladsl.server.Route

trait Dev {
    val config: Config
    def log: LoggingAdapter

    def debug(route: Route): Route = {
        config.mode match {
            case Some("dev") =>
                ctx =>
                    val start = System.currentTimeMillis
                    log.info(ctx.request.toString)
                    val result = route(ctx)
                    val runningTime = System.currentTimeMillis - start
                    log.info(s"Running time is ${runningTime} ms")
                    result
            case _ => route
        }
    }
}
// vim: set ts=4 sw=4 et:
