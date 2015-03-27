package enpassant

import core.Config

import akka.event.LoggingAdapter
import spray.routing.Route

trait Dev {
    val config: Config
    def log: LoggingAdapter

    def debug(route: Route): Route = {
        if (config.mode == "dev") {
            ctx =>
                val start = System.currentTimeMillis
                log.info(ctx.toString)
                route(ctx)
                val runningTime = System.currentTimeMillis - start
                log.info(s"Running time is ${runningTime} ms")
        } else route
    }
}
// vim: set ts=4 sw=4 et:
