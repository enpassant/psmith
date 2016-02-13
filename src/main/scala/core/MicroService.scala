package core

import java.util.UUID
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling._

case class MicroService(
    uuid: String = UUID.randomUUID.toString,
    path: String,
    host: String,
    port: Int = 9000,
    runningMode: Option[String] = None)

trait ServiceFormats extends BaseFormats {
    lazy val `application/vnd.enpassant.service+json`: MediaType.WithFixedCharset =
        MediaType.customWithFixedCharset(
            "application",
            "vnd.enpassant.service+json",
            HttpCharsets.`UTF-8`
        )

    implicit val ServiceUnmarshaller = Unmarshaller.firstOf(
        unmarshaller[MicroService](`application/vnd.enpassant.service+json`),
        unmarshaller[MicroService](MediaTypes.`application/json`))

    implicit val ServiceMarshaller = Marshaller.oneOf(
        marshaller[MicroService](`application/vnd.enpassant.service+json`),
        marshaller[MicroService](MediaTypes.`application/json`))

    implicit val SeqServiceMarshaller = marshaller[Seq[MicroService]](
        MediaTypes.`application/json`)
}
// vim: set ts=4 sw=4 et:
