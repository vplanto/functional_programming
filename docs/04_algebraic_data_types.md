# Лекція 04. Моделювання предметної області (Type-Driven Design): Алгебраїчні типи даних (ADT), унеможливлення некоректних станів та вичерпний Pattern Matching

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів — у [DISCLAIMER.md](DISCLAIMER.md).  
> 💡 **Місток курсу:** У [Лекції 03](03_railway_oriented_programming.md) ми навчилися безпечно обробляти помилки без `null` та `Exceptions`. Тепер постає архітектурне питання: **як проєктувати моделі бізнес-домену так, щоб баги та невалідні стани системи відсікалися ще на етапі компіляції?**

---

## Питання для розминки

<details>
<summary>1. Чому текстовий рядок status: String = "ACTIVE" у доменній моделі — це міна сповільненої дії?</summary>

Тому що компілятор вважає валідним абсолютно будь-який рядок: `"Active"`, `"activ "`, `"COMPLETED"`, `"DELETE FROM users"`. Будь-яка механічна одруківка в коді успішно скомпілюється, але під час виконання призведе до мовчазного збою бізнес-логіки. Статуси мають бути строго типізованими закритими множинами (`enum`), а не довільним текстом.
</details>

<details>
<summary>2. Що означає математичне слово «Алгебраїчні» у назві Алгебраїчних типів даних (ADT)?</summary>

У звичайній шкільній алгебрі ми додаємо та множимо числа ($2 + 3 = 5$, $2 \times 3 = 6$). В алгебрі типів ми **множимо** незалежні властивості через типи-добутки (**Product Types / AND**: `case class`) та **додаємо** взаємовиключні альтернативи через типи-суми (**Sum Types / OR**: `enum`).
</details>

<details>
<summary>3. Що краще: написати 100 тестів на перевірку коректності стану чи спроєктувати типи так, щоб некоректний стан було неможливо скомпілювати?</summary>

Тести перевіряють лише ті сценарії, про які здогадався подумати розробник. Строга система типів гарантує математичну неможливість помилки для 100% випадків. Як сформулював Ярон Мінскі з фінансової компанії Jane Street: **«Зробіть некоректні стани неможливими для представлення (Make illegal states unrepresentable)»**.
</details>

---

## 1. Архітектурний кейс: «Зомбі-замовлення з трекінг-номером»

**Контекст:** Міжнародна eCommerce-платформа обробляє 100 000 замовлень на добу.  
В імперативному ООП сутності зазвичай моделюють як один гігантський клас з усіма можливими полями:

```java
// ❌ Типовий роздутий ООП-клас у стилі Java:
public class Order {
    private String status;           // "PENDING", "SHIPPED", "CANCELLED"
    private String trackingNumber;   // Має бути ТІЛЬКИ для SHIPPED
    private String cancellationReason; // Має бути ТІЛЬКИ для CANCELLED
    private Long refundTransactionId; // Має бути ТІЛЬКИ для CANCELLED
    // ... ще 20 полів та гетери/сетери
}
```

### Що пішло не так:
1. Покупець скасував дороге замовлення на 50 000 $. Оператор техпідтримки викликав `order.setStatus("CANCELLED")` і оформив повернення коштів.
2. Але через збій у паралельному потоці раніше згенерований `trackingNumber` **залишився в об'єкті**.
3. Фоновий складський робот кожні 5 хвилин вичитував із бази всі замовлення з ненульовим трекінг-номером:
   ```java
   if (order.getTrackingNumber() != null) {
       warehouseApi.dispatchPackage(order); // Відвантажити зі складу!
   }
   ```
4. Робот відвантажив клієнту дорогу техніку зі складу, хоча гроші за неї вже повернули на картку покупця.
5. Збитки компанії за вихідні перевищили $200 000.

