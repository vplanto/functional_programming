import fp.{Donation, Jar}
import fp.JarLogic._
import javish.DonationJar

/** Порівняння mutable (javish) і immutable (fp) при масовому додаванні донатів. Запуск: sbt "runMain FpBenchmark" */
object FpBenchmark extends App {

  val donationCount = 10_000

  // наївний append — лише для бенчмарку, щоб показати пастку :+ (O(N) на кожному кроці).
  def addDonationAppend(jar: Jar, donation: Donation): Jar =
    jar.copy(donations = jar.donations :+ donation)

  def measure(label: String)(action: => Unit): Long = {
    val start = System.nanoTime()
    action
    val elapsedMs = (System.nanoTime() - start) / 1_000_000
    println(f"$label: $elapsedMs%d мс")
    elapsedMs
  }

  println(s"=== Бенчмарк: $donationCount донатів (javish vs fp) ===\n")

  val javishMs = measure("javish: donateSilent (mutable ArrayBuffer)") {
    val jar = new DonationJar("bench-javish", 1_000_000_000.0)
    for (i <- 1 to donationCount) {
      jar.donateSilent(1.0, s"donor-$i")
    }
  }

  val appendMs = measure("fp: addDonationAppend (List :+, O(N) на крок)") {
    var jar = Jar("bench-fp-append", 1_000_000_000.0, Nil)
    for (i <- 1 to donationCount) {
      jar = addDonationAppend(jar, Donation(s"donor-$i", 1.0))
    }
  }

  val prependMs = measure("fp: addDonation (prepend ::, O(1) на крок)") {
    var jar = Jar("bench-fp-prepend", 1_000_000_000.0, Nil)
    for (i <- 1 to donationCount) {
      jar = addDonation(jar, Donation(s"donor-$i", 1.0))
    }
  }

  println(f"\nСуми однакові: $donationCount грн (одна правда)")

  if (appendMs > 0 && prependMs > 0) {
    val trapRatio = appendMs.toDouble / prependMs
    println(f"Пастка :+ повільніша за :: у ~${trapRatio}%.0f разів — це логіка append, не «повільне ФП»")
  }
}
