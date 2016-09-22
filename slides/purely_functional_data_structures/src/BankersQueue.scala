package pfdstest

class BankersQueue[+T](lenf: Int, f: Lazy[Stream[T]], lenr: Int, r: Stream[T]) extends Queue[T]
{
  def isEmpty: Boolean = lenf == 0
  def enqueue[U >: T](u: U): BankersQueue[U] = BankersQueue.check(lenf, f, lenr + 1, Stream.Cons(u, r))
  def dequeue: (T, BankersQueue[T]) = f.force match {
    case Stream.Empty       => throw new RuntimeException("Empty!")
    case Stream.Cons(h, f2) => (h, BankersQueue.check(lenf - 1, f2, lenr, r))
  }
}

object BankersQueue
{
  def empty[T]: BankersQueue[T] = new BankersQueue(0, Lazy(Stream.empty[T]), 0, Stream.empty[T])

  private def check[T](lenf: Int, f: Lazy[Stream[T]], lenr: Int, r: Stream[T]): BankersQueue[T] = {
    if(lenf < lenr)
      new BankersQueue(lenf + lenr, Lazy(f.force ++ r.reverse), 0, Stream.Empty)
    else
      new BankersQueue(lenf, f, lenr, r)
  }
}
