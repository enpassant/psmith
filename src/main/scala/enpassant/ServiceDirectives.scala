package enpassant

import core.CommonDirectives
import core.ServiceFormats
import akka.http.scaladsl.model.HttpMethods._

trait ServiceDirectives extends CommonDirectives with ServiceFormats {
  def serviceLinks = respondWithLinks(
    jsonLink("/services", "services", GET),
    mtLink("/services/{serviceId}", "service",
      `application/vnd.enpassant.service+json`, GET, PUT, DELETE))
}