**У чому корінь проблеми?**  
Клас `Order` дозволяв одночасно існувати взаємовиключним даним: замовлення одночасно мало статус `"CANCELLED"` і реальний `trackingNumber`. Компілятор мовчав, бо типи дозволяли цей абсурд.

---

## 2. Алгебра типів: Product Types (AND) та Sum Types (OR)

Будь-яку сутність у світі можна скласти з двох базових алгебраїчних операцій над типами:

```
                      Алгебраїчні типи даних (ADT)
                                   │
         ┌─────────────────────────┴─────────────────────────┐
         ▼                                                   ▼
  Product Type (AND)                                   Sum Type (OR)
     Тип-добуток                                         Тип-сума
     `case class`                                         `enum`
  Атрибут А І Атрибут Б                             Варіант А АБО Варіант Б
```

### 1. Product Type (Тип-добуток / AND)
Коли об'єкт складається з кількох полів одночасно.  
У Scala 3 це звичайний незмінний **`case class`**:

```scala
case class GeoPoint(lat: Double, lon: Double)
```
* **Чому це добуток?**  
  Якщо поле `lat` має $N$ можливих значень, а `lon` — $M$, то загальна кількість можливих точок на карті дорівнює їхньому добутку: $|GeoPoint| = N \times M$.

---

### 2. Sum Type (Тип-сума / OR / Coproduct)
Коли сутність може перебувати **лише в одному** із заздалегідь відомих взаємовиключних станів.  
У Scala 3 це моделюється через **`enum`** або **`sealed trait`**:

```scala
enum PaymentMethod:
  case Cash
  case CreditCard(number: String, cvv: String)
  case ApplePay(token: String)
```
* **Чому це сума?**  
  Оплата може бути здійснена АБО готівкою ($1$), АБО карткою ($M$), АБО Apple Pay ($K$). Загальна множина можливих способів є сумою: $1 + M + K$.

---

## 3. «Make Illegal States Unrepresentable» на практиці

Перепишемо бізнес-модель замовлення за правилами строгого Type-Driven Design.  
Замість одного роздутого класу створюємо **тип-суму (Sum Type)**:

```scala
// ✅ Тепер кожен стан має ТІЛЬКИ ті дані, які йому належать:
enum Order:
  case Pending(createdAt: Long)
  case Shipped(trackingNumber: String, dispatchedAt: Long)
  case Cancelled(reason: String, refundTransactionId: Long)
```

### Чому ця архітектура бездоганна:
1. **Неможливість збою:** Ви фізично не можете створити об'єкт `Order.Cancelled`, у якого є `trackingNumber`. Такого поля у нього просто **не існує в пам'яті**.
2. **Нуль `null`:** Нам більше не потрібні поля з `null` чи надлишкові `Option` для кожного параметру.
3. **Чесність перед базою даних:** Стан системи завжди гарантовано консистентний.

---

### 🛡️ Паттерн «Розумний конструктор» (Smart Constructor)
ADT чудово захищає від *структурно* неможливих станів. Але як захистити систему від некоректних *значень* усередині валідних полів?  
Наприклад, якщо ми створимо `case class Email(value: String)`, ніхто не завадить передати туди `Email("not-an-email")`, і ми знову отримаємо збій у рантаймі під час відправки пошти!

**Рішення:** приватний первинний конструктор та фабричний метод у компаньйон-об'єкті, що повертає `Either`:

```scala
// 1. Конструктор приватний: ніхто ззовні не може викликати new Email(...)
final case class Email private (value: String)

object Email:
  // 2. Єдиний шлях створення — перевірений шлюз, що повертає Either:
  def fromString(raw: String): Either[String, Email] =
    val trimmed = raw.trim
    if trimmed.contains("@") && trimmed.split("@").length == 2 then
      Right(new Email(trimmed))
    else
      Left(s"Некоректний формат email: '${raw}'")

// Використання:
val badEmail  = Email.fromString("broken-string") // Left(Некоректний формат email...)
val goodEmail = Email.fromString("user@onu.edu.ua") // Right(Email(user@onu.edu.ua))
```
Це ідеальний шлюб між **Railway-Oriented Programming (Either з Лекції 03)** та **Type-Driven Design**: якщо функція приймає `Email`, їй більше непотрібно валідувати рядок — валідність гарантована самим фактом існування типу!

