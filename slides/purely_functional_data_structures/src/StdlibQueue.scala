package pfdstest

import scala.collection.immutable.{ Queue => SQueue }

class StdlibQueue[+T](q: SQueue[T]) extends Queue[T]
{
  def isEmpty: Boolean = q.isEmpty
  def enqueue[U >: T](u: U): StdlibQueue[U] = new StdlibQueue(q.enqueue(u))
  def dequeue: (T, StdlibQueue[T]) = {
    val (head, tail) = q.dequeue
    (head, new StdlibQueue(tail))
  }
}

object StdlibQueue
{
  def empty[T]: StdlibQueue[T] = new StdlibQueue(SQueue.empty[T])
}
