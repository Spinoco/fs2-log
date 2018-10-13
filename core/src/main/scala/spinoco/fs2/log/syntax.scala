package spinoco.fs2.log

import cats.implicits._
import cats.{Applicative, Show}
import fs2._
import sourcecode.{File, Line}

object syntax {

  class StreamLogOps[F[_] : Applicative : Log, A : Show](val stream: Stream[F, A]) {

    def apply(level: Log.Level.Value, message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext): Stream[F, A] = {
      lazy val m1 = message
      lazy val d1 = detail
      stream.evalMap { a =>
        Log[F].log(level, m1, Detail.as("value", a) append d1) as a
      }
    }

    @inline def error(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext) =
      apply(Log.Level.Error, message, detail)

    @inline def warn(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext) =
      apply(Log.Level.Warn, message, detail)

    @inline def info(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext) =
      apply(Log.Level.Info, message, detail)

    @inline def config(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext) =
      apply(Log.Level.Config, message, detail)

    @inline def debug(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext) =
      apply(Log.Level.Debug, message, detail)

    @inline def trace(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext) =
      apply(Log.Level.Trace, message, detail)


  }

  implicit class StreamLog[F[_] : Applicative:  Log, A : Show](val stream: Stream[F, A]) {

    def log: StreamLogOps[F, A] = new StreamLogOps[F, A](stream)

  }

}
