package spinoco.fs2.log

import sourcecode.FullName

trait LogContext {
  /** Name of the context, typically name of the class **/
  def name: String

}


object LogContext {

  /**
    * Provides default logging context that builds context from the name of closest enclosing class.
    */
  implicit def defaultInstance(implicit fullName: FullName): LogContext =
    new LogContext {
      def name: String = fullName.value
    }


}