package triage.algorithms

import triage.{Diagnosis, PatientRecord, RiskLevel}

/**
 * Algorithm 2: Framingham Risk Score (FRS / Adult Treatment Panel III - ATP III).
 *
 * Official Reference:
 * National Heart, Lung, and Blood Institute (NIH / NHLBI).
 * Third Report of the National Cholesterol Education Program (NCEP) Expert Panel
 * on Detection, Evaluation, and Treatment of High Blood Cholesterol in Adults (Adult Treatment Panel III).
 * https://www.nhlbi.nih.gov/files/docs/resources/heart/atp-3-cholesterol-full-report.pdf
 *
 * Functional Architecture ("Rules as Data" & Pattern Matching):
 * Multi-tiered scoring tables are modeled as declarative collections and evaluated
 * via pure pattern matching, eliminating deeply nested imperative if-else cascades.
 */
object FraminghamScore:

  // 1. Таблиця вікових балів ATP III: (поріг віку, (чоловіки, жінки))
  private val ageTiers: List[(Double, (Int, Int))] = List(
    (35.0, (-9, -7)),
    (40.0, (-4, -3)),
    (45.0, ( 0,  0)),
    (50.0, ( 3,  3)),
    (55.0, ( 6,  6)),
    (60.0, ( 8,  8)),
    (65.0, (10, 10)),
    (70.0, (11, 12)),
    (75.0, (12, 14))
  )
  private val ageDefault = (13, 16) // >= 75 років

  // 2. Таблиця холестеринових балів ATP III: (поріг холестерину, (Young Male, Old Male, Young Female, Old Female))
  private val cholTiers: List[(Double, (Int, Int, Int, Int))] = List(
    (160.0, (0, 0, 0, 0)),
    (200.0, (4, 2, 4, 2)),
    (240.0, (7, 5, 8, 5)),
    (280.0, (9, 6, 11, 7))
  )
  private val cholDefault = (11, 8, 13, 8) // >= 280 мг/дл

  def evaluate(p: PatientRecord): Diagnosis =
    val isMale = p.sex >= 1.0
    val isYoung = p.age < 50.0

    // 1. Вікові бали за табличною структурою
    val (maleAgePts, femaleAgePts) = ageTiers
      .find { case (threshold, _) => p.age < threshold }
      .map(_._2)
      .getOrElse(ageDefault)
    val agePoints = if isMale then maleAgePts else femaleAgePts

    // 2. Холестеринові бали за ATP III стратифікацією (Rules as Data)
    val (ym, om, yf, of) = cholTiers
      .find { case (threshold, _) => p.chol < threshold }
      .map(_._2)
      .getOrElse(cholDefault)

    val cholPoints = (isMale, isYoung) match
      case (true, true)   => ym
      case (true, false)  => om
      case (false, true)  => yf
      case (false, false) => of

    // 3. Систолічний тиск (Pattern Matching)
    val bpPoints = p.trestbps match
      case bp if bp < 130.0 => 0
      case bp if bp < 160.0 => 1
      case _                => 2

    // 4. Цукровий діабет (Rules as Data через Option.when)
    val diabetesPoints = Option.when(p.fbs == 1.0)(2).getOrElse(0)

    val totalPoints = agePoints + cholPoints + bpPoints + diabetesPoints

    // 5. Декларативна стратифікація ризику через Pattern Matching
    val risk = totalPoints match
      case pts if pts >= 11 => RiskLevel.High
      case pts if pts >= 6  => RiskLevel.Moderate
      case _                => RiskLevel.Low

    val reasoning = f"Framingham ATP III score: $totalPoints pts (Age: $agePoints, Chol: $cholPoints, BP: $bpPoints, Diab: $diabetesPoints)"

    Diagnosis(
      evaluatorName = "Framingham Risk Score",
      risk = risk,
      score = totalPoints.toDouble,
      reasoning = reasoning
    )
