package pipeline

/**
 * Доменна модель платіжного конвеєра (Type-Driven Design).
 * Усі невалідні стани унеможливлені на рівні типів через Smart Constructors та ADT.
 */

// 1. Повна закрита множина доменних помилок (Sum Type ADT)
enum TxError:
  case MalformedRow(row: Int, raw: String, expectedCols: Int, actualCols: Int)
  case InvalidEmail(raw: String, reason: String)
  case InvalidAmount(raw: String, reason: String)
  case UnsupportedCurrency(raw: String)
  case FraudLimitExceeded(amount: BigDecimal, limit: BigDecimal)

  def formatMessage: String = this match
    case MalformedRow(r, raw, exp, act) =>
      s"[Рядок ${r}] Очікувалося ${exp} колонок, отримано ${act}. Вміст: '${raw}'"
    case InvalidEmail(raw, reason) =>
      s"[Невалідний Email] '${raw}': ${reason}"
    case InvalidAmount(raw, reason) =>
      s"[Невалідна Сума] '${raw}': ${reason}"
    case UnsupportedCurrency(raw) =>
      s"[Невідома Валюта] '${raw}'. Підтримуються лише: UAH, USD, EUR"
    case FraudLimitExceeded(amount, limit) =>
      s"[Антифрод] Сума ${amount} перевищує ліміт одноразового переказу ${limit}"

// 2. Розумний конструктор Email (Smart Constructor)
// Первинний конструктор приватний: створити об'єкт можна лише пройшовши валідацію
final case class Email private (value: String):
  override def toString: String = value

object Email:
  def fromString(raw: String): Either[TxError.InvalidEmail, Email] =
    val trimmed = raw.trim
    if !trimmed.contains("@") then
      Left(TxError.InvalidEmail(raw, "Відсутній символ '@'"))
    else
      val parts = trimmed.split("@")
      if parts.length != 2 || parts(0).isEmpty || parts(1).isEmpty then
        Left(TxError.InvalidEmail(raw, "Email повинен мати непорожні ім'я та домен"))
      else if !parts(1).contains(".") then
        Left(TxError.InvalidEmail(raw, "Доменна частина повинна містити крапку (наприклад: .com, .ua)"))
      else
        Right(new Email(trimmed.toLowerCase))

// 3. Підтримувані валюти (Sum Type)
enum Currency:
  case UAH, USD, EUR

object Currency:
  def fromString(raw: String): Either[TxError.UnsupportedCurrency, Currency] =
    raw.trim.toUpperCase match
      case "UAH" => Right(Currency.UAH)
      case "USD" => Right(Currency.USD)
      case "EUR" => Right(Currency.EUR)
      case other => Left(TxError.UnsupportedCurrency(other))

// 4. Розумний конструктор грошової суми (суворо більше нуля)
final case class Money private (amount: BigDecimal, currency: Currency):
  override def toString: String = f"${amount}%.2f ${currency}"

object Money:
  def create(raw: String, currency: Currency): Either[TxError.InvalidAmount, Money] =
    raw.trim.toDoubleOption match
      case None =>
        Left(TxError.InvalidAmount(raw, "Значення не є числом"))
      case Some(num) if num <= 0.0 =>
        Left(TxError.InvalidAmount(raw, f"Сума повинна бути строго додатною (> 0), отримано ${num}%.2f"))
      case Some(valid) =>
        Right(new Money(BigDecimal(valid), currency))

// 5. Повністю валідована незмінна транзакція (Product Type)
case class Transaction(
  id: String,
  sender: Email,
  recipient: Email,
  money: Money,
  note: Option[String],
  fee: BigDecimal = BigDecimal(0.0),
  cashback: BigDecimal = BigDecimal(0.0)
):
  def netAmount: BigDecimal = money.amount - fee + cashback
