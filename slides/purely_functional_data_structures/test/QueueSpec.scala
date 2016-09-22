package pfdstest

import java.util.Date
import scala.util.{ Try, Random }
import org.scalacheck.{ Properties, Prop }

trait QueueSpecification
{
  self: Properties =>

  val empty: Queue[Int]

  def stopwatch[T](f: => T): (T, Long) = {
    val d1 = new Date
    val ret = f
    (ret, (new Date).getTime - d1.getTime)
  }

  property("empty queue") = Prop.forAll { (i: Int) =>
    val b1 = empty.isEmpty
    val b2 = !empty.enqueue(i).isEmpty
    val b3 = Try { empty.dequeue }.isFailure
    b1 && b2 && b3
  }

  property("enqueue and then dequeue") = Prop.forAll { (l: List[Int]) =>
    def buildList(q: Queue[Int], acc: List[Int]): List[Int] = {
      if(q.isEmpty) {
        acc
      } else {
        val (head, tail) = q.dequeue
        buildList(tail, head :: acc)
      }
    }
    val q0 = l.foldLeft(empty) { (q, elem) => q.enqueue(elem) }
    val l2 = buildList(q0, Nil)
    l2 == l.reverse
  }

  property("measure enqueue/dequeue time") = Prop.exists { (u: Unit) =>
    val count = 1000000

    val l = List.fill(count) { Random.nextInt }
    val (q1, total1) = stopwatch {
      l.foldLeft(empty) { case (q, elem) => q.enqueue(elem) }
    }
    println(s"Enqueue ${count} times: total time = " + total1 + ", average = " + total1.toDouble / count)

    val (q2, total2) = stopwatch {
      (1 to count).foldLeft(q1) { case (q, _) => q.dequeue._2 }
    }
    println(s"Dequeue ${count} times: total time = " + total2 + ", average = " + total2.toDouble / count)

    !q1.isEmpty && q2.isEmpty
  }

  property("measure pivot cost during dequeues") = Prop.exists { (u: Unit) =>
    val n = 1024 * 1024 - 2
    val q = (1 to n).foldLeft(empty) { (q0, i) => q0.enqueue(i) }
    val count = 100
    val (_, time) = stopwatch { (1 to 100).foreach { _ => q.dequeue } }
    println(s"Dequeue ${count} times: total time = " + time)
    !q.isEmpty
  }
}

object StdlibQueueSpecification extends Properties("StdlibQueue") with QueueSpecification
{
  val empty = StdlibQueue.empty[Int]
}

object BatchedQueueSpecification extends Properties("BatchedQueue") with QueueSpecification
{
  val empty = BatchedQueue.empty[Int]
}

object BankersQueueSpecification extends Properties("BankersQueue") with QueueSpecification
{
  val empty = BankersQueue.empty[Int]
}

object RealtimeQueueSpecification extends Properties("RealtimeQueue") with QueueSpecification
{
  val empty = RealtimeQueue.empty[Int]
}
