package pipeline

/**
 * Точка входу в демонстраційний додаток.
 * Запуск: sbt "run"
 */
object Main:

  def main(args: Array[String]): Unit =
    println("=" * 75)
    println("🏦 ПРАКТИКА 03: ЗАЛІЗНИЧНИЙ ПЛАТІЖНИЙ КОНВЕЄР (TRANSACTION PIPELINE)")
    println("=" * 75)

    // «Брудний» вхідний потік даних від зовнішніх клієнтів (CSV):
    val rawCsvData = List(
      // 1. Ідеальна транзакція зі звичайною сумою та коментарем
      "tx-001, alice@onu.edu.ua, bob@onu.edu.ua, 450.00, UAH, дякую за каву",
      // 2. VIP транзакція (має отримати 2% кешбеку)
      "tx-002, charlie@onu.edu.ua, david@onu.edu.ua, 5000.00, UAH, оплата за сервер",
      // 3. ❌ Помилка: від'ємна сума
      "tx-003, evil@hacker.com, victim@onu.edu.ua, -100.00, UAH, злам балансу",
      // 4. ❌ Помилка: битий email відправника
      "tx-004, broken-email, bob@onu.edu.ua, 250.00, UAH, привіт",
      // 5. ❌ Помилка: невідома валюта
      "tx-005, user@onu.edu.ua, shop@onu.edu.ua, 100.00, BITCOIN, крипта",
      // 6. Валідна транзакція без коментаря (перевірка Option[String])
      "tx-006, olena@onu.edu.ua, petro@onu.edu.ua, 1200.00, EUR",
      // 7. ❌ Помилка: неповний рядок (бракує колонок)
      "tx-007, incomplete-row-data",
      // 8. Транзакція з підозріло гігантською сумою (для перевірки правила антифроду)
      "tx-008, oligarch@corp.com, offshore@bank.com, 250000.00, USD, терміновий переказ"
    )

    println(s"\n📥 Отримано ${rawCsvData.size} вхідних записів для пакетної обробки...\n")

    // КРОК 1: Залізничний парсинг через partitionMap
    val (parseErrors, parsedTransactions) = Parser.parseBatch(rawCsvData)

    println(f"🔍 РЕЗУЛЬТАТИ ВХІДНОГО ПАРСИНГУ:")
    println(f"   Успішно розпізнано: ${parsedTransactions.size} транзакцій")
    println(f"   Відхилено парсером:  ${parseErrors.size} записів")

    println("\n❌ ВІДХИЛЕНІ ЗАПИСИ (Рейка Left):")
    parseErrors.foreach(err => println(s"   🔴 ${err.formatMessage}"))

    // КРОК 2: Застосування конвеєра бізнес-правил (First-Class Functions)
    // Трансформація: комісія + кешбек + нормалізація нотаток
    val processedTransactions = parsedTransactions.map(Rules.standardPipeline)

    // КРОК 3: Бізнес-перевірка антифроду (ліміт 100 000)
    val fraudRule = Rules.fraudLimitRule(maxLimit = BigDecimal(100_000.0))
    val (fraudErrors, approvedTransactions) =
      processedTransactions.partitionMap(fraudRule)

    if fraudErrors.nonEmpty then
      println("\n🚨 ВІДХИЛЕНО АНТИФРОД-СИСТЕМОЮ:")
      fraudErrors.foreach(err => println(s"   ⛔ ${err.formatMessage}"))

    // КРОК 4: Фінальний реєстр успішних проводок
    println("\n" + "=" * 75)
    println("✅ ПРОВЕДЕНІ ТРАНЗАКЦІЇ (Рейка Right):")
    println("=" * 75)
    approvedTransactions.foreach { tx =>
      val noteStr = tx.note.map(n => s"'$n'").getOrElse("(без примітки)")
      println(f"   💳 ID: ${tx.id}%-7s | Від: ${tx.sender}%-22s | Сума: ${tx.money}%-14s")
      println(f"      Комісія: ${tx.fee}%.2f | Кешбек: ${tx.cashback}%.2f | До зарахування: ${tx.netAmount}%.2f | Примітка: $noteStr")
      println("   " + "-" * 71)
    }

    // КРОК 5: Чистий функціональний підрахунок підсумків через foldLeft
    val totalVolume = approvedTransactions.foldLeft(BigDecimal(0.0))((acc, tx) => acc + tx.money.amount)
    val totalFees   = approvedTransactions.foldLeft(BigDecimal(0.0))((acc, tx) => acc + tx.fee)
    val totalCashback = approvedTransactions.foldLeft(BigDecimal(0.0))((acc, tx) => acc + tx.cashback)

    println(f"\n📊 ФІНАНСОВИЙ ПІДСУМОК ПАКЕТУ (обчислено через foldLeft):")
    println(f"   Загальний оборот: $totalVolume%.2f")
    println(f"   Дохід шлюзу (комісії): +$totalFees%.2f")
    println(f"   Виплачено кешбеку:    -$totalCashback%.2f")
    println()

    // КРОК 6: Запуск інженерного бенчмарку продуктивності
    Benchmark.runComparison(iterations = 100_000)
