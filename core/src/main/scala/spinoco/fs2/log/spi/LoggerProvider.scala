package spinoco.fs2.log.spi

import java.time.Instant

import spinoco.fs2.log.{LogContext, Log}

trait LoggerProvider[F[_]] {

  /**
    * Must yield to true/false if logger accepts logging for given level and loggerName.
    *
    * Note that as an optimization, this may be effect-full function relying on some external state (i.e. ConcurrentMap)
    * In the `Log` itself it will be used and guarded by `F`.
    *
    * Note that this must not ever throw an exception.
    *
    * @param level        Level of the logger
    * @param context      Context of the logger, where the log entry shall be populated.
    *
    */
  def shouldLog(level: Log.Level.Value, context: LogContext): Boolean

  /**
    * Logs supplied log event
    *
    * @param level        Level of the logger
    * @param context      Context of the logger, where the log entry shall be populated.
    * @param time         Time of the record
    * @param message      Message to log
    * @param details      Details to log with message
    * @param line         Line of the event
    * @param file         File of the event
    * @param thrown      Captured exception, if any.
    * @return
    */
  def log(
    level: Log.Level.Value
    , context: LogContext
    , time: Instant
    , message: String
    , details: Map[String, String]
    , line: Int
    , file: String
    , thrown: Option[Throwable]
   ): F[Unit]

}


object LoggerProvider {

  @inline def apply[F[_]](implicit instance: LoggerProvider[F]): LoggerProvider[F] = instance


}