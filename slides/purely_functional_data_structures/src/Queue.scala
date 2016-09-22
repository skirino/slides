package pfdstest

trait Queue[+T]
{
  def isEmpty: Boolean
  def enqueue[U >: T](u: U): Queue[U]
  def dequeue: (T, Queue[T])
}
