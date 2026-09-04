package javish

import org.slf4j.LoggerFactory

import scala.collection.mutable

object DonationJar {
  // у Java: private static final Logger log = LoggerFactory.getLogger(DonationJar.class);
  private val log = LoggerFactory.getLogger(classOf[DonationJar])
}

final class DonationJar(val jarId: String, val goalAmount: Double) {

  var balance: Double = 0.0
  // у Java: private final List<Donation> donations = new ArrayList<>();
  private val donations = mutable.ArrayBuffer.empty[Donation]

  private def recordDonation(amount: Double, donor: String): Boolean = {
    if (amount <= 0 || donor == null) return false
    balance += amount
    donations += new Donation(donor, amount, System.currentTimeMillis())
    true
  }

  def donate(amount: Double, donor: String): Unit = { // Unit ≈ void у Java
    if (recordDonation(amount, donor)) {
      log.info(s"[$jarId] Донат $amount грн від $donor. Баланс: $balance")
    }
  }

  /** Той самий запис донату, без log.info — для порівняння швидкості. */
  def donateSilent(amount: Double, donor: String): Unit =
    recordDonation(amount, donor)

  def applyPromoBonus(bonus: Double): Unit = {
    if (bonus <= 0) return
    balance += bonus
    log.info(s"[$jarId] Промо-бонус $bonus грн. Баланс: $balance")
  }

  def sumFromDonations(): Double =
    donations.map(_.amount).sum

  def progressPercent(): Double =
    if (goalAmount <= 0) 0.0 else (balance / goalAmount) * 100.0

  def donationsSnapshot: List[Donation] =
    donations.toList
}
