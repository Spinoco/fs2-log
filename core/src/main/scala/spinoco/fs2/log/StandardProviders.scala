package spinoco.fs2.log

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

import cats.effect.{Resource, Sync}
import spinoco.fs2.log.spi.LoggerProvider

object StandardProviders {

  /** simple log to console, that logs any events, where level <= maxLevel **/
  def console[F[_] : Sync](maxLevel: Log.Level.Value): Resource[F, LoggerProvider[F]] = Resource.pure {
    new LoggerProvider[F] {

      val timeFormat = DateTimeFormatter.ISO_INSTANT

      def shouldLog(level: Log.Level.Value, context: LogContext): Boolean =
        level.id <= maxLevel.id


      def log(level: Log.Level.Value, context: LogContext, time: Instant, message: String, details: Map[String, String], line: Int, file: String): F[Unit] =
        Sync[F].delay {
          println(s"${timeFormat.format(time)} ${level} ${context.name} ${message} ${details} @ $file:$line")
        }

    }
  }


  /**
    * Provider for java util logging
    *
    * Logger names are taken from Context.name.
    *
    */
  def juliProvider[F[_] : Sync]: Resource[F, LoggerProvider[F]] = Resource.pure {
    new LoggerProvider[F] {

      def toJuliLevel(level: Log.Level.Value): java.util.logging.Level = {
        level match {
          case Log.Level.Error => java.util.logging.Level.SEVERE
          case Log.Level.Warn => java.util.logging.Level.WARNING
          case Log.Level.Info => java.util.logging.Level.INFO
          case Log.Level.Config => java.util.logging.Level.CONFIG
          case Log.Level.Debug => java.util.logging.Level.FINE
          case Log.Level.Trace => java.util.logging.Level.FINER

        }
      }

      def shouldLog(level: Log.Level.Value, context: LogContext): Boolean =
        Logger.getLogger(context.name).isLoggable(toJuliLevel(level))

      def log(level: Log.Level.Value, context: LogContext, time: Instant, message: String, details: Map[String, String], line: Int, file: String): F[Unit] =
        Sync[F].delay {
          def msg =s"$message ${details.map { case (k, v) => s"$k -> $v"}.mkString("(",",",")")} @ ($file:$line)"
          Logger.getLogger(context.name).log(new java.util.logging.LogRecord(toJuliLevel(level), msg))
        }

    }

  }

}
