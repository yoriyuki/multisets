package scala.collection.mutable

import scala.collection._

trait BagLike[A, This <: mutable.BagLike[A, This] with mutable.Bag[A]]
  extends collection.BagLike[A, This]
  with GenBagLike[A, This]
  with generic.Subtractable[A, This]
  with generic.GrowableBag[A] {
  self =>

  protected override type BagBucket[X] = mutable.BagBucket[X]
  protected override type BagBucketFactory[X] = mutable.BagBucketFactory[X]


  override protected[this] def newBuilder: mutable.BagBuilder[A, This] = new mutable.GrowingBagBuilder[A, This](empty)

}
