package pfdstest

class RealtimeQueue[+T](f: Stream[T], r: List[T], s: Stream[T]) extends Queue[T]
{
  def isEmpty: Boolean = f.isEmpty
  def enqueue[U >: T](u: U): RealtimeQueue[U] = RealtimeQueue.check(f, u :: r, s)
  def dequeue: (T, RealtimeQueue[T]) = f match {
    case Stream.Empty       => throw new RuntimeException("Empty!")
    case Stream.Cons(h, f2) => (h, RealtimeQueue.check(f2.force, r, s))
  }
}

object RealtimeQueue
{
  def empty[T] = new RealtimeQueue(Stream.empty, Nil, Stream.empty)

  private def check[T](f: Stream[T], r: List[T], s0: Stream[T]): RealtimeQueue[T] = s0 match {
    case Stream.Cons(x, s) => new RealtimeQueue(f, r, s.force)
    case Stream.Empty      =>
      val f2 = rotate(f, r, Stream.Empty)
      new RealtimeQueue(f2, Nil, f2)
  }

  private def rotate[T](xs0: Stream[T], ys0: List[T], a: Stream[T]): Stream[T] = ys0 match {
    case Nil     => throw new Exception("Logic error!")
    case y :: ys => xs0 match {
      case Stream.Empty       => Stream.Cons(y, a)
      case Stream.Cons(x, xs) => Stream.Cons(x, Lazy(rotate(xs.force, ys, Stream.Cons(y, a))))
    }
  }
}
