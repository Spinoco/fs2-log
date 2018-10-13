# fs2-log
Simple logging facade for fs2

This library provides simple logging facade over standard java logging frameworks so it will be easier and more pleasant to work with. 

It is based on cats.effects, and also has some syntax sugar for fs2._

To get going, just use in SBT:


```
libraryDependencies += "com.spinoco" %% "fs2-log-core" % "0.1.0"

```

Now, you shold just `import spinoco.fs2.log._` and you should be good to go.


## Logging

Logging is available via `Log` instance that shall be passed along as implicit to constructs where you need to schedule your logs. 

For example 

```scala

package p

trait Foo[F[_]] {

 def myImportandMethod(implicit log: Log[F]): F[Unit] = {
  doSomeStuff <* log.info("That was fun")  
 }
 
}

```

### Displaying and capturing data

Apart from logging simple statements you may also dump your local/global variables or simple expressions such as 

```scala 

trait Foo[F[_]] {

 def myImportandMethod(pars: String)(implicit log: Log[F]): F[Unit] = {
  doSomeStuff <* log.info("That was fun", Display(pars)) 
 }
 
}

```

This logging statement above will pass to your logger implementation message and also Map("pars" -> "_value_") if the method was invoked with "_value_" as its parameter. Note that this actually captures name of your variable automatically. 

### LogContext

Each loging statement implicitly looks for `LogContext`. Default logContext is simple string name, which shall resemble to FQDN of the class where the logging statement is present. 

As we do not want always provide it, there is _default_ instance of `LogContext` implicitly summoned, if none is available that materializes `LogContext` via macro as FQDN of enclosing class. 

There may be custom `LogContext`'s provided, that may capture also other type of data (i.e. thread name) and pass them along for logger to use them when writing log statement. 

### Capturing source file and line

fs2-log is also with every log statement capturing the actual line in source code and passing them along to logger. This is acheived via macro, and shall have minimal runtime cost, while providing detail information about the logging statement. 


### Asynchronous logging and Delayed evaluation

Typically, the logging involves performing effects assotiated with File // stdout or even Networks. Cost of the logging when turned on may not be insignificant and in highly concurrent environemnt may also have an negative impact on performance overall, sometimes even misguiding you where ther problem really is. 

As such, fs2-log allows user to implement asynchronous logging and allows to switch between synchronous / asynchronous logging whenever required. 

`Log.async` construct logger, that allows to log statements asynchronously. Technically this captures timestamp, any variables, statements, strings etc. that shall appear in the log and submits them to the queue for further processing. This guarantees, that there is constant cost of the log operation, and also that eventually long and costly i/o operations does not have impact on the actual program, while still, there is correct timing captured. 

Currently asynchronous operation does not sort incoming events based on data, so the events may come to underlying logger implementation out of order (in millisecond windows).

`Log.sync` in contrast to asynchronous variant construct synchronous logging, and as such all effects are performed sequentially. However still, shall the logging be subject of any i/o failure or evaluating lazy captured variables will raise error, all these are captured and logged to stderr w/o implications to main program. 

### LoggingProvider

_fs2-log_ is only simple facade and syntax sugar. To hook-up java loggers, use `LoggingProvider` trait, which is passed as implicit to `Log.async` or `Log.sync` constructors.

To see simple examples how this is done, please see `StandardProviders` object. 

We will be adding more logging providers along the way. Should you miss yours, please feel free to open issue. 


### fs2 Stream syntax

Try to `import spinoco.fs2.log.syntax._` that will add to your streams `log` methods, that allows simple bilerplate free logging. 

```scala

val stream: Stream[F, A] = ???

import spinoco.fs2.log.syntax._

Stream.resource(StandardProviders.juliProvider[IO]).flatMap { implicit provider =>
Stream.resource(Log.async[IO]).flatMap { implicit log =>
  val myData: Long = 1l
  
  stream.log.info("Got next A ", Display(myData)).map {a => /* do something with `A`*/ }
  
}}.compile.drain.unsafeRunSync()

```



