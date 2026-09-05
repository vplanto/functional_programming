# Практика 03: Залізничний платіжний конвеєр (Railway-Oriented Pipeline & First-Class Functions)

🔗 **Код до цієї практики:** [03_transaction_pipeline](file:///home/pvv/projects/onu/functional_programming/03_transaction_pipeline)  
> **Декларація курсу.** Академічна доброчесність та авторство матеріалів — у [DISCLAIMER.md](DISCLAIMER.md).  
> 💡 **Місток курсу:** У [Практиці 02](p02_javish_donation_jar.md) ми навчилися замінювати мутабельні змінні `var` на `val`. Але реальний світ жорстокий: зовнішні клієнти надсилають «брудні» невалідні дані. У цій практиці ми збудуємо **відмовостійкий платіжний шлюз**, поєднавши залізничне програмування ([Лекція 03](03_railway_oriented_programming.md)), функції як дані ([Лекція 02](02_first_class_functions.md)) та підготуємося до складного консиліуму в [Практиці 04](p04_heart_disease_triage.md).

---

## Експрес-опитування та розминка

<details>
<summary>1. Чому валідація даних через throw new IllegalArgumentException() — це катастрофа для High-Load сервісу?</summary>

Кожного разу, коли JVM створює виняток, вона викликає нативний метод ядра `fillInStackTrace()`. Цей метод заморожує потік і покроково сканує всю глибину стеку викликів, щоб побудувати текстовий стек-трейс. На 100 000 невалідних запитів процесор спалює понад 95% ресурсів не на бізнес-логіку, а на запис імен файлів і номерів рядків! Функціональний `Either` повертає звичайний легковажний об'єкт у купі, працюючи у 50–100 разів швидше.
</details>

<details>
<summary>2. Чому опціональне призначення платежу має бути Option[String], а не String зі значенням null?</summary>

Тому що тип `String` не дає гарантій відсутності `NullPointerException`. Поле `Option[String]` робить наявність або відсутність коментаря **чесним контрактом на рівні системи типів**: компілятор просто не дозволить викликати `.trim` або `.toUpperCase` безпосередньо, змушуючи розробника безпечно обробити обидва варіанти через `.map` або `.getOrElse`.
</details>

<details>
<summary>3. У чому перевага конструювання бізнес-логіки зі списку функцій `List[Transaction => Transaction]` замість одного монолітного методу?</summary>

Принцип відкритості/закритості (Open-Closed Principle): ви можете додавати нові правила (наприклад, святковий кешбек або комісію для юросіб), просто додавши ще одну чисту функцію до списку, не чіпаючи і не ризикуючи зламати вже протестований код попередніх правил.
</details>

---

## 1. Архітектурний кейс: «Нічний краш банку від невідомого символу»

**Контекст:** Фінтех-стартап запустив обробник вхідних виписок у форматі CSV.  
Розробники написали класичний імперативний сервіс на Java/Spring:

```java
// ❌ Типовий імперативний підхід з Exception для валідації:
for (String line : fileLines) {
    try {
        Transaction tx = parseAndValidate(line); // кидає IllegalArgumentException якщо сума <= 0
        processPayment(tx);
    } catch (Exception e) {
        logger.error("Збій рядка", e); // Кожен збій породжує важкий стек-трейс
    }
}
```

### Що пішло не так:
1. Шахраї запустили скрипт, який засипав шлюз 500 000 запитів із від'ємними сумами (`amount = -100`) та битими адресами пошти.
2. Програма не впала з помилкою компілятора, але на кожному запиті JVM ініціалізувала дорогий `IllegalArgumentException`.
3. Виклики `fillInStackTrace()` наглухо забили всі ядра процесора. Час відповіді шлюзу підскочив з 5 мс до 12 секунд.
4. Легітимні платежі звичайних користувачів почали відвалюватися за тайм-аутом.
5. Більше того, один із парсерів містив помилку: для порожнього коментаря він повернув `null`, що через два кроки викликало `NullPointerException` усередині транзакції на $50 000, яка зависла в невизначеному стані.

**Рішення:** Повна відмова від `Exceptions for Control Flow` на користь **Railway-Oriented Programming (Either)** та **Smart Constructors**.

---

## 2. Анатомія проєкту `03_transaction_pipeline`

Проєкт демонструє покроковий перехід від розрізнених методів до цілісного функціонального конвеєра:

```
[Сирий вхідний потік CSV]
         │
         ▼
 ┌────────────────────────────────────────────────────────┐
 │ 1. Railway-Парсер (Parser.scala)                       │
 │    Рядок -> Either[TxError, Transaction]               │
 │    Використовує Smart Constructors: Email, Money       │
 └───────────────────────┬────────────────────────────────┘
                         │
        ┌────────────────┴────────────────┐
        ▼ (Рейка Left)                    ▼ (Рейка Right)
  [Список помилок TxError]        [Валідні транзакції]
  • MalformedRow                          │
  • InvalidEmail                          ▼
  • InvalidAmount          ┌───────────────────────────────────┐
  • UnsupportedCurrency    │ 2. Конвеєр First-Class Functions │
                           │    applyFee.andThen(applyCashback)│
                           └──────────────┬────────────────────┘
                                          │
                                          ▼
                           ┌───────────────────────────────────┐
                           │ 3. Фільтр Антифроду (Validator)   │
                           │    tx -> Either[FraudLimit, tx]   │
                           └──────────────┬────────────────────┘
                                          │
                                          ▼
                           [Фінальний реєстр та foldLeft підсумок]
```

---

## 3. Розбір ключових рішень у коді

### А. Типізовані доменні помилки (ADT у `domain.scala`)
Замість одного загального винятку `RuntimeException("помилка")` ми створюємо замкнений Sum Type:

```scala
enum TxError:
  case MalformedRow(row: Int, raw: String, expectedCols: Int, actualCols: Int)
  case InvalidEmail(raw: String, reason: String)
  case InvalidAmount(raw: String, reason: String)
  case UnsupportedCurrency(raw: String)
  case FraudLimitExceeded(amount: BigDecimal, limit: BigDecimal)
```
Будь-яка помилка має повний контекст, необхідний для аудиту та відповіді клієнту.

---

### Б. Розумні конструктори (Smart Constructors)
Ми захищаємо систему від створення об'єктів із некоректними даними:

```scala
final case class Email private (value: String)

object Email:
  def fromString(raw: String): Either[TxError.InvalidEmail, Email] =
    val trimmed = raw.trim
    if !trimmed.contains("@") then
      Left(TxError.InvalidEmail(raw, "Відсутній символ '@'"))
    else
      // Детальна перевірка домену...
      Right(new Email(trimmed.toLowerCase))
```
Ви **фізично не зможете створити `Email`** із битим рядком — компілятор змушує обробити `Either`.

---

### В. Залізничний парсер у `for`-comprehension (`Parser.scala`)
Кілька незалежних перевірок об'єднуються в один гарний залізничний шлях:

```scala
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
  note = noteOpt // Option[String] замість null!
)
```
Якщо `sender` невалідний, виконання **миттєво завершується з `Left`**, не витрачаючи такти процесора на парсинг решти полів.

---

### Г. Композиція функцій вищого порядку (`Rules.scala`)
Кожне бізнес-правило — це чиста функція типу `Transaction => Transaction`:

```scala
type Transformer = Transaction => Transaction

val applyProcessingFee: Transformer = tx => tx.copy(fee = BigDecimal(5.00))
val applyVipCashback: Transformer   = tx => /* нарахування 2% для суми >= 1000 */
val normalizeNote: Transformer      = tx => tx.copy(note = tx.note.map(_.capitalize))

// Склеювання конвеєра в один виклик через andThen:
val standardPipeline: Transformer =
  applyProcessingFee
    .andThen(applyVipCashback)
    .andThen(normalizeNote)
```

---

### Д. Відмовостійна пакетна обробка через `partitionMap`
Ми обробляємо весь пакет рядків одним викликом, гарантуючи, що жодна валідна транзакція не буде втрачена через збій у сусідньому рядку:

```scala
val (parseErrors, parsedTransactions) = Parser.parseBatch(rawCsvData)
```

---

## 4. Інженерний замір: Exception vs Either (`Benchmark.scala`)

У модулі реалізовано мікро-бенчмарк на 100 000 ітерацій.  
Він наочно демонструє ціну створення стек-трейсів у JVM:

```
=================================================================
⚡ БЕНЧМАРК ПРОДУКТИВНОСТІ: 100,000 операцій на невалідних даних
=================================================================
❌ 1. Exceptions (try/catch):     98.40 мс (  984.0 нс/оп)
✅ 2. Functional (Either):          1.45 мс (   14.5 нс/оп)
-----------------------------------------------------------------
🚀 Результат: Either швидший у 67.8 рази без жодного блокування потоку!
=================================================================
```

---

## 5. Як запустити проєкт власноруч

Усі команди виконуються в терміналі з кореня репозиторію:

```bash
# 1. Переходимо до модуля практики:
cd 03_transaction_pipeline

# 2. Компілюємо код:
sbt compile

# 3. Запускаємо демонстраційний конвеєр та бенчмарк:
sbt run
```

---

## Резюме: Правило трьох секунд

* **Винятки лише для катастроф:** `throw Exception` — лише для непередбачуваних системних збоїв (падіння диска, OOM). Бізнес-помилки валідації моделюються через `Either`.
* **Smart Constructor:** Ховайте первинний конструктор (`private`), щоб не пропустити некоректні дані в домен.
* **Композиція замість моноліту:** Розбивайте складну обробку на ланцюжок чистих функцій (`andThen` / `foldLeft`).

---

## Контрольні питання для самоперевірки

<details>
<summary>1. Що станеться, якщо у for-comprehension над Either перший крок поверне Left, а другий викликає важку мережеву операцію?</summary>

Другий крок навіть не буде викликаний! За правилами монадичного зв'язування `flatMap`, як тільки генератор зустрічає `Left`, він миттєво повертає цей `Left` як результат усього виразу. Це захищає систему від марних обчислень.
</details>

<details>
<summary>2. Чому copy у case class є ключовим інструментом для побудови конвеєрів трансформації?</summary>

Тому що `copy` повертає **новий незмінний об'єкт**, змінюючи лише вказані поля (наприклад, `tx.copy(fee = 5.0)`), зберігаючи всі інші поля незмінними за принципом Structural Sharing без мутації оригінального об'єкта.
</details>

<details>
<summary>3. Чому метод partitionMap надійніший за поєднання двох викликів filter?</summary>

Виклик `filter` двічі (`list.filter(_.isLeft)` та `list.filter(_.isRight)`) проходить по всій колекції двічі (два проходи по пам'яті). `partitionMap` робить розділення **за один прохід** і одночасно розгортає значення з контейнерів (`Left(e) => e`, `Right(v) => v`), повертаючи готовий кортеж `(List[E], List[A])`.
</details>
