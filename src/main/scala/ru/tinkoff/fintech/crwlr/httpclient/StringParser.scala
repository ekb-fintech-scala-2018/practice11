package ru.tinkoff.fintech.crwlr.httpclient
import java.net.URI

import scala.util.Try

class StringParser extends Parser[String] {
  val regexp = """<a[^>]+href="(.+?)"[^>]*>""".r

  override def links(base: Url, page: String): List[Url] = {
    regexp.findAllMatchIn(page).map{m => m.group(1)}.map { href =>
      Try(new URI(href)).toOption.map { uri =>
        Url(
          host = Option(uri.getHost).getOrElse(base.host),
          path = Option(uri.getRawPath)
            .map { p =>
              if (p.startsWith("/")) p
              else base.path.getOrElse("/") + p
            }
            .orElse(base.path),
          proto = Option(uri.getScheme).getOrElse(base.proto),
          port = (if (uri.getPort == -1) None else Some(uri.getPort))
            .orElse(base.port),
          query = Option(uri.getRawQuery)
        )
      }
    }.toList.flatten
  }
}

object StringParser extends StringParser