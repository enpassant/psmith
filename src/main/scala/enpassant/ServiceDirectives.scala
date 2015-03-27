package enpassant

import core.CommonDirectives
import spray.http.HttpMethods._

trait ServiceDirectives extends CommonDirectives {
    def serviceLinks = respondWithLinks(jsonLink("/services", "services", GET),
        jsonLink("/services/{serviceId}", "service", GET, PUT, DELETE))
}
// vim: set ts=4 sw=4 et:
