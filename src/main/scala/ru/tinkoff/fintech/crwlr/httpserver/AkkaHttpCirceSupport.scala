package ru.tinkoff.fintech.crwlr.httpserver

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentType, ContentTypeRange, HttpEntity}
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import io.circe.{Decoder, DecodingFailure, Encoder, Json, Printer, jawn}

object AkkaHttpCirceSupport {
  def unmarshallerContentTypes: Seq[ContentTypeRange] =
    mediaTypes.map(ContentTypeRange.apply)

  def mediaTypes: Seq[MediaType.WithFixedCharset] =
    List(`application/json`)

  implicit final def jsonMarshaller(
    implicit printer: Printer = Printer.noSpaces
  ): ToEntityMarshaller[Json] =
    Marshaller.oneOf(mediaTypes: _*) { mediaType =>
      Marshaller.withFixedContentType(ContentType(mediaType)) { json =>
        HttpEntity(mediaType, printer.pretty(json))
      }
    }

  implicit final def marshaller[A: Encoder](
    implicit printer: Printer = Printer.noSpaces
  ): ToEntityMarshaller[A] =
    jsonMarshaller(printer).compose(Encoder[A].apply)

  implicit final val jsonUnmarshaller: FromEntityUnmarshaller[Json] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(unmarshallerContentTypes: _*)
      .map {
        case ByteString.empty => throw Unmarshaller.NoContentException
        case data =>
          jawn.parseByteBuffer(data.asByteBuffer).fold(throw _, identity)

      }

  implicit def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    def decode(json: Json) = Decoder[A].decodeJson(json).fold(throw _, identity)
    jsonUnmarshaller.map(decode)
  }
}
