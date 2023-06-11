import scala.language.higherKinds

package object kyo {

  type >[+T, -S] >: T // = T | Kyo[_, _, _, T, S]

  extension [T, S](v: T > S) {

    /*inline(3)*/
    def flatMap[U, S2](f: T => U > S2): U > (S with S2) =
      kyo.core.transform(v)(f)

    /*inline(3)*/
    def map[U, S2](f: T => U > S2): U > (S with S2) =
      flatMap(f)

    /*inline(3)*/
    def unit: Unit > S =
      map(_ => ())

    /*inline(3)*/
    def withFilter(p: T => Boolean): T > S =
      map(v => if (!p(v)) throw new MatchError(v) else v)

    /*inline(3)*/
    def flatten[U, S2](implicit ev: T => U > S2): U > (S with S2) =
      flatMap(ev)

    /*inline(3)*/
    def andThen[U, S2](f: => U > S2)(implicit ev: T => Unit): U > (S with S2) =
      flatMap(_ => f)

    /*inline(3)*/
    def repeat(i: Int)(implicit ev: T => Unit): Unit > S =
      if (i <= 0) () else andThen(repeat(i - 1))

    /*inline(3)*/
    def forever(implicit ev: T => Unit): Unit > S =
      andThen(forever)

    /*inline(3)*/
    def pure(implicit ev: Any => S): T =
      v.asInstanceOf[T]
  }

  def zip[T1, T2, S](v1: T1 > S, v2: T2 > S): (T1, T2) > S =
    v1.map(t1 => v2.map(t2 => (t1, t2)))

  def zip[T1, T2, T3, S](v1: T1 > S, v2: T2 > S, v3: T3 > S): (T1, T2, T3) > S =
    v1.map(t1 => v2.map(t2 => v3.map(t3 => (t1, t2, t3))))

  def zip[T1, T2, T3, T4, S](
      v1: T1 > S,
      v2: T2 > S,
      v3: T3 > S,
      v4: T4 > S
  ): (T1, T2, T3, T4) > S =
    v1.map(t1 => v2.map(t2 => v3.map(t3 => v4.map(t4 => (t1, t2, t3, t4)))))

}