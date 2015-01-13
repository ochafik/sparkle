package nest.sparkle.datastream

/** An interval with optional start and end boundaries
  */
case class SoftInterval[T](start: Option[T] = None, until: Option[T] = None)