---

### 🌲 Рекурсивні типи даних (Recursive ADTs): Анатомія списку
Алгебраїчні типи даних можуть посилатися на самих себе. Класичний незмінний `List` у Scala — це не магія мови, а звичайний рекурсивний Sum Type, який кожен може написати за 5 хвилин:

```scala
// Наш власний однозв'язний список:
enum MyList[+A]:
  case Nil                                 // Порожній список (термінальний вузол)
  case Cons(head: A, tail: MyList[A])      // Рекурсивна ланка: елемент + залишок списку

// Створення списку 1 -> 2 -> 3 -> Nil:
import MyList._
val numbers = Cons(1, Cons(2, Cons(3, Nil)))

import scala.annotation.tailrec

// Хвостово-рекурсивний підрахунок суми (Stack-Safe: O(1) стеку, оптимізується компілятором у цикл):
def sum(list: MyList[Int]): Int =
  @tailrec
  def loop(curr: MyList[Int], acc: Int): Int = curr match
    case MyList.Nil         => acc
    case MyList.Cons(x, xs) => loop(xs, acc + x)

  loop(list, 0)
```

> 💡 **Чому це критично для Production (@tailrec):**  
> Наївний виклик `case Cons(x, xs) => x + sum(xs)` не є хвостовим: операція `+` змушує JVM тримати у стеку всі попередні виклики. На списку з 100 000 елементів це призведе до **`java.lang.StackOverflowError`**. Анотація `@tailrec` гарантує, що компілятор перетворить рекурсію на звичайний плоский цикл байткоду без зростання стеку пам'яті.

---

## 4. Живий зв'язок із нашими практичними проєктами

Саме на цій ідеології ми будували весь наш код:

### А. Тріаж у [04_heart_triage (Практика 05)](p05_heart_disease_triage.md):
Вердикт кардіологічного консиліуму змодельований як чіткий ADT:

```scala
enum TriageVerdict:
  case CriticalHospitalize(patientId: Int, highVotes: Int, diagnoses: List[Diagnosis])
  case SplitOpinionDisagreement(patientId: Int, summary: String, diagnoses: List[Diagnosis])
  case ModerateRiskObservation(patientId: Int, moderateVotes: Int, diagnoses: List[Diagnosis])
  case LowRiskDischarge(patientId: Int, lowVotes: Int, diagnoses: List[Diagnosis])
```
Неможливо випадково відправити пацієнта додому з прапорцем `highVotes = 3` — типи розділені на рівні компілятора!

### Б. Ігровий світ у [23_runner (Практика 06)](p06_engine_architecture.md):
Кожна перешкода в тунелі — це замкнений Sum Type:

```scala
enum Obstacle:
  case LowBarrier
  case HighBarrier
  case LeftWall
  case RightWall
```

---

## 5. Вичерпний Pattern Matching (Exhaustiveness Checking)

Коли стан змодельовано через ADT, класичний оператор `switch` із Java стає непотрібним. Ми використовуємо **Pattern Matching**:

```scala
def processOrder(order: Order): Unit = order match
  case Order.Pending(time) =>
    println(s"Очікує оплати від $time")
    
  case Order.Shipped(trackId, _) =>
    notifyCourier(trackId)
    
  case Order.Cancelled(reason, refundId) =>
    refundMoney(refundId)
```

### Компілятор як безкоштовний QA-інженер
Уявіть, що через пів року бізнес додав новий статус замовлення:  
`case ReturnedToWarehouse(defectDescription: String)`.

