package triage

import triage.algorithms.{AhaProtocol, DukeScore, FraminghamScore}

/**
 * Consensus Arbiter (MISD Medical Council).
 *
 * Implements N-Version Programming / Multiple Instruction Single Data pattern:
 * A single immutable PatientRecord is concurrently analyzed by 3 distinct, independent algorithms.
 *
 * Quorum Rules:
 * - 3/3 or 2/3 High Risk votes -> CriticalHospitalize (Urgent intervention required)
 * - 1 High Risk vote -> SplitOpinionDisagreement (Controversy: urgent human arbiter needed)
 * - 0 High Risk & >= 2 Moderate -> ModerateRiskObservation (Quorum for planned observation)
 * - 0 High Risk & < 2 Moderate -> LowRiskDischarge (Quorum for outpatient care)
 */
object ConsensusArbiter:

  // List of pure functional evaluators
  val evaluators: List[PatientRecord => Diagnosis] = List(
    AhaProtocol.evaluate,
    FraminghamScore.evaluate,
    DukeScore.evaluate
  )

  def evaluatePatient(patient: PatientRecord): TriageVerdict =
    // MISD: The same immutable patient is passed to multiple evaluators
    val diagnoses = evaluators.map(evaluator => evaluator(patient))

    val highVotes     = diagnoses.count(_.risk == RiskLevel.High)
    val moderateVotes = diagnoses.count(_.risk == RiskLevel.Moderate)
    val lowVotes      = diagnoses.count(_.risk == RiskLevel.Low)

    // Ідіоматичний Pattern Matching у стилі чистого ФП замість if/else
    (highVotes, moderateVotes, lowVotes) match
      case (h, _, _) if h >= 2 =>
        TriageVerdict.CriticalHospitalize(patient.id, h, diagnoses)

      case (1, m, l) =>
        // 1 лікар бачить High, інші - ні -> гостра розбіжність
        TriageVerdict.SplitOpinionDisagreement(patient.id, s"1 High, $m Mod, $l Low", diagnoses)

      case (0, m, _) if m >= 2 =>
        // Ніхто не бачить загрози смерті, але більшість радить нагляд
        TriageVerdict.ModerateRiskObservation(patient.id, m, diagnoses)

      case (_, _, l) =>
        // Більшість (2 або 3) вважають пацієнта здоровим
        TriageVerdict.LowRiskDischarge(patient.id, l, diagnoses)
