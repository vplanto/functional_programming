import fp.{Donation, Jar}
import fp.JarLogic._

object Workshop extends App {

  println("=== Та сама банка, функціональний стиль ===\n")

  val jar = Jar("zsu-help", 10_000.0, Nil)

  val afterDonations =
    addDonation(
      addDonation(
        addDonation(jar, Donation("Олена", 500.0)),
        Donation("Андрій", 1_200.0)),
      Donation("Марія", 300.0))

  val finalJar = applyPromoBonus(afterDonations, 250.0)

  println(s"Сума: ${total(finalJar)} грн")
  println(s"Прогрес: ${progressPercent(finalJar)}%")
  println(s"Топ донори: ${topDonors(finalJar, 2)}")

  val demo = Jar(
    id = "demo",
    goal = 10_000.0,
    donations = List(
      Donation("Олена", 500.0),
      Donation("Андрій", 1_200.0),
      Donation("Марія", 300.0),
      Donation("Олена", 700.0)
    )
  )

  println(s"\nДемо для тестів — сума: ${total(demo)} грн")
}
