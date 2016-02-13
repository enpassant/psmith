package core

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpMethod, MediaType, Uri}
import akka.http.scaladsl.model.headers.{Link, LinkParams, LinkValue}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
//import spray.routing.HttpService
//import spray.http.{ HttpMethod, MediaType, MediaTypes, Uri }
//import spray.http.HttpHeaders._
//import spray.httpx.marshalling._
//import spray.httpx.unmarshalling._
import scala.reflect.ClassTag

trait CommonDirectives
    //extends HttpService
{
    //val model: ActorRef

    implicit val timeout = Timeout(10.seconds)
    import scala.concurrent.ExecutionContext.Implicits.global

    //def respondWithJson =
        //respondWithMediaType(MediaTypes.`application/json`)

    //def completeJson(block: ToResponseMarshallable) =
        //respondWithMediaType(MediaTypes.`application/json`) {
            //complete(block)
        //}

    def headComplete = (options | head) { complete("") }

    def jsonLink(uri: String, rel: String, methods: HttpMethod*) = {
        LinkValue(Uri(uri),
            LinkParams.rel(rel),
            LinkParams.`type`(MediaType.customBinary("application", "json",
                MediaType.Compressible, Nil, Map("method" -> methods.mkString(" ")))))
    }

    def respondWithLinks(links: LinkValue*) = respondWithHeader(Link(links : _*))
}
// vim: set ts=4 sw=4 et:
