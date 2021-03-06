package play.sockjs.core
package transports

import scala.concurrent.Future

import play.api.libs.iteratee._
import play.api.mvc._
import play.api.http._
import play.api.libs.json._

/**
 * HTMLfile transport
 */
private[sockjs] object HtmlFile extends HeaderNames with Results {

  def transport = Transport.Streaming { (req, session) =>
    req.getQueryString("c").orElse(req.getQueryString("callback")).map { callback =>
      if (!callback.matches("[^a-zA-Z0-9-_.]")) session.bind { enumerator =>
        val tpl =
          """
            |<!doctype html>
            |<html><head>
            |  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
            |  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
            |</head><body><h2>Don't panic!</h2>
            |  <script>
            |    document.domain = document.domain;
            |    var c = parent.""".stripMargin+callback+""";
            |    c.start();
            |    function p(d) {c.message(d);};
            |    window.onload = function() {c.stop();};
            |  </script>
          """.stripMargin
        val prelude = tpl + Array.fill(1024 - tpl.length + 14)(' ').mkString + "\r\n\r\n"
        Transport.Res(Enumerator(prelude) >>> (enumerator &> Enumeratee.map { (frame: Frame) =>
          s"<script>\np(${JsString(frame.text)});\n</script>\r\n"
        }(play.api.libs.iteratee.Execution.trampoline)))
      } else Future.successful(InternalServerError("invalid \"callback\" parameter"))
    }.getOrElse(Future.successful(InternalServerError("\"callback\" parameter required")))
  }

  implicit def writeableOf_HtmlFileTransport: Writeable[String] = Writeable[String] (
    txt => Codec.utf_8.encode(txt),
    Some("text/html; charset=UTF-8"))

}
