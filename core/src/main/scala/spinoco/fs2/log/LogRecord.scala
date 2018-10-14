package spinoco.fs2.log

import java.time.Instant

import cats.Eval
import cats.effect.Sync
import spinoco.fs2.log.spi.LoggerProvider

/**
  * As an intermediate storage for asynchronously logged events
  *
  * @param level      Level of the event
  * @param time       Actual time when the event was generated
  * @param message    Message
  * @param detail     Details to be logged with event
  * @param line       Line where the event happened
  * @param file       File, where the event happened
  * @param ctx        Context of the event (usually logger name)
  * @param thrown     Thrown exception
  */
private[log] final case class LogRecord (
  level: Log.Level.Value
  , time: Instant
  , message: Eval[String]
  , detail: Eval[Detail]
  , line: sourcecode.Line
  , file: sourcecode.File
  , ctx: LogContext
  , thrown: Option[Throwable]
) { self =>

  def toProvider[F[_] : Sync : LoggerProvider]: F[Unit] =
    Sync[F].handleErrorWith(
      Sync[F].suspend {
        LoggerProvider[F].log(level, ctx, time, message.value, detail.value.dump, line.value, file.value, thrown)
      }
    )(err => Sync[F].delay {
      new Throwable(s"Unexpected error while logging record: $self", err)
     .printStackTrace(System.err)
    })


}


