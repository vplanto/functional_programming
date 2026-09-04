import javish.DonationJar
import org.scalatest.funsuite.AnyFunSuite

class JavishPainSpec extends AnyFunSuite {

  test("balance повинен дорівнювати сумі донатів у журналі") {
    val jar = new DonationJar("invariant-test", 10_000.0)

    jar.donate(100.0, "Alice")
    jar.donate(200.0, "Bob")
    jar.applyPromoBonus(50.0)

    assert(
      jar.balance == jar.sumFromDonations(),
      s"Дві правди: balance=${jar.balance}, сума донатів=${jar.sumFromDonations()}"
    )
  }
}
