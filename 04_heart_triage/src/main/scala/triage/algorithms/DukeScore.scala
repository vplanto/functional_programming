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
 * Functional Architecture ("Rules as Data" & Pattern Matching):
 * Hemodynamic penalties are modeled as an immutable collection of rules (analogous to AhaProtocol),
 * and risk stratification is performed via pure pattern matching without tangled if-else branches.
 */
object DukeScore:

  def evaluate(p: PatientRecord): Diagnosis =
    val maxPredHr = math.max(120.0, 220.0 - p.age)
    val pulseReserve = p.thalach / maxPredHr

    // 1. Штрафні санкції стрес-тесту за парадигмою "Rules as Data"
    val penaltyRules: List[Option[Double]] = List(
      Option.when(p.slope >= 2.0)(2.0), // Патологічний нахил ST (flat/downsloping)
      Option.when(p.exang >= 1.0)(4.0), // Стенокардія при навантаженні
      Some(5.0 * p.oldpeak)             // Глибина депресії ST
    )
    val totalPenalties = penaltyRules.flatten.sum

    val stressIndex = (pulseReserve * 10.0) - totalPenalties

    // 2. Гострі ішемічні маркери
    val isSevereOldpeakAndAngina = p.oldpeak >= 2.0 && p.exang >= 1.0
    val isExtremeDepression = p.oldpeak >= 3.0
    val hasCriticalMarker = isSevereOldpeakAndAngina || isExtremeDepression

    // 3. Декларативна стратифікація ризику через Pattern Matching
    val risk = (stressIndex, hasCriticalMarker) match
      case (_, true)                     => RiskLevel.High
      case (idx, _) if idx < 2.0         => RiskLevel.High
      case (idx, _) if idx < 6.0         => RiskLevel.Moderate
      case _                             => RiskLevel.Low

    val reasoning = f"Stress Index: $stressIndex%.2f (Reserve: ${pulseReserve * 100}%.0f%%, ST-dep: ${p.oldpeak}%.1fmm, Angina: ${p.exang}%.0f, Slope: ${p.slope}%.0f)"

    Diagnosis(
      evaluatorName = "Duke Treadmill Score",
      risk = risk,
      score = stressIndex,
      reasoning = reasoning
    )
