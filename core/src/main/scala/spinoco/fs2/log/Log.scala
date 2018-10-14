package spinoco.fs2.log

import java.time.Instant

import cats.implicits._
import cats.{Applicative, Eval, Show}
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
  def log(level: Log.Level.Value, message: => String, detail: => Detail = Detail.empty, thrown: Option[Throwable] = None)(implicit line: Line, file: File, ctx: LogContext): F[Unit]

  /** alias for log(Error, message, detail, thrown) **/
  @inline def error(message: => String, detail: => Detail = Detail.empty, thrown: Option[Throwable] = None)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Error, message, detail, thrown)

  /** alias for log(Warn, message, detail, thrown) **/
  @inline def warn(message: => String, detail: => Detail = Detail.empty, thrown: Option[Throwable] = None)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Warn, message, detail, thrown)

  /** alias for log(Info, message, detail, thrown) **/
  @inline def info(message: => String, detail: => Detail = Detail.empty, thrown: Option[Throwable] = None)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Info, message, detail, thrown)

  /** alias for log(Config, message, detail, thrown) **/
  @inline def config(message: => String, detail: => Detail = Detail.empty, thrown: Option[Throwable] = None)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Config, message, detail, thrown)

  /** alias for log(Debug, message, detail, thrown) **/
  @inline def debug(message: => String, detail: => Detail = Detail.empty, thrown: Option[Throwable] = None)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Debug, message, detail, thrown)

  /** alias for log(Trace, message, detail, thrown) **/
  @inline def trace(message: => String, detail: => Detail = Detail.empty, thrown: Option[Throwable] = None)(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
    log(Log.Level.Trace, message, detail, thrown)

  /**
    * Observes execution of `f`. When `f` completes successfully,
    * then this will log value `A` as detail with `A` value converted to string via supplied Show.
    *
    * Note that when `f` fails this will not log the failure. Use `observeRaised` if you want to capture failures
    *
    * @param level      Level to log message with
    * @param message    Message to log
    * @param detail     Any details to attach with message
    * @param f          A `f` to run
    */
  def observe[A : Show](level: Log.Level.Value, message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A]

  /** alias for observe(Error, message, detail)(f) **/
  @inline def observeAsError[A: Show](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observe(Log.Level.Error, message, detail)(f)

  /** alias for observe(Warn, message, detail)(f) **/
  @inline def observeAsWarn[A: Show](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observe(Log.Level.Warn, message, detail)(f)

  /** alias for observe(Info, message, detail)(f) **/
  @inline def observeAsInfo[A: Show](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observe(Log.Level.Info, message, detail)(f)

  /** alias for observe(Config, message, detail)(f) **/
  @inline def observeAsConfig[A: Show](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observe(Log.Level.Config, message, detail)(f)

  /** alias for observe(Debug, message, detail)(f) **/
  @inline def observeAsDebug[A: Show](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observe(Log.Level.Debug, message, detail)(f)

  /** alias for observe(Trace, message, detail)(f) **/
  @inline def observeAsTrace[A: Show](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observe(Log.Level.Trace, message, detail)(f)

  /**
    * Observes execution of `f`. When `f` completes with raised failure,
    * then this will log that failure, and rethrow the failure
    *
    * Note that when `f` completes successfully this will not log the value.
    * If you want to log the value use `observe`.
    *
    * @param level      Level to log message with
    * @param message    Message to log
    * @param detail     Any details to attach with message
    * @param f          A `f` to run
    */
  def observeRaised[A](level: Log.Level.Value, message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A]


  /** alias for observeRaised(Error, message, detail)(f) **/
  @inline def observeRaisedAsError[A](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observeRaised(Log.Level.Error, message, detail)(f)

  /** alias for observeRaised(Warn, message, detail)(f) **/
  @inline def observeRaisedAsWarn[A](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observeRaised(Log.Level.Warn, message, detail)(f)

  /** alias for observeRaised(Info, message, detail)(f) **/
  @inline def observeRaisedAsInfo[A](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observeRaised(Log.Level.Info, message, detail)(f)

  /** alias for observeRaised(Config, message, detail)(f) **/
  @inline def observeRaisedAsConfig[A](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observeRaised(Log.Level.Config, message, detail)(f)

  /** alias for observeRaised(Debug, message, detail)(f) **/
  @inline def observeRaisedAsDebug[A](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observeRaised(Log.Level.Debug, message, detail)(f)

  /** alias for observeRaised(Trace, message, detail)(f) **/
  @inline def observeRaisedAsTrace[A](message: => String, detail: => Detail = Detail.empty)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
    observeRaised(Log.Level.Trace, message, detail)(f)

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
      new CommonLog[F] {
        @inline def log_(level: Level.Value, message: => String, detail: => Detail, thrown: Option[Throwable])(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
        LogRecord(level, Instant.now, Eval.later(message), Eval.later(detail), line, file, ctx, thrown).toProvider
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
          new CommonLog[F] {
            @inline def log_(level: Level.Value, message: => String, detail: => Detail, thrown: Option[Throwable])(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
              queue.enqueue1(Some(LogRecord(level, Instant.now, Eval.later(message), Eval.later(detail), line, file, ctx, thrown)))

          }

        asyncLog(queue.dequeue, done.complete(())) as ((log, queue.enqueue1(None), done.get))
      }}
    }

    def release(log: Log[F], stop: F[Unit], awaitDone: F[Unit]): F[Unit] =
      stop >> awaitDone

    Resource.make(acquire)(release _ tupled).flatMap { case (log, _, _) => Resource.pure(log) }
  }


  private abstract class CommonLog[F[_] : Sync : LoggerProvider] extends Log[F] {
    @inline def log_(level: Level.Value, message: => String, detail: => Detail, thrown: Option[Throwable])(implicit line: Line, file: File, ctx: LogContext): F[Unit]

    def log(level: Level.Value, message: => String, detail: => Detail, thrown: Option[Throwable])(implicit line: Line, file: File, ctx: LogContext): F[Unit] =
      Sync[F].suspend {
        if (! LoggerProvider[F].shouldLog(level, ctx)) Applicative[F].unit
        else log_(level, message, detail, thrown)
      }

    def observe[A: Show](level: Level.Value, message: => String, detail: => Detail)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
      Sync[F].suspend {
        if (!LoggerProvider[F].shouldLog(level, ctx)) f
        else f.flatTap { a => log_(level, message, detail append Detail.as("value", a), None) }
      }

    def observeRaised[A](level: Level.Value, message: => String, detail: => Detail)(f: F[A])(implicit line: Line, file: File, ctx: LogContext): F[A] =
      Sync[F].suspend {
        if (!LoggerProvider[F].shouldLog(level, ctx)) f
        else f.handleErrorWith { thrown =>
          log_(level, message, detail, Some(thrown)) *>
            Sync[F].raiseError(thrown)
        }
      }
  }

}
