package triage

/**
 * Immutable Domain Model for Emergency Cardiology Triage.
 *
 * Based on the Cleveland Heart Disease Dataset (UCI Machine Learning Repository):
 * https://archive.ics.uci.edu/dataset/45/heart+disease
 */

enum RiskLevel:
  case Low, Moderate, High

case class PatientRecord(
  id: Int,
  age: Double,
  sex: Double,         // 1.0 = male, 0.0 = female
  cp: Double,          // chest pain: 1 = typical angina, 2 = atypical, 3 = non-anginal, 4 = asymptomatic
  trestbps: Double,    // resting blood pressure in mm Hg
  chol: Double,        // serum cholesterol in mg/dl
  fbs: Double,         // fasting blood sugar > 120 mg/dl: 1.0 = true, 0.0 = false
  restecg: Double,     // resting ECG: 0 = normal, 1 = ST-T wave abnormality, 2 = left ventricular hypertrophy
  thalach: Double,     // maximum heart rate achieved
  exang: Double,       // exercise induced angina: 1.0 = yes, 0.0 = no
  oldpeak: Double,     // ST depression induced by exercise relative to rest
  slope: Double,       // slope of the peak exercise ST segment: 1 = upsloping, 2 = flat, 3 = downsloping
  ca: Option[Double],  // number of major vessels (0-3) colored by flourosopy (missing represented as None)
  thal: Option[Double],// 3.0 = normal, 6.0 = fixed defect, 7.0 = reversable defect
  target: Int          // diagnosis of heart disease: 0 = healthy (<50% stenosis), 1..4 = heart disease
):
  def isHealthyAccordingToGroundTruth: Boolean = target == 0
  def hasDiseaseAccordingToGroundTruth: Boolean = target > 0

case class Diagnosis(
  evaluatorName: String,
  risk: RiskLevel,
  score: Double,
  reasoning: String
)

enum TriageVerdict:
  case CriticalHospitalize(patientId: Int, highVotes: Int, diagnoses: List[Diagnosis])
  case SplitOpinionDisagreement(patientId: Int, summary: String, diagnoses: List[Diagnosis])
  case ModerateRiskObservation(patientId: Int, moderateVotes: Int, diagnoses: List[Diagnosis])
  case LowRiskDischarge(patientId: Int, lowVotes: Int, diagnoses: List[Diagnosis])

  def patientId: Int
  def diagnoses: List[Diagnosis]
  def isCritical: Boolean = this match
    case CriticalHospitalize(_, _, _) => true
    case _ => false
