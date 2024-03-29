package ildl
package benchmark
package gcd

import org.scalameter.CurveData
import org.scalameter.api._
import org.scalameter.Key
import org.scalameter.DSL._

object BenchmarkRunner extends PerformanceTest.Microbenchmark {

  import GreatestCommonDivisor._

  val bench   = Gen.enumeration("bench")("direct", "adrt__")

  var data: List[(Double, Double)] = _
  var slope: Double = _
  var offset: Double = _
  val eps = 1E-6

  for (interp <- Seq(true, false))
    measure method "gcd" in {
      using(Gen.tupled(bench, Gen.single("jvm_interpreter")(interp))) config (
          exec.independentSamples -> 5,
          exec.benchRuns -> 5,
          exec.jvmflags -> flags(interp)
      ) setUp {
        _ =>
          val r1 = (10, 3)
          val r2 = gcd_direct((544,185), (131,181))
          val r3 = gcd_adrt((544,185), (131,181))
          assert(r2 == r1, r2.toString)
          assert(r3 == r1, r3.toString)
      } in {
        case (bench, _) =>
          bench match {
            case "direct" =>
              var i = 10000
              while (i > 0) {
                gcd_direct((544,185), (131,181))
                i -= 1
              }
            case "adrt__" =>
              var i = 10000
              while (i > 0) {
                gcd_adrt((544,185), (131,181))
                i -= 1
              }
          }
      }
    }

  def flags(interp: Boolean): String = interp match {
    case true =>  "-Xint"
    case false => ""
  }
}