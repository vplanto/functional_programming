package triage.algorithms

import triage.{Diagnosis, PatientRecord, RiskLevel}

/**
 * Algorithm 1: AHA / ACC Clinical Practice Guidelines.
 *
 * Official Reference:
 * 2017 ACC/AHA Guideline for the Prevention, Detection, Evaluation, and
 * Management of High Blood Pressure in Adults.
 * https://www.ahajournals.org/doi/10.1161/circulationaha.117.032582
 *
 * Functional Architecture ("Rules as Data"):
 * Instead of imperative mutations (var, ListBuffer), rules are modeled
 * as an immutable List[Option[String]] using Option.when and flattened.
 */
object AhaProtocol:

  def evaluate(p: PatientRecord): Diagnosis =
    // Кожне правило повертає Option[String]. Якщо умови не дотримано -> None
    val rules: List[Option[String]] = List(
      Option.when(p.trestbps >= 140.0)(f"Hypertension Stage 2 (${p.trestbps}%.0f mmHg >= 140)"),
      Option.when(p.chol >= 240.0)(f"Hypercholesterolemia (${p.chol}%.0f mg/dL >= 240)"),
      Option.when(p.cp == 4.0)("Asymptomatic ischemia (Type 4 Chest Pain)"),
      p.ca.filter(_ > 0.0).map(vessels => f"Coronary fluoroscopy calcification ($vessels%.0f vessels)"),
      Option.when(p.fbs == 1.0 && p.trestbps >= 130.0)("Diabetic cardiovascular risk (FBS > 120 + Elevated BP)")
    )

    // Видаляємо всі None, залишаючи тільки активовані критичні зауваження
    val activeReasons: List[String] = rules.flatten
    val criticalFlags = activeReasons.size

    val risk = if criticalFlags >= 2 then
      RiskLevel.High
    else if criticalFlags == 1 then
      RiskLevel.Moderate
    else
      RiskLevel.Low

    Diagnosis(
      evaluatorName = "AHA/ACC Protocol",
      risk = risk,
      score = criticalFlags.toDouble,
      reasoning = if activeReasons.isEmpty then "Vitals within normal limits" else activeReasons.mkString("; ")
    )
