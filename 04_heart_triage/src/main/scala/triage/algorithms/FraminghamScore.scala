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
 * Diagnostic Logic:
 * Calculates a point-based 10-year coronary heart disease (CHD) risk:
 * - Age score (tiered by gender).
 * - Total Cholesterol score (tiered by age & gender).
 * - Systolic Blood Pressure score.
 * - Diabetes / Fasting Blood Sugar marker (+2 points).
 *
 * Risk Thresholds:
 * - High Risk: Score >= 11 points (>20% 10-year risk).
 * - Moderate Risk: Score 6..10 points (10-20% 10-year risk).
 * - Low Risk: Score < 6 points (<10% 10-year risk).
 */
object FraminghamScore:
  def evaluate(p: PatientRecord): Diagnosis =
    val isMale = p.sex >= 1.0

    // 1. Age Points
    val agePoints = if isMale then
      if p.age < 35 then -9
      else if p.age < 40 then -4
      else if p.age < 45 then 0
      else if p.age < 50 then 3
      else if p.age < 55 then 6
      else if p.age < 60 then 8
      else if p.age < 65 then 10
      else if p.age < 70 then 11
      else if p.age < 75 then 12
      else 13
    else
      if p.age < 35 then -7
      else if p.age < 40 then -3
      else if p.age < 45 then 0
      else if p.age < 50 then 3
      else if p.age < 55 then 6
      else if p.age < 60 then 8
      else if p.age < 65 then 10
      else if p.age < 70 then 12
      else if p.age < 75 then 14
      else 16

    // 2. Cholesterol Points (ATP III age-stratified)
    val cholPoints = if isMale then
      if p.chol < 160.0 then 0
      else if p.chol < 200.0 then (if p.age < 50 then 4 else 2)
      else if p.chol < 240.0 then (if p.age < 50 then 7 else 5)
      else if p.chol < 280.0 then (if p.age < 50 then 9 else 6)
      else (if p.age < 50 then 11 else 8)
    else
      if p.chol < 160.0 then 0
      else if p.chol < 200.0 then (if p.age < 50 then 4 else 2)
      else if p.chol < 240.0 then (if p.age < 50 then 8 else 5)
      else if p.chol < 280.0 then (if p.age < 50 then 11 else 7)
      else (if p.age < 50 then 13 else 8)

    // 3. Systolic Blood Pressure Points (trestbps)
    val bpPoints =
      if p.trestbps < 120.0 then 0
      else if p.trestbps < 130.0 then 0
      else if p.trestbps < 140.0 then 1
      else if p.trestbps < 160.0 then 1
      else 2

    // 4. Diabetes Comorbidity Points
    val diabetesPoints = if p.fbs == 1.0 then 2 else 0

    val totalPoints = agePoints + cholPoints + bpPoints + diabetesPoints

    val risk = if totalPoints >= 11 then
      RiskLevel.High
    else if totalPoints >= 6 then
      RiskLevel.Moderate
    else
      RiskLevel.Low

    val reasoning = f"Framingham ATP III score: $totalPoints pts (Age: $agePoints, Chol: $cholPoints, BP: $bpPoints, Diab: $diabetesPoints)"

    Diagnosis(
      evaluatorName = "Framingham Risk Score",
      risk = risk,
      score = totalPoints.toDouble,
      reasoning = reasoning
    )
