package pfdstest

class Lazy[+A](thunk: => A) {
  private lazy val value: A = thunk

  def force: A = value
}

object Lazy {
  def apply[A](thunk: => A): Lazy[A] = new Lazy[A](thunk)
}

sealed trait Stream[+T]
{
  def isEmpty: Boolean
  def ++[U >: T](rhs: Stream[U]): Stream[U]
  def reverse: Stream[T]
}

object Stream
{
  def empty[T]: Stream[T] = Empty

  object Empty extends Stream[Nothing]
  {
    def isEmpty: Boolean = true
    def ++[U](rhs: Stream[U]): Stream[U] = rhs
    def reverse: Stream[Nothing] = Empty
  }

  case class Cons[+T](t: T, s: Lazy[Stream[T]]) extends Stream[T]
  {
    def isEmpty: Boolean = false
    def ++[U >: T](rhs: Stream[U]): Stream[U] = Cons(t, Lazy(s.force ++ rhs))
    def reverse: Stream[T] = {
      def reverseImpl(a: Stream[T], b: Stream[T]): Stream[T] = a match {
        case Empty       => b
        case Cons(h, a2) => reverseImpl(a2.force, Cons(h, b))
      }
      reverseImpl(this, Empty)
    }
  }
  object Cons
  {
    def apply[T](t: T, s: Stream[T]): Stream[T] = Cons(t, Lazy(s))
  }

  def fromList[T](l: List[T]): Stream[T] = l match {
    case Nil      => Empty
    case hd :: tl => Cons(hd, Lazy(fromList(tl)))
  }
}
