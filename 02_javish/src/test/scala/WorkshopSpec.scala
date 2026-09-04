import fp.{Donation, Jar}
import fp.JarLogic._
import org.scalatest.funsuite.AnyFunSuite

class WorkshopSpec extends AnyFunSuite {

  private val demo = Jar(
    id = "demo",
    goal = 10_000.0,
    donations = List(
      Donation("Олена", 500.0),
      Donation("Андрій", 1_200.0),
      Donation("Марія", 300.0),
      Donation("Олена", 700.0)
    )
  )

  test("total рахує суму з журналу донатів") {
    assert(total(demo) == 2_700.0)
  }

  test("progressPercent рахує прогрес від суми донатів") {
    assert(progressPercent(demo) == 27.0)
  }

  test("addDonation повертає нову банку і не мутує стару") {
    val updated = addDonation(demo, Donation("Іван", 100.0))
    assert(total(demo) == 2_700.0)
    assert(total(updated) == 2_800.0)
  }

  test("topDonors повертає топ донорів за сумою") {
    assert(topDonors(demo, 2) == List(("Олена", 1_200.0), ("Андрій", 1_200.0)))
  }
}
