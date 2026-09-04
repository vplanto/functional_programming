package fp

// object — синглтон: один екземпляр на JVM.
// у Java наближено: final class JarLogicUtils { private JarLogicUtils() {} public static ... }
// Тут без стану — не Spring-сервіс, а «модуль» для чистих функцій.
object JarLogic {

  def addDonation(jar: Jar, donation: Donation): Jar =
    // copy — нова копія; :: (cons) — prepend у голову за O(1); старий список перевикористовується як хвіст.
    jar.copy(donations = donation :: jar.donations)

  def total(jar: Jar): Double =
    // map + sum — аналог stream().map(Donation::getAmount).sum() у Java.
    jar.donations.map(_.amount).sum

  def progressPercent(jar: Jar): Double =
    if (jar.goal <= 0) 0.0 else (total(jar) / jar.goal) * 100.0

  def applyPromoBonus(jar: Jar, bonus: Double): Jar =
    // у частині 1 promo лише змінював balance; тут — ще один факт у журналі донатів.
    if (bonus <= 0) jar
    else addDonation(jar, Donation("промо від банку", bonus))

  def topDonors(jar: Jar, n: Int): List[(String, Double)] =
    jar.donations
      .groupBy(_.donor)              // Map[String, List[Donation]] — донори з їхніми донатами
      .view
      .mapValues(_.map(_.amount).sum) // Map[String, Double] — ім'я → сумарний внесок
      .toList                         // List[(String, Double)] — пари (донор, сума)
      // sortBy(-_._2) — сортування за другим елементом пари (сумою) за спаданням;
      // -_._2 — мінус перетворює зростання на спадання (більші суми спочатку).
      .sortBy(-_._2)
      .take(n)
}
