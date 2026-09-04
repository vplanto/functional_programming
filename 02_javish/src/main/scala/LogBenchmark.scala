import javish.DonationJar

/** Порівняння швидкості donate з log.info і без. Запуск: sbt "runMain LogBenchmark" */
object LogBenchmark extends App {

  val donationCount = 100_000

  def measure(label: String)(action: => Unit): Long = {
    val start = System.nanoTime()
    action
    val elapsedMs = (System.nanoTime() - start) / 1_000_000
    println(f"$label: $elapsedMs%d мс")
    elapsedMs
  }

  println(s"=== Бенчмарк: $donationCount донатів ===\n")

  val jarWithLog = new DonationJar("bench-log", 1_000_000_000.0)
  val jarSilent = new DonationJar("bench-silent", 1_000_000_000.0)

  // Прогрів JVM
  jarWithLog.donateSilent(1.0, "warmup")
  jarSilent.donateSilent(1.0, "warmup")

  val withLogMs = measure("donate + log.info") {
    for (i <- 1 to donationCount) {
      jarWithLog.donate(1.0, s"donor-$i")
    }
  }

  val silentMs = measure("donate без логу (donateSilent)") {
    for (i <- 1 to donationCount) {
      jarSilent.donateSilent(1.0, s"donor-$i")
    }
  }

  if (silentMs > 0) {
    val ratio = withLogMs.toDouble / silentMs
    println(f"\nЛогування у donate повільніше у ~${ratio}%.1f разів")
  }
}
