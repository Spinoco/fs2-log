package spinoco.fs2.log

import fs2._
import cats.implicits._
import cats.effect.{IO, Resource}
import org.scalacheck.Prop._
import org.scalacheck.Properties


object LogSpec extends Properties("LogSpec") {

  property("will.log") = protect {
    val logged =
    InMemoryLoggerProvider.instance[IO](Log.Level.Trace).flatMap { implicit provider =>
      Log.sync[IO].flatMap { log => Resource.pure(log -> provider) }
    }.use { case (log, provider) =>
      def hello = "THAT"
      val dolly = 123
      log.config("Messy", Detail(hello) and dolly) >>
      provider.logged
    }.unsafeRunSync()

    (logged.size ?= 1) &&
    (logged.map(_.details) ?= Vector(Map("hello" -> "THAT", "dolly" -> "123")))
  }


  property("observes") = protect {
    val logged =
      InMemoryLoggerProvider.instance[IO](Log.Level.Trace).flatMap { implicit provider =>
        Log.sync[IO].flatMap { log => Resource.pure(log -> provider) }
      }.use { case (log, provider) =>
        def action = IO { 1 }
        def failure: IO[Int] = IO.raiseError(new Throwable("Boom"))

        log.observeAsInfo("action")(action) >>
        log.observeAsInfo("action")(failure).attempt >>
        log.observeRaisedAsError("failure")(failure).attempt >>
        log.observeRaisedAsError("failure")(action) >>
        provider.logged
      }.unsafeRunSync()

    (logged.size ?= 2) &&
    (logged.map(_.message) ?= Vector("action", "failure"))
  }


  property("logs-stream") = protect {
    import spinoco.fs2.log.syntax._
    val logged =
    Stream.resource(InMemoryLoggerProvider.instance[IO](Log.Level.Trace)).flatMap { implicit provider =>
    Stream.resource(Log.sync[IO]).flatMap { implicit log =>
      val foo : Long = 1l
      Stream[IO, Int](1, 2, 3).log(Log.Level.Info, "That", Detail(foo)).last >>
      Stream.evalUnChunk(provider.logged.map(Chunk.vector))
    }}.compile.toVector.unsafeRunSync()


    (logged.size ?= 3) &&
      (logged.map(_.details) ?= Vector(
        Map("foo" -> "1", "value" -> "1")
        , Map("foo" -> "1", "value" -> "2")
        , Map("foo" -> "1", "value" -> "3")
      ))
  }

}
