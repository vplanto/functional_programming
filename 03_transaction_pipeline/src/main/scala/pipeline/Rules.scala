package pipeline

/**
 * Бізнес-правила та функції вищого порядку (First-Class Functions).
 *
 * Тут демонструється:
 * 1. Функція як значення (Function as a First-Class Citizen).
 * 2. Композиція конвеєрів через `andThen`.
 * 3. Динамічне застосування списку функцій через `foldLeft`.
 */
object Rules:

  // Тип для трансформації валідної транзакції
  type Transformer = Transaction => Transaction

  // Тип для бізнес-валідації (може відхилити транзакцію)
  type Validator = Transaction => Either[TxError, Transaction]

  // --- Правило 1: Фіксована банківська комісія (5.00 UAH або еквівалент) ---
  val applyProcessingFee: Transformer = tx =>
    val feeAmount = BigDecimal(5.00)
    tx.copy(fee = feeAmount)

  // --- Правило 2: VIP Кешбек (2% для транзакцій від 1000) ---
  val applyVipCashback: Transformer = tx =>
    if tx.money.amount >= BigDecimal(1000.0) then
      val bonus = (tx.money.amount * BigDecimal(0.02)).setScale(2, BigDecimal.RoundingMode.HALF_UP)
      tx.copy(cashback = bonus)
    else
      tx

  // --- Правило 3: Нормалізація примітки до платежу ---
  val normalizeNote: Transformer = tx =>
    val cleanedNote = tx.note.map(n => n.trim.capitalize)
    tx.copy(note = cleanedNote)

  // --- Статична композиція через andThen ---
  // andThen об'єднує функції: f.andThen(g)(x) == g(f(x))
  val standardPipeline: Transformer =
    applyProcessingFee
      .andThen(applyVipCashback)
      .andThen(normalizeNote)

  // --- Динамічна композиція списку правил через foldLeft ---
  val defaultRules: List[Transformer] = List(
    applyProcessingFee,
    applyVipCashback,
    normalizeNote
  )

  def applyRulesFold(tx: Transaction, rules: List[Transformer]): Transaction =
    rules.foldLeft(tx)((currentTx, rule) => rule(currentTx))

  // --- Валідатор антифрод: ліміт на максимальну одноразову суму ---
  def fraudLimitRule(maxLimit: BigDecimal): Validator = tx =>
    if tx.money.amount > maxLimit then
      Left(TxError.FraudLimitExceeded(tx.money.amount, maxLimit))
    else
      Right(tx)
