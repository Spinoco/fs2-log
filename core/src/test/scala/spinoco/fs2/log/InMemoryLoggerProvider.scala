package spinoco.fs2.log

import java.time.Instant

import cats.implicits._
import cats.effect.{Resource, Sync}
import cats.effect.concurrent.Ref
import spinoco.fs2.log.InMemoryLoggerProvider.Entry
import spinoco.fs2.log.spi.LoggerProvider

trait InMemoryLoggerProvider[F[_]] extends LoggerProvider[F] {

  def logged: F[Vector[Entry]]

}


object InMemoryLoggerProvider {

  case class Entry (
    level: Log.Level.Value
    , context: LogContext
    , time: Instant
    , message: String
    , details: Map[String, String]
    , line: Int
    , file: String
  )


  def instance[F[_] : Sync](maxLevel: Log.Level.Value): Resource[F, InMemoryLoggerProvider[F]] = Resource.liftF {
    Ref.of[F, Vector[Entry]](Vector.empty).map { ref =>
      new InMemoryLoggerProvider[F] {
        def logged: F[Vector[Entry]] = ref.get
        def shouldLog(level: Log.Level.Value, context: LogContext): Boolean = level.id <= maxLevel.id
        def log(level: Log.Level.Value, context: LogContext, time: Instant, message: String, details: Map[String, String], line: Int, file: String): F[Unit] =
          ref.update(_ :+ Entry(level, context, time, message, details, line, file))
      }

    }
  }

}