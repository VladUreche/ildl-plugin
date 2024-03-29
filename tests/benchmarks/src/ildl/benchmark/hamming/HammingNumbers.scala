package ildl
package benchmark
package hamming

object HammingNumbers {

  import scala.collection.mutable.Queue

  // we want to be able to enqueue a single element at once
  implicit class QueueWithEnqueue1[T](val q: Queue[T]) extends AnyVal {
    def enqueue1(t: T) = q.enqueue(t)
  }

  // taken from http://rosettacode.org/wiki/Hamming_numbers#Scala
  class HammingDirect extends Iterator[Long] {
    val q2 = new Queue[Long]
    val q3 = new Queue[Long]
    val q5 = new Queue[Long]
    def enqueue(n: Long) = {
      q2 enqueue1 n * 2
      q3 enqueue1 n * 3
      q5 enqueue1 n * 5
    }
    def next = {
      val n = q2.head min q3.head min q5.head
      if (q2.head == n) q2.dequeue
      if (q3.head == n) q3.dequeue
      if (q5.head == n) q5.dequeue
      enqueue(n)
      n
    }
    def hasNext = true
    q2 enqueue 1
    q3 enqueue 1
    q5 enqueue 1
  }

  adrt(QueueOfLongAsFunnyQueue) {
    class HammingADRT extends Iterator[Long] {
      val q2 = new Queue[Long]
      val q3 = new Queue[Long]
      val q5 = new Queue[Long]
      def enqueue(n: Long) = {
        q2 enqueue1 n * 2
        q3 enqueue1 n * 3
        q5 enqueue1 n * 5
      }
      def next = {
        val n = q2.head min q3.head min q5.head
        if (q2.head == n) q2.dequeue
        if (q3.head == n) q3.dequeue
        if (q5.head == n) q5.dequeue
        enqueue(n)
        n
      }
      def hasNext = true
      q2 enqueue 1
      q3 enqueue 1
      q5 enqueue 1
    }
  }
}