package pipeline

/**
 * Чистий парсер транзакцій (Railway-Oriented Programming).
 * Жодних 'null', 'throw new Exception' чи брудних 'try/catch'.
 */
object Parser:

  /**
   * Парсить один сирий рядок формату:
   * "id, sender_email, recipient_email, amount, currency, optional_note"
   *
   * @param rowIdx номер рядка (для деталізації помилок)
   * @param line вміст рядка
   * @return Either[TxError, Transaction]
   */
  def parseLine(rowIdx: Int, line: String): Either[TxError, Transaction] =
    val parts = line.split(",").map(_.trim)

    // Перевірка кількості обов'язкових колонок (мінімум 5, 6-та опціональна)
    if parts.length < 5 then
      Left(TxError.MalformedRow(rowIdx, line, expectedCols = 5, actualCols = parts.length))
    else
      val idRaw        = parts(0)
      val senderRaw    = parts(1)
      val recipientRaw = parts(2)
      val amountRaw    = parts(3)
      val currencyRaw  = parts(4)
      val noteOpt      = if parts.length >= 6 && parts(5).nonEmpty then Some(parts(5)) else None

      // Залізничний конвеєр (Railway): зупиняється на першому ж Left!
      for
        sender    <- Email.fromString(senderRaw)
        recipient <- Email.fromString(recipientRaw)
        currency  <- Currency.fromString(currencyRaw)
        money     <- Money.create(amountRaw, currency)
      yield Transaction(
        id = idRaw,
        sender = sender,
        recipient = recipient,
        money = money,
        note = noteOpt
      )

  /**
   * Пакетний парсер списку рядків.
   * Використовує partitionMap для розділення на (помилки, валідні_транзакції)
   */
  def parseBatch(lines: List[String]): (List[TxError], List[Transaction]) =
    lines.zipWithIndex.partitionMap { case (line, idx) =>
      parseLine(idx + 1, line)
    }
