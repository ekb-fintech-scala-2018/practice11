package ru.tinkoff.fintech.crwlr
import ru.tinkoff.fintech.crwlr.crawler.{CrawlReult, Crawler, Status}
import ru.tinkoff.fintech.crwlr.httpclient._

trait Program {
    def run[F[_]: OnFinish, M[_]: OnNext](
      crwlr: Crawler[M, F, String, String],
      url: Url): Unit
  }

object Program0 extends Program {
  def run[F[_]: OnFinish, M[_]: OnNext](
                                         crwlr: Crawler[M,F,String,String],
                                         url: Url
                                       ): Unit = {
    val (mon, res) = crwlr.crawl(url)

    implicitly[OnNext[M]].next(mon, {s: Status[String] =>
      println("status: ")
      println(s)
      ()
    })

    implicitly[OnFinish[F]].call(
      res,
      { x: Either[Throwable, CrawlReult[String]] =>
        x match {
          case Left(th) => th.printStackTrace()
          case Right(s: CrawlReult[String]) =>
            println("result: ")
            s.values.foreach {
              case (host, count) =>
                println(s"$host \t $count")
            }
        }
      }
    )
  }
}

object ServerProgram {
  def apply(
             updateMon: Status[String] => Unit,
             onComplete: CrawlReult[String] => Unit
  ): Program = new Program {
    override def run[F[_]: OnFinish, M[_]: OnNext](
      crwlr: Crawler[M, F, String, String],
      url: Url): Unit = {
      val (mon, res) = crwlr.crawl(url)

      implicitly[OnNext[M]].next(mon, updateMon)

      implicitly[OnFinish[F]].call(
        res,
        { x: Either[Throwable, CrawlReult[String]] =>
          x match {
            case Left(th) => th.printStackTrace()
            case Right(s: CrawlReult[String]) =>
              onComplete(s)
          }
        }
      )
    }
  }
}