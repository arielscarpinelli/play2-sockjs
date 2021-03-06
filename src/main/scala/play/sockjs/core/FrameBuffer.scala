package play.sockjs.core

import scala.collection.mutable

/**
 * Mutable queue based buffer used by session actors.
 * Contiguos MessageFrame are aggregated as one.
 */
private[sockjs] class FrameBuffer {

  private[this] val queue = mutable.Queue[Frame]()
  private[this] var last: Frame = _

  def isEmpty: Boolean = last eq null

  def enqueue(frame: Frame) {
    (last, frame) match {
      case (null, f) => last = f
      case (f1: Frame.MessageFrame, f2: Frame.MessageFrame) => last = f1 ++ f2
      case (f1, f2) => queue.enqueue(f1); last = f2
    }
  }

  def dequeue(): Frame = {
    if (!queue.isEmpty) queue.dequeue()
    else if (!isEmpty) {val cur = last; last = null; cur}
    else throw new NoSuchElementException("queue empty")
  }

}
