package pipeline

/**
 * Інженерний мікро-бенчмарк: Ціна Exception проти Either.
 *
 * Демонструє, чому "Exceptions for flow control" — це катастрофа для High Load:
 * Метод fillInStackTrace() у JVM змушений сканувати весь стек викликів потоку,
 * що коштує у 50-100 разів дорожче, ніж створення звичайного функціонального об'єкта Left!
 */
object Benchmark:

  // 1. Імперативний підхід: кидання винятку
  def validateImperative(raw: String): Double =
    val num = raw.toDoubleOption.getOrElse(throw new IllegalArgumentException("Not a number"))
    if num <= 0.0 then throw new IllegalArgumentException("Amount must be > 0")
    num

  // 2. Функціональний підхід: повернення Either
  def validateFunctional(raw: String): Either[String, Double] =
    raw.toDoubleOption match
      case Some(d) if d > 0.0 => Right(d)
      case Some(_)            => Left("Amount must be > 0")
      case None               => Left("Not a number")

  def runComparison(iterations: Int = 100_000): Unit =
    println("=" * 65)
    println(f"⚡ БЕНЧМАРК ПРОДУКТИВНОСТІ: $iterations%,d операцій на невалідних даних")
    println("=" * 65)

    val invalidInputs = List("-50.0", "not_a_number", "0.0", "-999.99")

    // Прогрів JVM (Warmup), щоб JIT скомпілював байткод
    for i <- 0 until 20_000 do
      val sample = invalidInputs(i % invalidInputs.length)
      try validateImperative(sample) catch case _ => ()
      validateFunctional(sample)

    // --- Тест 1: Exceptions ---
    System.gc()
    Thread.sleep(50)
    val startEx = System.nanoTime()
    var exFailures = 0
    for i <- 0 until iterations do
      val sample = invalidInputs(i % invalidInputs.length)
      try
        validateImperative(sample)
      catch
        case _: IllegalArgumentException => exFailures += 1
    val timeExNs = System.nanoTime() - startEx
    val timeExMs = timeExNs / 1_000_000.0
    val nsPerOpEx = timeExNs.toDouble / iterations

    // --- Тест 2: Either ---
    System.gc()
    Thread.sleep(50)
    val startEither = System.nanoTime()
    var eitherFailures = 0
    for i <- 0 until iterations do
      val sample = invalidInputs(i % invalidInputs.length)
      validateFunctional(sample) match
        case Left(_)  => eitherFailures += 1
        case Right(_) => ()
    val timeEitherNs = System.nanoTime() - startEither
    val timeEitherMs = timeEitherNs / 1_000_000.0
    val nsPerOpEither = timeEitherNs.toDouble / iterations

    val speedup = timeExNs.toDouble / timeEitherNs

    println(f"❌ 1. Exceptions (try/catch):  $timeExMs%8.2f мс ($nsPerOpEx%7.1f нс/оп)")
    println(f"✅ 2. Functional (Either):     $timeEitherMs%8.2f мс ($nsPerOpEither%7.1f нс/оп)")
    println("-" * 65)
    println(f"🚀 Результат: Either швидший у $speedup%.1f рази без жодного блокування потоку!")
    println("=" * 65)
