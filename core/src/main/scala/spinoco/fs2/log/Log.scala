package spinoco.fs2.log

import java.time.Instant

import cats.implicits._
import cats.{Applicative, Eval}
import cats.effect.concurrent.Deferred
import cats.effect.{Concurrent, Resource, Sync}
import fs2.concurrent.Queue
import sourcecode.{File, Line}
import spinoco.fs2.log.spi.LoggerProvider
import fs2._

trait Log[F[_]] {

  /**
    * Logs the message with supplied level
    * @param level      Level to log message with
    * @param message    Message to log
    * @param detail     Any details to attach with message
    */
  def log(level: Log.Level.Value, message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext): F[Unit]

  /** alias for log(Error, message, detail) **/
  @inline def error(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Error, message, detail)

  /** alias for log(Warn, message, detail) **/
  @inline def warn(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Warn, message, detail)

  /** alias for log(Info, message, detail) **/
  @inline def info(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Info, message, detail)

  /** alias for log(Config, message, detail) **/
  @inline def config(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Config, message, detail)

  /** alias for log(Debug, message, detail) **/
  @inline def debug(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Debug, message, detail)

  /** alias for log(Trace, message, detail) **/
  @inline def trace(message: => String, detail: => Detail = Detail.empty)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Trace, message, detail)

}


object Log {

  /** various Log levels to log **/
  object Level extends Enumeration {

    val Error, Warn, Info, Config, Debug, Trace = Value

  }


  @inline def apply[F[_]](implicit instance: Log[F]): Log[F] = instance

  /**
    * Synchronous logger instance. Resulting `F` after logging statement will not complete,
    * until log record is committed to underlying logging provider.
    *
    * Neither the message nor details are materialized if the provider won't accept the message in supplied context and level.
    */
  def sync[F[_] : Sync : LoggerProvider]: Resource[F, Log[F]] = {
    Resource.pure {
      new Log[F] {
        def log(level: Level.Value, message: => String, detail: => Detail)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
          Sync[F].suspend {
            if (LoggerProvider[F].shouldLog(level, ctx))
              LogRecord(level, Instant.now, Eval.later(message), Eval.later(detail), line, file, ctx).toProvider
            else
              Applicative[F].unit
          }

      }
    }
  }


  /**
    * Asynchronous logger instance.
    *
    * Resulting `F` completes immediately once all logging event components are captured and enqueued for processing.
    * Then, in single thread the events are processed to supplied logging provider.
    *
    * This is useful if you do not want to block the main path of the program with any delay our logging (i.e. witting to file)
    * shall incur. Also note that any `message` or `detail` passed to log, is materialized after the log event is submitted, effectively
    * preventing program from possible expensive operation (i.e. converting details to string).
    *
    *
    * This logger incurs in total larger overhead than `sync` variant, but gives much more predictable performance
    * penalty while logging.
    *
    * The time is captured when the message is submitted to queue, to provide accurate timing. That means, however that
    * in high concurrent scenarios messages may be output to provider out of time order, however with correct timestamp.
    *
    * @tparam F
    * @return
    */
  def async[F[_] : Concurrent : LoggerProvider]: Resource[F, Log[F]] = {
    def asyncLog(source: Stream[F, LogRecord], done: F[Unit]): F[Unit] =
      Concurrent[F].start {
        Sync[F].guarantee(
          source.evalMap(_.toProvider).compile.drain
        )(done)
      }.void

    def acquire: F[(Log[F], F[Unit], F[Unit])] = {
      Deferred[F, Unit].flatMap { done =>
      Queue.noneTerminated[F, LogRecord].flatMap { queue =>
        val log =
         new Log[F] {
           def log(level: Level.Value, message: => String, detail: => Detail)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
             Sync[F].suspend {
               if (! LoggerProvider[F].shouldLog(level, ctx))
                 Applicative[F].unit
               else
                 queue.enqueue1(Some(LogRecord(level, Instant.now, Eval.later(message), Eval.later(detail), line, file, ctx)))
             }
         }

        asyncLog(queue.dequeue, done.complete(())) as ((log, queue.enqueue1(None), done.get))
      }}
    }

    def release(log: Log[F], stop: F[Unit], awaitDone: F[Unit]): F[Unit] =
      stop >> awaitDone

    Resource.make(acquire)(release _ tupled).flatMap { case (log, _, _) => Resource.pure(log) }
  }

}