Якщо ви забудете обробити його десь у глибині 100 000 рядків коду, компілятор Scala 3 **не скомпілює проєкт** і видасть попередження:

```text
[warn] match may not be exhaustive!
[warn] It would fail on the following input: Order.ReturnedToWarehouse(_)
```

**Це кінець епохи непередбачуваних рантайм-помилок:**  
Вам більше не потрібно шукати всі виклики через текстовий `grep` по проєкту — компілятор сам перерахує точні номери рядків у всіх файлах, де потрібно дописати обробку нового бізнес-стану.

---

### 🚨 Пастка лінивого розробника: Чому дефолтний `case _` — це міна сповільненої дії

Студенти часто полюбляють ставити `case _ => ...` наприкінці кожного `match`, аби швидше задовольнити компілятор.  
В індустріальному коді це **один із найнебезпечніших антипатернів**.

#### Драматичний сценарій: Втрата грошей через Wildcard
Уявіть платіжну систему банку:

```scala
enum PaymentStatus:
  case Pending
  case Authorized
  case Declined

def processPayout(status: PaymentStatus): Unit = status match
  case PaymentStatus.Authorized => releaseFundsToMerchant()
  case PaymentStatus.Declined   => notifyCustomer()
  case _                        => logWarning("Невідомий статус, пропустимо")
```

Через пів року фінмоніторинг додає новий критичний статус: `case FraudSuspicious` (підозра на шахрайство).

1. **Що станеться з `case _`:**  
   Компілятор мовчки скомпілює код! Підозріла транзакція провалиться в `case _`, гроші будуть заблоковані не там або система не сповістить безпеку, і компанія зазнає збитків.
2. **Що станеться БЕЗ `case _`:**  
   Компілятор відмовиться збирати проєкт:
   ```text
   [warn/error] match may not be exhaustive!
   [warn/error] It would fail on the following input: PaymentStatus.FraudSuspicious
   ```
   Він буквально бере розробника за руку й змушує явно визначити поведінку для кожного нового стану.

> 💡 **Золоте правило інженера:**  
> Пишіть дефолтний `case _` тільки тоді, коли ви **свідомо** хочете ігнорувати абсолютно всі майбутні зміни типу. В інших 95% випадків перераховуйте всі гілки явно!

---

## 6. Pattern Guards (Охоронні умови) та деструктуризація

Pattern Matching уміє заглядати глибоко всередину вкладених структур і перевіряти складні логічні умови прямо в заголовку `case`.

### Зіставлення кортежу голосів лікарів (з нашого `ConsensusArbiter`):

```scala
// Зіставляємо три незалежні лічильники одночасно:
(highVotes, moderateVotes, lowVotes) match
  // 1. Охоронна умова (Guard): спрацює, лише якщо h >= 2
  case (h, _, _) if h >= 2 =>
    TriageVerdict.CriticalHospitalize(patient.id, h, diagnoses)

  // 2. Точне зіставлення зі значенням 1:
  case (1, m, l) =>
    TriageVerdict.SplitOpinionDisagreement(patient.id, s"1 High, $m Mod, $l Low", diagnoses)

  // 3. Комбінована перевірка: нуль High і хоча б два Moderate:
  case (0, m, _) if m >= 2 =>
    TriageVerdict.ModerateRiskObservation(patient.id, m, diagnoses)

  // 4. Усі інші випадки:
  case (_, _, l) =>
    TriageVerdict.LowRiskDischarge(patient.id, l, diagnoses)
```

---

## 7. 🏛️ Куточок архітектора: Моделювання бізнес-процесів як скінченних автоматів (State Machine / FSM)

ADT — це не просто пасивні контейнери для даних. Це **математичний фундамент для моделювання життєвого циклу бізнес-процесів**.

У надійних розподілених системах перехід між станами моделюють як чисту детерміновану функцію:

$$\text{transition}: (\text{State}, \text{Command}) \implies \text{Either}[\text{DomainError}, \text{State}]$$

