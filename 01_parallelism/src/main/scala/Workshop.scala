import scala.collection.parallel.CollectionConverters._

object Workshop extends App {
  println("=== Практика 00: Вмикаємо мозок ===")

  // Генеруємо 100 транзакцій для швидких тестів (від 1 до 100)
  val data: Vector[Double] = (1 to 100).toVector.map(_.toDouble)

  // =========================================================================
  // ЗАВДАННЯ 1: ПАСТКА НА RACE CONDITION
  // =========================================================================
  // Студенте! Розкоментуй блок коду нижче і запусти програму (sbt "runMain Workshop") 3 рази підряд.
  // Чому результат кожного разу різний, хоча вхідні дані однакові?
  
/*

  var totalRisk = 0.0 // Зовнішній мутабельний стан (var)

  data.par.foreach { transactionId =>
    // Симуляція якогось обчислення (взято з Main.scala)
    val risk = math.sin(transactionId) * math.cos(transactionId) + math.tan(transactionId % 1.0)

    // Кілька потоків одночасно намагаються перезаписати totalRisk!
    totalRisk += risk
  }

  println(s"Сумарний ризик (через var): $totalRisk")
*/



  // =========================================================================
  // ЗАВДАННЯ 2: РЕФАКТОРИНГ В ЧИСТЕ ФП (EXPRESSION-ORIENTED)
  // =========================================================================
  
  // 1. Оголошуємо Алгебраїчні Типи Даних (ADT) для бізнес-домену
  sealed trait RiskLevel
  case object HighRisk extends RiskLevel
  case object MediumRisk extends RiskLevel
  case object LowRisk extends RiskLevel

  // 2. Ваше завдання: реалізувати чисту функцію класифікації без жодного var чи if-else.
  // Використайте Pattern Matching (match).
  // Умова:
  // - Якщо amount > 80.0 -> HighRisk
  // - Якщо amount > 50.0 -> MediumRisk
  // - Інакше -> LowRisk
  def categorize(amount: Double): RiskLevel = {
    // ТУТ ВАШ КОД
    LowRisk // Заглушка, щоб код компілювався
  }

  // 3. Реалізуйте чисту функцію для отримання коефіцієнта.
  // HighRisk -> 1.5, MediumRisk -> 1.2, LowRisk -> 1.0
  def getMultiplier(level: RiskLevel): Double = {
    // ТУТ ВАШ КОД
    1.0 // Заглушка
  }

  // 4. Побудуйте чистий конвеєр обчислень. 
  // Відфільтруйте транзакції (наприклад, залишіть лише > 50.0), 
  // розрахуйте для них фінальний ризик (transactionId * multiplier) і знайдіть суму.
  
  /* 
  val finalRiskSum = data
    .filter(???)
    .map(???)
    .sum
  */
}
