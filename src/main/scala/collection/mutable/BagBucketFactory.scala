package scala.collection.mutable

import scala.collection.{immutable, mutable}

trait BagBucketFactory[A]
  extends collection.BagBucketFactory[A, mutable.BagBucket[A]] {

  def newBuilder(sentinel: A) = empty(sentinel)
}


object BagBucketFactory {

  def ofMultiplicities[A] = new MultiplicityBagBucketFactory[A]

  def ofSeq[A] = new SeqBagBucketFactory[A]


  class MultiplicityBagBucketFactory[A] extends scala.collection.mutable.BagBucketFactory[A] {
    def empty(sentinel: A) = new mutable.MultiplicityBagBucket[A](sentinel, 0)
  }


  class SeqBagBucketFactory[A] extends scala.collection.mutable.BagBucketFactory[A] {
    def empty(sentinel: A): mutable.BagBucket[A] = new mutable.SeqBagBucket[A](sentinel, immutable.Seq.empty[A])
  }

}

