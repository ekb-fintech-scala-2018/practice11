package ru.tinkoff.fintech.crwlr
import io.circe.generic.JsonCodec

package object httpserver {
  @JsonCodec
  final case class JobStatus(isCompleted: Boolean, hosts: Map[String, Int])

  @JsonCodec
  final case class AddJob(proto: String, host: String)
}
