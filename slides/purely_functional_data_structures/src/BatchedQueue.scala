package pfdstest

class BatchedQueue[+T](f: List[T], r: List[T]) extends Queue[T]
{
  def isEmpty: Boolean = f.isEmpty
  def enqueue[U >: T](u: U): BatchedQueue[U] = BatchedQueue.check(f, u :: r)
  def dequeue: (T, BatchedQueue[T]) = f match {
    case Nil    => throw new RuntimeException("Empty!")
    case h :: t => (h, BatchedQueue.check(t, r))
  }
}

object BatchedQueue
{
  def empty[T] = new BatchedQueue(Nil, Nil)

  private def check[T](newf: List[T], newr: List[T]): BatchedQueue[T] = {
    if(newf.isEmpty)
      new BatchedQueue(newr.reverse, Nil)
    else
      new BatchedQueue(newf, newr)
  }
}
