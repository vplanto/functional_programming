import javish.DonationJar

/**
 * Демонстрація розбіжності balance і суми донатів.
 * Запуск: sbt run
 */
object JavishDemo extends App {

  println("=== Банка для збору (java-ish Scala) ===\n")

  val jar = new DonationJar("zsu-help", 10_000.0)

  jar.donate(500.0, "Олена")
  jar.donate(1_200.0, "Андрій")
  jar.donate(300.0, "Марія")
  jar.applyPromoBonus(250.0)

  println(f"Баланс на екрані (var balance):     ${jar.balance}%.2f грн")
  println(f"Сума з журналу донатів:             ${jar.sumFromDonations()}%.2f грн")
  println(f"Прогрес до цілі (за balance):       ${jar.progressPercent()}%.1f%%")

  if (jar.balance != jar.sumFromDonations()) {
    println("\n⚠️  Дві правди в одній банці. Хто головний — balance чи список донатів?")
  }
}
