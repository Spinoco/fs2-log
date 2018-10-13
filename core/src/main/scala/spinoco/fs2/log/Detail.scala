package spinoco.fs2.log

import cats.Show
import sourcecode.Text

trait Detail {

  /** appends another detail to log, capturing variable as key **/
  def and[A : Show](a: Text[A]): Detail

  /** appends another detail to log with key `k` **/
  def and[A : Show](k: String, a: A): Detail

  /** appends supplied detail to this detail **/
  def append(detail: Detail): Detail

  /** dumps so far collected data **/
  private[log] def dump: Map[String, String]
}



object Detail {

  val empty: Detail = mk(Map.empty)



  def apply[A : Show](a: Text[A]): Detail =
    mk { Map(a.source -> Show[A].show(a.value)) }


  def as[A: Show](k: String, a: A): Detail =
    mk { Map(k -> Show[A].show(a)) }

  private def mk(data: Map[String, String]):Detail = {
    new Detail {
      def and[A: Show](a: Text[A]): Detail =  mk {
        val arg = a
        data + (arg.source -> Show[A].show(arg.value))
      }

      def and[A: Show](k: String, a: A): Detail =
        mk { data + (k -> Show[A].show(a)) }


      def append(detail: Detail): Detail =
        mk { data ++ detail.dump }

      private[log] def dump = data
    }
  }
}