### Приклад: Управління життєвим циклом замовлення

```scala
// 1. Команди (події), що надходять у систему:
enum OrderCommand:
  case Ship(trackingNumber: String)
  case Cancel(reason: String)
  case Deliver

// 2. Чистий перехід станів (State Transition FSM):
def handleCommand(order: Order, cmd: OrderCommand): Either[String, Order] = 
  (order, cmd) match
    // Валідний перехід: Pending -> Shipped
    case (Order.Pending(_), OrderCommand.Ship(track)) =>
      Right(Order.Shipped(track, System.currentTimeMillis()))

    // Валідний перехід: Pending -> Cancelled
    case (Order.Pending(_), OrderCommand.Cancel(reason)) =>
      Right(Order.Cancelled(reason, refundTransactionId = 987654L))

    // ❌ Спроба відправити вже скасоване замовлення — неможливий перехід!
    case (Order.Cancelled(_, _), OrderCommand.Ship(_)) =>
      Left("Помилка домену: неможливо відправити скасоване замовлення!")

    // ❌ Будь-яка інша заборонена бізнес-правилами комбінація:
    case (current, illegalCmd) =>
      Left(s"Недопустима дія $illegalCmd для поточного стану $current!")
```

### Чому цей патерн фундаментальний:
* **Неможливість збою гонитви (Race Conditions):** Функція переходу станів є чистою. Якщо два запити прийдуть одночасно, один із них гарантовано поверне зрозумілий `Left`.
* **Інкапсуляція бізнес-правил:** Усі дозволені та заборонені переходи описані в одній матриці `match`, а не розкидані по 10 різних сервісах.
* **Місток до Event Sourcing та Akka/Pekko:** Саме за таким принципом функціонують розподілені актори, які ми будемо створювати в блоці реактивних систем!

---

## Резюме: Правило трьох секунд

* **Моделюйте бізнес-домен через ADT:** Комбінуйте `case class` (AND) та `enum` (OR).
* **Make Illegal States Unrepresentable:** Не зберігайте невалідні комбінації даних, які потім доведеться ловити валідаторами.
* **Довіряйте компілятору:** Вичерпний Pattern Matching гарантує, що при розширенні системи жоден бізнес-сценарій не буде забутий.

---

## Контрольні питання для самоперевірки

<details>
<summary>1. Чому при моделюванні через sealed trait або enum усі підтипи мають бути оголошені в одному файлі?</summary>

Ключове слово `sealed` ("запечатаний") забороняє розширювати ієрархію за межами поточного файлу вихідного коду. Тільки за цієї умови компілятор знає **повний і закритий перелік усіх можливих нащадків** і може гарантувати 100% вичерпність перевірки (Exhaustiveness Check) у `match`.
</details>

<details>
<summary>2. У чому архітектурна небезпека використання `case _ =>` (Default Wildcard) у Pattern Matching для бізнес-сутностей?</summary>

Сліпий `case _ =>` "заглушає" діагностику компілятора. Якщо через рік у систему додадуть новий важливий стан (наприклад, `Order.FraudSuspicion`), компілятор промовчить, і цей критичний випадок тихо провалиться в дефолтну гілку `case _`, спричинивши непередбачувану поведінку. Намагайтеся перераховувати всі стани явно!
</details>

<details>
<summary>3. Скільки можливих станів має тип Pair[Boolean, Option[Boolean]]?</summary>

Порахуємо за правилами алгебри типів:
* `Boolean` має $2$ стани (`true`, `false`).
* `Option[Boolean]` — це тип-сума: `None` ($1$) або `Some(Boolean)` ($2$). Разом: $1 + 2 = 3$ стани.
* `Pair` — це тип-добуток (Product Type): $|Boolean| \times |Option[Boolean]| = 2 \times 3 = \mathbf{6}$ унікальних станів.
</details>
