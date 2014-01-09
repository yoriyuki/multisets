package scala.collection


import scala.collection.generic.{CanBuildFrom, Subtractable}
import scala.collection.immutable.RedBlackTree


/** A template trait for bag collections of type `Bag[A]`.
  *
  * Note: This trait replaces every method that uses `break` in
  * `TraversableLike` by an iterator version.
  *
  * @author Nicolas Stucki
  * @tparam A    the element type of the collection
  * @tparam This the type of the actual collection containing the elements.
  *
  * @define Coll Bag
  * @define coll bag collection
  */
trait BagLike[A, +This <: BagLike[A, This] with Bag[A]]
  extends IterableLike[A, This]
  with GenBagLike[A, This]
  with (A => This)
  with Subtractable[A, This] {
  self =>

  def empty: This

  override protected[this] def newBuilder: mutable.BagBuilder[A, This] = mutable.BagBuilder(empty)

  def apply(elem: A): This = getBucket(elem) match {
    case Some(bucket) => (newBuilder addBucket bucket).result()
    case None => empty
  }


  def contains(elem: A): Boolean = repr.multiplicity(elem) > 0

  /** Returns a Bag with the most common elements up to the equivalence defined in the `BagConfiguration`
    * @return An bag with the most common elements up to equivalence
    */
  def mostCommon: This = {
    if (isEmpty) return empty

    val maxSize = bucketsIterator.map(_.size).max
    val b = newBuilder
    for (bucket <- bucketsIterator if maxSize == bucket.size) {
      b addBucket bucket
    }
    b.result()
  }

  /** Returns a Bag with the least common elements up to the equivalence defined in the `BagConfiguration`
    * @return An bag with the least common elements up to equivalence
    */
  override def leastCommon: This = {
    if (isEmpty) return empty

    val minM = bucketsIterator.map(_.size).min
    val b = newBuilder
    for (bucket <- bucketsIterator if minM == bucket.size) {
      b addBucket bucket
    }
    b.result()
  }

  def distinct: This = {
    val b = newBuilder
    for (bucket <- bucketsIterator; elem <- bucket.distinctIterator)
      b += elem
    b.result()
  }

  // Added elements
  def +(elem: A): This = this added(elem, 1)


  def added(elem: A, count: Int): This = this.addedBucket(bagConfiguration.bucketFrom(elem, count))

  def added(elemCount: (A, Int)): This = added(elemCount._1, elemCount._2)

  def ++(elems: GenTraversableOnce[A]): This = {
    val b = newBuilder
    this.bucketsIterator.foreach(b addBucket _)
    elems.foreach(b += _)
    b.result()
  }


  // Added Bucket


  def getBucket(elem: A): Option[BagBucket] = bucketsIterator.find(bucket => bagConfiguration.equiv(elem, bucket.sentinel))

  def addedBucket(bucket: collection.BagBucket[A]): This = getBucket(bucket.sentinel) match {
    case Some(bucket2) => updatedBucket(bagConfiguration.bucketFrom(bucket, bucket2))
    case None => updatedBucket(bagConfiguration.bucketFrom(bucket))
  }

  /**
   * Put or replace the bucket associated with the new bucket
   * @param bucket  new bucket
   * @return the bag with the new bucket in place
   */
  protected def updatedBucket(bucket: BagBucket): This

  /**
   * Returns a bag containing the multi-set union of this `bag` and `that` bag
   * @param that the other bag
   * @return bag containing the multi-set union of this `bag` and `that` bag
   */
  def union(that: GenBag[A]): This = {
    val b = newBuilder
    this.bucketsIterator.foreach(b addBucket _)
    that.bucketsIterator.foreach(b addBucket _)
    b.result()
  }

  def ++(that: GenBag[A]) = this union that


  /**
   * Returns a bag containing the multi-set difference of this `bag` with `that` bag
   * @param that the other bag
   * @return bag containing the multi-set difference of this `bag` and `that` bag
   */
  def diff(that: GenBag[A]): This = {
    val b = newBuilder
    for (bucket <- bucketsIterator) {
      that.getBucket(bucket.sentinel) match {
        case Some(bucket2) =>
          val diffBucket = bucket diff bucket2
          if (diffBucket.nonEmpty)
            b addBucket diffBucket
        case None =>
          b addBucket bucket
      }
    }
    b.result()
  }

  def --(that: GenBag[A]): This = this diff that


  /**
   * Returns a bag containing the multi-set max union (or generalized set union) of this `bag` and `that` bag
   * @param that the other bag
   * @return bag containing the multi-set max union of this `bag` and `that` bag
   */
  def maxUnion(that: GenBag[A]): This = {
    val b = newBuilder
    val seen = mutable.Set.empty[A]

    for (bucket <- this.bucketsIterator; elem <- bucket.distinctIterator) {
      b.add(elem, Math.max(bucket.multiplicity(elem), that.multiplicity(elem)))
      seen += elem
    }
    for (bucket <- that.bucketsIterator; elem <- bucket.distinctIterator) {
      if (!seen(elem)) {
        b.add(elem, bucket.multiplicity(elem))
        seen += elem
      }
    }

    b.result()
  }


  /**
   * Returns a bag containing the multi-set intersection of this `bag` and `that` bag
   * @param that the other bag
   * @return bag containing the multi-set intersection of this `bag` and `that` bag
   */
  override def intersect(that: GenBag[A]): This = {
    val b = newBuilder
    for (bucket <- bucketsIterator) {
      that.getBucket(bucket.sentinel) match {
        case Some(bucket2) =>
          val intersectionBucket = bucket intersect bucket2
          if (intersectionBucket.nonEmpty)
            b addBucket intersectionBucket
        case None =>
      }
    }
    b.result()
  }

  // Removed elements
  def -(elem: A): This = removed(elem, 1)

  def -(elemCount: (A, Int)): This = removed(elemCount._1, elemCount._2)

  def removed(elem: A, count: Int): This = {
    val b = newBuilder

    for (bucket <- bucketsIterator) {
      if (bagConfiguration.equiv(bucket.sentinel, elem)) {
        val bucket2 = bucket.removed(elem, count)
        if (bucket2.nonEmpty)
          b addBucket bucket2

      } else {
        b addBucket bucket
      }
    }

    b.result()
  }

  def removedAll(elem: A): This = removedBucket(elem)

  def removedBucket(elem: A): This = {
    val b = newBuilder
    for (bucket <- bucketsIterator if !bagConfiguration.equiv(bucket.sentinel, elem)) {
      b addBucket bucket
    }
    b.result()
  }

  /**
   * Returns a mapping of the multiplicities of the bag
   * @return map containing the mapping of multiplicities of the of the bag 
   */
  def multiplicities: Map[A, Int] = new Multiplicities(repr)

  /**
   * Returns a bag with an updated multiplicity of some element
   * @param elem element that will have it's multiplicity changed
   * @param count number of times the element will be repeated in the bag
   * @return bag with the new multiplicity
   */
  def withMultiplicity(elem: A, count: Int): This = (this removedAll elem).added(elem, count)

  override def forall(p: (A) => Boolean): Boolean = bucketsIterator.forall(_.forall(p))

  override def exists(p: (A) => Boolean): Boolean = bucketsIterator.exists(_.exists(p))


  override def find(p: (A) => Boolean): Option[A] = {
    val it = bucketsIterator
    while (it.hasNext) {
      it.next().find(p) match {
        case value@Some(_) => return value
        case None =>
      }
    }
    None
  }


  override def foldLeft[B](z: B)(op: (B, A) => B): B = {
    var result = z
    this.bucketsIterator foreach (bucket => result = bucket.foldLeft[B](result)(op))
    result
  }


  override def take(n: Int): This = super.take(n)

  override def map[B, That](f: (A) => B)(implicit bf: CanBuildFrom[This, B, That]): That = super.map(f)(bf)


  override def sum[B >: A](implicit num: Numeric[B]): B = bucketsIterator.map(_.sum(num)).foldLeft(num.zero)(num.plus)

  override def product[B >: A](implicit num: Numeric[B]): B = bucketsIterator.map(_.product(num)).foldLeft(num.one)(num.times)

  override def min[B >: A](implicit cmp: Ordering[B]): A = bucketsIterator.map(_.min(cmp)).min(cmp)

  override def max[B >: A](implicit cmp: Ordering[B]): A = bucketsIterator.map(_.max(cmp)).max(cmp)


  override def reduceLeft[B >: A](op: (B, A) => B): B = {
    if (isEmpty)
      throw new UnsupportedOperationException("empty.reduceLeft")

    var first = true
    var acc: B = 0.asInstanceOf[B]

    for (bucket <- bucketsIterator if bucket.nonEmpty) {
      if (first) {
        acc = bucket.reduceLeft(op)
        first = false
      }
      else acc = bucket.foldLeft(acc)(op)
    }
    acc
  }

  override def count(p: (A) => Boolean): Int = {
    var cnt = 0
    for (bucket <- bucketsIterator)
      cnt += bucket.count(p)
    cnt
  }

}
