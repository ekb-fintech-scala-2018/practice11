package ru.tinkoff.fintech.crwlr


package object httpclient {
  sealed case class Url(
                         host: String,
                         path: Option[String] = None,
                         proto: String = "http",
                         port: Option[Int] = None,
                         query: Option[String] = None
                       ) {
    def show: String =
      proto + "://" + host + port.map(":"+_).getOrElse("") + path.getOrElse("") + query.getOrElse("")
  }


  trait HttpClient[F[_],Body] {
    def get(url: Url): F[Body]
  }

  trait Parser[Body] {
    def links(url: Url, page: Body): List[Url]
  }
}