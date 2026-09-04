import fp.{Donation, Jar}
import fp.JarLogic._
import org.slf4j.LoggerFactory

/** Imperative Shell: `var` і логування на краю; `JarLogic` — чисте ядро. Запуск: sbt "runMain DonationApp" */
object DonationApp extends App {

  private val log = LoggerFactory.getLogger(getClass)

  // мутабельний «поточний стан» лише тут, на верхньому рівні програми
  var currentJar = Jar("zsu-help", 10_000.0, Nil)

  def handleUserDonation(amount: Double, donor: String): Unit = {
    currentJar = addDonation(currentJar, Donation(donor, amount))
    log.info(
      s"[${currentJar.id}] Донат $amount грн від $donor. Сума з журналу: ${total(currentJar)} грн"
    )
  }

  def handlePromoBonus(bonus: Double): Unit = {
    currentJar = applyPromoBonus(currentJar, bonus)
    log.info(
      s"[${currentJar.id}] Промо $bonus грн. Сума з журналу: ${total(currentJar)} грн"
    )
  }

  handleUserDonation(500.0, "Олена")
  handleUserDonation(1_200.0, "Андрій")
  handleUserDonation(300.0, "Марія")
  handlePromoBonus(250.0)

  log.info(f"Підсумок: ${total(currentJar)}%.0f грн, прогрес ${progressPercent(currentJar)}%.1f%%")
}
