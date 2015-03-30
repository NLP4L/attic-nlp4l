package org.nlp4l.stats

/**
 * Utility object to calculate various statistics.
 */
object Stats {

  /**
   * Calculate the average for given values.
   */
  def average(values: Iterable[Long]): Double = values.sum.toDouble / values.size

  /**
   * Calculate the variance for given values
   */
  def variance(values: Iterable[Long]): Double = {
    val ave = average(values)
    values.map(v => math.pow(v - ave, 2)).sum / values.size
  }

  /**
   * Calculate the covariance for given two list of values.
   */
  // TODO: verifying the sizes of both arguments are same?
  def covariance(vs1: Seq[Long], vs2: Seq[Long]): Double = {
    val ave1 = average(vs1)
    val ave2 = average(vs2)
    // using zip, if one of the two collections is longer than the other, its remaining elements are ignored.
    vs1.zip(vs2).map(t => (t._1 - ave1) * (t._2 - ave2)).sum / vs1.size
  }

  /**
   * Calculate the coefficient for given two list of values.
   */
  def correlationCoefficient(vs1: Seq[Long], vs2: Seq[Long]): Double = {
    val cov = covariance(vs1, vs2)
    val var1 = variance(vs1)
    val var2 = variance(vs2)
    val vv = math.sqrt(var1 * var2)
    cov / vv
  }

  /**
   * Calculate the chi-square without yetes correction.
   * @param wc1
   * @param ocs1
   * @param wc2
   * @param ocs2
   * @param yatesCorrection
   * @return
   */
  def chiSquare(wc1: Long, ocs1: Iterable[Long], wc2: Long, ocs2: Iterable[Long], yatesCorrection: Boolean): Double = {
    chiSquare(wc1, ocs1.sum, wc2, ocs2.sum, yatesCorrection)
  }

  /**
   * Calculate the chi-square.
   * @param wc1
   * @param oc1
   * @param wc2
   * @param oc2
   * @param yatesCorrection
   * @return
   */
  def chiSquare(wc1: Long, oc1: Long, wc2: Long, oc2: Long, yatesCorrection: Boolean): Double = {
    val sumwc = (wc1 + wc2).toDouble
    val sumoc = (oc1 + oc2).toDouble
    val sum1 = (oc1 + wc1).toDouble
    val sum2 = (oc2 + wc2).toDouble
    val gs = sum1 + sum2

    val expwc1 = sum1 / gs * sumwc
    val expoc1 = sum1 / gs * sumoc
    val expwc2 = sum2 / gs * sumwc
    val expoc2 = sum2 / gs * sumoc

    val oewc1 = averagedSquaredError(wc1, expwc1, yatesCorrection)
    val oewc2 = averagedSquaredError(wc2, expwc2, yatesCorrection)
    val oeoc1 = averagedSquaredError(oc1, expoc1, yatesCorrection)
    val oeoc2 = averagedSquaredError(oc2, expoc2, yatesCorrection)

    oewc1 + oewc2 + oeoc1 + oeoc2
  }

  private def averagedSquaredError(rc: Long, expc: Double, yatesCorrection: Boolean): Double = {
    if(yatesCorrection)
      ((rc - expc).abs - 0.5) * ((rc - expc).abs - 0.5) / expc
    else
      (rc - expc) * (rc - expc) / expc
  }
}
