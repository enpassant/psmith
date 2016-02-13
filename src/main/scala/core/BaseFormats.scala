package core

import org.json4s.jackson.Serialization.{ read, writePretty }
//import spray.httpx.Json4sSupport
import de.heikoseeberger.akkahttpjson4s
import org.json4s.{ DefaultFormats, Formats, jackson }
import org.joda.time.DateTime
//import akka.http.scaladsl.marshalling._
//import akka.http.scaladsl.unmarshalling._
//import akka.http.scaladsl.model.{ ContentType, ContentTypeRange, HttpEntity, MediaType, MediaTypes }

trait BaseFormats {
    private implicit val serialization = jackson.Serialization
    private implicit val formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

    //def unmarshal[T](mediaType: ContentTypeRange*)(implicit m: Manifest[T]): Unmarshaller[T] =
        //Unmarshaller[T](mediaType:_*) {
            //case HttpEntity.NonEmpty(contentType, data) =>
                //read[T](data.asString)
        //}

    //def marshal[T <: AnyRef](contentType: ContentType*): Marshaller[T] =
        //Marshaller.of[T](contentType:_*) { (value, contentType, ctx) =>
            //ctx.marshalTo(HttpEntity(contentType, writePretty[T](value)))
        //}
}
// vim: set ts=4 sw=4 et:
