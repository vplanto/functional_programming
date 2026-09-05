package triage.algorithms

import triage.{Diagnosis, PatientRecord, RiskLevel}

/**
 * Algorithm 3: Duke Treadmill Score (DTS) & Hemodynamic Stress Reserve.
 *
 * Official Reference:
 * Mark, D. B., Shaw, L., Harrell, F. E., et al. (1991).
 * "Prognostic value of a treadmill exercise score in outpatients with suspected coronary artery disease".
 * Circulation, 83(3), 902–909. PubMed ID: 1875969.
 * https://pubmed.ncbi.nlm.nih.gov/1875969/
 *
 * Diagnostic Logic:
 * In cardiology stress testing, coronary insufficiency manifests under exertion:
 * 1. Pulse Reserve: Achieved heart rate vs age-predicted maximum:
 *      Reserve = thalach / (220.0 - age)
 *    Normal >= 0.85 (85%). Inadequate chronotropic response indicates ischemia or dysfunction.
 * 2. ST-segment Depression: oldpeak (in mm).
 * 3. ST-segment Slope: slope == 2.0 (flat) or 3.0 (downsloping) indicates severe myocardial ischemia.
 * 4. Exercise-induced Angina: exang == 1.0.
 *
 * Stress Index Calculation:
 *   Index = (Reserve * 10.0) - (5.0 * oldpeak) - (4.0 * exang) - (if slope >= 2.0 then 2.0 else 0.0)
 *
 * Thresholds:
 * - High Risk: Index < 2.0 OR (oldpeak >= 2.0 && exang == 1.0)
 * - Moderate Risk: 2.0 <= Index < 6.0
 * - Low Risk: Index >= 6.0
 */
object DukeScore:
  def evaluate(p: PatientRecord): Diagnosis =
    val maxPredHr = math.max(120.0, 220.0 - p.age)
    val pulseReserve = p.thalach / maxPredHr

    val slopePenalty = if p.slope >= 2.0 then 2.0 else 0.0
    val anginaPenalty = if p.exang >= 1.0 then 4.0 else 0.0
    val stDepressionPenalty = 5.0 * p.oldpeak

    val stressIndex = (pulseReserve * 10.0) - stDepressionPenalty - anginaPenalty - slopePenalty

    val isSevereOldpeakAndAngina = p.oldpeak >= 2.0 && p.exang >= 1.0
    val isExtremeDepression = p.oldpeak >= 3.0

    val risk = if stressIndex < 2.0 || isSevereOldpeakAndAngina || isExtremeDepression then
      RiskLevel.High
    else if stressIndex < 6.0 then
      RiskLevel.Moderate
    else
      RiskLevel.Low

    val reasoning = f"Stress Index: $stressIndex%.2f (Reserve: ${pulseReserve * 100}%.0f%%, ST-dep: ${p.oldpeak}%.1fmm, Angina: ${p.exang}%.0f, Slope: ${p.slope}%.0f)"

    Diagnosis(
      evaluatorName = "Duke Treadmill Score",
      risk = risk,
      score = stressIndex,
      reasoning = reasoning
    )
