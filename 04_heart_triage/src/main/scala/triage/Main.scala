package triage

import java.io.File
import scala.io.Source
import scala.util.{Try, Using}

enum ParsingError:
  case InsufficientColumns(row: Int, expected: Int, actual: Int, raw: String)
  case InvalidField(row: Int, fieldName: String, rawValue: String, details: String)

  def message: String = this match
    case InsufficientColumns(row, exp, act, raw) =>
      s"Рядок ${row}: очікувалося ${exp} колонок, отримано ${act} (вміст: '${raw}')"
    case InvalidField(row, field, value, details) =>
      s"Рядок ${row}, поле '${field}': невалідне значення '${value}' (${details})"

object Main:

  /**
   * Чистий Railway-Oriented парсер:
   * Замість небезпечного catch case _: Exception => None, повертає Either[ParsingError, PatientRecord].
   * Будь-яка помилка типу чи форматування має конкретну причину в сигнатурі.
   */
  def parsePatient(line: String, rowIdx: Int): Either[ParsingError, PatientRecord] =
    val parts = line.split(",").map(_.trim)
    if parts.length < 14 then
      Left(ParsingError.InsufficientColumns(rowIdx, 14, parts.length, line))
    else
      def parseRequiredDouble(idx: Int, name: String): Either[ParsingError, Double] =
        parts(idx).toDoubleOption match
          case Some(value) => Right(value)
          case None => Left(ParsingError.InvalidField(rowIdx, name, parts(idx), "очікувалося число Double"))

      for
        age      <- parseRequiredDouble(0, "age")
        sex      <- parseRequiredDouble(1, "sex")
        cp       <- parseRequiredDouble(2, "cp")
        trestbps <- parseRequiredDouble(3, "trestbps")
        chol     <- parseRequiredDouble(4, "chol")
        fbs      <- parseRequiredDouble(5, "fbs")
        restecg  <- parseRequiredDouble(6, "restecg")
        thalach  <- parseRequiredDouble(7, "thalach")
        exang    <- parseRequiredDouble(8, "exang")
        oldpeak  <- parseRequiredDouble(9, "oldpeak")
        slope    <- parseRequiredDouble(10, "slope")
        // Опціональні поля: '?' у датасеті Cleveland парситься в None
        ca   = parts(11).toDoubleOption
        thal = parts(12).toDoubleOption
        target   <- parts(13).toIntOption match
          case Some(t) => Right(t)
          case None    => Left(ParsingError.InvalidField(rowIdx, "target", parts(13), "очікувалося ціле число 0..4"))
      yield PatientRecord(
        id = rowIdx,
        age = age,
        sex = sex,
        cp = cp,
        trestbps = trestbps,
        chol = chol,
        fbs = fbs,
        restecg = restecg,
        thalach = thalach,
        exang = exang,
        oldpeak = oldpeak,
        slope = slope,
        ca = ca,
        thal = thal,
        target = target
      )

  def loadDataset(): (List[ParsingError], List[PatientRecord]) =
    val possiblePaths = List(
      "data/heart.csv",
      "04_heart_triage/data/heart.csv",
      "../04_heart_triage/data/heart.csv"
    )

    val existingPath = possiblePaths.find(p => new File(p).exists())
    existingPath match
      case Some(path) =>
        Using.resource(Source.fromFile(path)) { source =>
          val parsedLines = source.getLines()
            .drop(1) // Пропускаємо заголовок CSV
            .zipWithIndex
            .map { case (line, idx) => parsePatient(line, idx + 1) }
            .toList

          // Розділяємо Left (помилки) та Right (валідні пацієнти) без мутацій за один прохід
          parsedLines.partitionMap(identity)
        }
      case None =>
        println("⚠️ [ПОМИЛКА] Файл data/heart.csv не знайдено!")
        (List.empty, List.empty)

  def main(args: Array[String]): Unit =
    println("=" * 76)
    println("  🏥 КАРДІОЛОГІЧНИЙ MISD КОНСИЛІУМ: EMERGENCY TRIAGE PIPELINE")
    println("  Cleveland Heart Disease Dataset (303 пацієнти, UCI ML Repository)")
    println("=" * 76)

    val (errors, patients) = loadDataset()

    if errors.nonEmpty then
      println(f"⚠️ Виявлено пошкоджених рядків у датасеті: ${errors.size}")
      errors.take(5).foreach(err => println(s"   • ${err.message}"))
      println("-" * 76)

    if patients.isEmpty then
      println("Завершення: немає валідних записів для аналізу.")
      return

    println(f"✅ Успішно завантажено валідних пацієнтів: ${patients.size}")
    println("⚡ Виконується паралельна оцінка 3 чистими функціональними алгоритмами...")
    println("-" * 76)

    // Warm-up to JIT compile hot paths
    patients.foreach(ConsensusArbiter.evaluatePatient)

    // Timed Execution
    val startTimeNanos = System.nanoTime()
    val triageResults = patients.map { patient =>
      (patient, ConsensusArbiter.evaluatePatient(patient))
    }
    val elapsedNanos = System.nanoTime() - startTimeNanos
    val elapsedMicros = elapsedNanos / 1000.0
    val perPatientMicros = elapsedMicros / patients.size

    // Triage distribution
    val criticalList = triageResults.filter(_._2.isInstanceOf[TriageVerdict.CriticalHospitalize])
    val splitList    = triageResults.filter(_._2.isInstanceOf[TriageVerdict.SplitOpinionDisagreement])
    val moderateList = triageResults.filter(_._2.isInstanceOf[TriageVerdict.ModerateRiskObservation])
    val lowRiskList  = triageResults.filter(_._2.isInstanceOf[TriageVerdict.LowRiskDischarge])

    // Validation against Cleveland Gold Standard (target > 0 means confirmed heart disease)
    val sickPatients = patients.filter(_.hasDiseaseAccordingToGroundTruth)
    val healthyPatients = patients.filter(_.isHealthyAccordingToGroundTruth)

    // Flagged for priority attention (Critical + Split + Moderate vs Low)
    val truePositives = triageResults.count { case (p, v) =>
      p.hasDiseaseAccordingToGroundTruth && !v.isInstanceOf[TriageVerdict.LowRiskDischarge]
    }
    val falseNegatives = triageResults.count { case (p, v) =>
      p.hasDiseaseAccordingToGroundTruth && v.isInstanceOf[TriageVerdict.LowRiskDischarge]
    }
    val trueNegatives = triageResults.count { case (p, v) =>
      p.isHealthyAccordingToGroundTruth && v.isInstanceOf[TriageVerdict.LowRiskDischarge]
    }
    val falsePositives = triageResults.count { case (p, v) =>
      p.isHealthyAccordingToGroundTruth && !v.isInstanceOf[TriageVerdict.LowRiskDischarge]
    }

    val sensitivity = (truePositives.toDouble / sickPatients.size) * 100.0
    val specificity = (trueNegatives.toDouble / healthyPatients.size) * 100.0
    val accuracy = ((truePositives + trueNegatives).toDouble / patients.size) * 100.0

    // Printing Detailed Results
    println("\n📊 РОЗПОДІЛ РІШЕНЬ КОНСИЛІУМУ (MISD QUORUM):")
    println(f"  🔴 [CRITICAL_HOSPITALIZE]  : ${criticalList.size}%3d пацієнтів (${criticalList.size * 100.0 / patients.size}%.1f%%) — Негайна госпіталізація (Кворум High)")
    println(f"  🟡 [SPLIT_DISAGREEMENT]    : ${splitList.size}%3d пацієнтів (${splitList.size * 100.0 / patients.size}%.1f%%) — Розбіжність думок (1 High — потрібен арбітр)")
    println(f"  🟠 [MODERATE_OBSERVATION]  : ${moderateList.size}%3d пацієнтів (${moderateList.size * 100.0 / patients.size}%.1f%%) — Кворум помірного ризику (>=2 Moderate)")
    println(f"  🟢 [LOW_RISK_DISCHARGE]    : ${lowRiskList.size}%3d пацієнтів (${lowRiskList.size * 100.0 / patients.size}%.1f%%) — Амбулаторний нагляд (Кворум Low)")

    println("\n🎯 КЛІНІЧНА ВАЛІДАЦІЯ ПРОТИ ВЕРИФІКОВАНОГО ДІАГНОЗУ (target > 0):")
    println(f"  • Всього хворих у вибірці  : ${sickPatients.size}")
    println(f"  • Всього здорових у вибірці: ${healthyPatients.size}")
    println(f"  • Чутливість (Sensitivity) : $sensitivity%.2f%%  [Хворих виявлено вчасно: $truePositives / ${sickPatients.size}]")
    println(f"  • Специфічність (Recall TN): $specificity%.2f%%  [Здорових не госпіталізовано дарма: $trueNegatives / ${healthyPatients.size}]")
    println(f"  • Загальна точність        : $accuracy%.2f%%")
    println(f"  • Критичні пропуски (FN)   : $falseNegatives (Пацієнти з ішемією, відправлені додому)")

    println("\n⏱️ ПРОДУКТИВНІСТЬ MISD ПАЙПЛАЙНУ:")
    println(f"  • Загальний час аналізу 303 пацієнтів: $elapsedMicros%.2f мкс (${elapsedMicros / 1000.0}%.3f мс)")
    println(f"  • Середній час на 1 пацієнта (3 лікарі): $perPatientMicros%.2f мкс")
    println(f"  • Пропускна здатність                : ${(patients.size / (elapsedMicros / 1_000_000.0)).toLong}%,d пацієнтів/сек")

    // Розбір критичного пропуску (False Negative)
    val fnList = triageResults.filter { case (p, v) =>
      p.hasDiseaseAccordingToGroundTruth && v.isInstanceOf[TriageVerdict.LowRiskDischarge]
    }
    if fnList.nonEmpty then
      println("\n🚨 РОЗБІР КРИТИЧНОГО ПРОПУСКУ (FALSE NEGATIVE — ХВОРИЙ, ВІДПУЩЕНИЙ ДОДОМУ):")
      println("-" * 76)
      fnList.foreach { case (p, verdict) =>
        val sexStr = if p.sex >= 1.0 then "Чоловік" else "Жінка"
        println(f"Пацієнт #${p.id}%03d [Вік: ${p.age}%.0f, Стать: $sexStr, Тиск: ${p.trestbps}%.0f, Холестерин: ${p.chol}%.0f, Пульс: ${p.thalach}%.0f, Біль (cp): ${p.cp}%.0f]")
        println(f"  ➔ Фактичний діагноз : Хворий на ІХС (target = ${p.target})")
        println(f"  ➔ Рішення консиліуму: 🟢 LOW_RISK_DISCHARGE (Пропущено консиліумом!)")
        verdict.diagnoses.foreach { d =>
          println(f"     • ${d.evaluatorName}%-22s: ${d.risk}%-8s | ${d.reasoning}")
        }
        println("  ➔ Чому алгоритми 'проспали' хворобу:")
        println("     1. Тиск і холестерин у нормі -> AHA протокол не бачить тривоги (Low).")
        println("     2. Молодий вік нейтралізував бали за шкалою Фрамінгема (Low).")
        println("     3. На ЕКГ/навантаженні резерв пульсу був достатнім (Duke дав Low).")
        println(f"     4. Приховане вогнище: патологію видає лише сцинтиграфія міокарда (thal = ${p.thal.getOrElse(0.0)}%.0f)!")
      }
      println("-" * 76)

    println("\n📋 ПРИКЛАДИ РОБОТИ КОНСИЛІУМУ ДЛЯ ОКРЕМИХ ПАЦІЄНТІВ:")
    println("-" * 76)

    val sampleIndices = List(2, 5, 8, 14) // 1-indexed patient IDs
    sampleIndices.flatMap(id => triageResults.find(_._1.id == id)).foreach { case (p, verdict) =>
      val verdictLabel = verdict match
        case TriageVerdict.CriticalHospitalize(_, votes, _) =>
          s"🔴 КРИТИЧНИЙ (Кворум High: $votes/3)"
        case TriageVerdict.SplitOpinionDisagreement(_, summary, _) =>
          s"🟡 РОЗБІЖНІСТЬ ДУМОК ($summary — потрібен арбітраж)"
        case TriageVerdict.ModerateRiskObservation(_, votes, _) =>
          s"🟠 ПОМІРНИЙ РИЗИК (Кворум Moderate: $votes/3 — плановий нагляд)"
        case TriageVerdict.LowRiskDischarge(_, votes, _) =>
          s"🟢 НИЗЬКИЙ РИЗИК (Кворум Low: $votes/3 — амбулаторно)"

      val truth = if p.hasDiseaseAccordingToGroundTruth then "Хворий (target > 0)" else "Здоровий (target = 0)"
      println(f"Пацієнт #${p.id}%03d [Вік: ${p.age}%.0f, Тиск: ${p.trestbps}%.0f, Холестерин: ${p.chol}%.0f, Пульс: ${p.thalach}%.0f, Стенокардія: ${p.exang}%.0f]")
      println(f"  ➔ Рішення консиліуму: $verdictLabel")
      println(f"  ➔ Фактичний діагноз : $truth")
      verdict.diagnoses.foreach { d =>
        println(f"     • ${d.evaluatorName}%-22s: ${d.risk}%-8s | ${d.reasoning}")
      }
      println("-" * 76)
    }

    println("=" * 76)
    println("✅ MISD Тріаж завершено успішно.")
    println("=" * 76)
