package core

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpMethod, MediaType, Uri}
import akka.http.scaladsl.model.headers.{Link, LinkParams, LinkValue}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.reflect.ClassTag

trait CommonDirectives {
  implicit val timeout = Timeout(10.seconds)
  import scala.concurrent.ExecutionContext.Implicits.global

  def headComplete = (options | head) { complete("") }

  def mtLink(uri: String, rel: String, mt: MediaType, methods: HttpMethod*) = {
    LinkValue(Uri(uri),
      LinkParams.rel(rel),
      LinkParams.`type`(mt.withParams(
        Map("method" -> methods.map(_.name).mkString(" ")))))
  }

  def jsonLink(uri: String, rel: String, methods: HttpMethod*) = {
    LinkValue(Uri(uri),
      LinkParams.rel(rel),
      LinkParams.`type`(MediaType.customBinary("application", "json",
        MediaType.Compressible, Nil,
        Map("method" -> methods.map(_.name).mkString(" ")))))
  }

  def respondWithLinks(links: LinkValue*) = respondWithHeader(Link(links : _*))
}
