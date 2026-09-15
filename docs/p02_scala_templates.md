# Шаблони Scala (довідник)

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів — у [DISCLAIMER.md](DISCLAIMER.md).

Короткий **текстовий довідник шаблонів**: куди подивитись, коли в [p01](p01_parallelism.md) чи [p03](p03_javish_donation_jar.md) зустрічається незнайомий синтаксис. Окремого проєкту для запуску немає — лише шпаргалка. Повна теорія — на лекціях 2–4.

---

## Naming convention

Стиль імен — як у [Scala Style Guide](https://docs.scala-lang.org/style/naming-conventions.html):

| Що | Стиль | Приклад |
| -- | ----- | ------- |
| Пакет | lowercase, крапки | `fp`, `edu.onu.fp` |
| `class` / `trait` / `object` / `case class` | **PascalCase** | `DonationJar`, `JarLogic`, `Donation` |
| Точка входу (`@main`) | **camelCase** для методу, **PascalCase** для об'єкта | `@main def runWorkshop`, `object Workshop` |
| Метод / `def` | **camelCase** | `addDonation`, `sumFromDonations`, `donateSilent` |
| `val` / локальна змінна | **camelCase** | `finalJar`, `donationCount`, `currentJar` |
| `var` (рідко) | **camelCase** | `balance`, `currentJar` — лише в shell |
| Параметр функції | **camelCase** | `amount`, `donor`, `jarId` |
| Файл `.scala` | як головний тип у файлі | `Workshop.scala` → `object Workshop` |
| Тест-клас | ім'я + `Spec` | `WorkshopSpec`, `JavishPainSpec` |

**Запуск:**

| Ситуація | Команда |
| -------- | ------- |
| Один `main` у проєкті | `sbt run` |
| Кілька `main` | `sbt "runMain Workshop"` — ім'я **об'єкта** чи `@main` методу |
| Тести | `sbt test` |

---

## 1. Точка входу в програму

У старих статтях, книгах чи legacy-проєктах (Scala 2) ви часто можете побачити застарілий підхід:

```scala
// Старий стиль (Scala 2 / Legacy) — не використовувати в Scala 3
object Workshop extends App {
  println("Старт")
}
```

У Scala 3 `extends App` **заборонений до вжитку**, оскільки він базується на механізмі `DelayedInit` і може викликати приховані `NullPointerException` через збій порядку ініціалізації полів.

У Scala 3 є два чисті способи:

### Варіант А. Сучасний стандарт Scala 3 (анотація `@main`) — рекомендовано
Функція точки входу оголошується на верхньому рівні (top-level), обгортка `object` не потрібна:

```scala
// Файл Workshop.scala
@main def runWorkshop(): Unit =
  println("Старт")
```
* **Запуск у sbt:** `sbt "runMain runWorkshop"` (вказується ім'я методу).

### Варіант Б. Класичний об'єктний стиль (сумісний із Java)
Якщо код організовано як модуль усередині `object`:

```scala
object Workshop:
  def main(args: Array[String]): Unit =
    println("Старт")
```
* **Запуск у sbt:** `sbt "runMain Workshop"` (вказується ім'я об'єкта).

| Парадигма / Мова | Оголошення точки входу | Запуск через sbt |
| ---------------- | ---------------------- | ---------------- |
| **Java** | `public static void main(String[] args)` | `sbt "runMain Workshop"` |
| **Scala 2 (legacy)** | `object Workshop extends App` | `sbt "runMain Workshop"` (не рекомендовано) |
| **Scala 3 (сучасний)** | `@main def runWorkshop(): Unit` | `sbt "runMain runWorkshop"` |
| **Scala 3 (об'єктний)** | `object Workshop: def main(...)` | `sbt "runMain Workshop"` |

---

## 2. `val` і `var`

```scala
val pi = 3.14   // незмінна — основа ФП
var counter = 0 // мутація — рідко; зазвичай лише в Imperative Shell
```

---

## 3. Типи даних

Scala — **строго типізована** мова. Усі типи є повноцінними об'єктами (немає поділу на примітиви та класи-обгортки, як у Java).

### Основні базові типи

| Тип | Опис | Приклад значення |
| --- | ---- | ---------------- |
| `Int`, `Long` | Цілі числа (32 та 64 біт) | `42`, `1000L` |
| `Double`, `Float` | Дійсні числа (64 та 32 біт) | `3.14`, `3.14f` |
| `Boolean` | Логічний тип | `true`, `false` |
| `Char`, `String` | Символ та текст | `'A'`, `"Scala 3"` |
| `Unit` | Відсутність корисного значення (аналог `void`) | `()` (єдине значення) |
| `BigInt`, `BigDecimal` | Довільна точність (фінанси, великі суми) | `BigInt("1000000000000")` |

---

### Як вказувати типи: зліва чи в правій частині?

Зазвичай компілятор **виводить тип автоматично** (`val n = 42` ⭢ `Int`). Якщо потрібна конкретизація:

#### 1. Класично: явний тип зліва (Type Annotation)
```scala
val rate: Double = 0.5   // чітко видно тип змінної
val count: Long = 100    // компілятор перетворить Int-літерал на Long
```

#### 2. У правій частині (Type Ascription та суфікси літералів)
Іноді зручно чи необхідно уточнити тип саме самого **виразу** праворуч:

* **Літеральні суфікси:**
  ```scala
  val id = 42L         // тип виводиться як Long завдяки суфіксу 'L'
  val weight = 75.0f   // Float завдяки суфіксу 'f'
  ```
* **Приписування типу (Type Ascription `expr: Type`):**
  Безпечна перевірка компілятором під час збірки (не плутати з небезпечним `asInstanceOf`):
  ```scala
  val rate = 42: Double        // змушує компілятор розглядати вираз як Double
  val empty = Nil: List[Int]   // типізуємо порожній список Nil
  ```
  > **Що таке `Nil`?** Це синглтон-об'єкт, що позначає **порожній незмінний список** (`List[Nothing]`). Усі зв'язні списки у ФП завершуються ним (наприклад, `1 :: 2 :: 3 :: Nil` — це те саме, що `List(1, 2, 3)`).
* **Методи приведення (`to<Type>`):**
  ```scala
  val balance = 100.toDouble   // явний виклик методу конвертації
  ```
* **Параметри типів у фабриках / конструкторах:**
  ```scala
  val items = List.empty[String] // створює список потрібного типу
  ```

---

## 4. Методи (`def`) та функції-значення (`val`)

```scala
def square(x: Int): Int = x * x           // метод (def): нуль накладних витрат пам'яті
val double: Int => Int = x => x * 2       // повний синтаксис лямбди
val short = (_: Int) * 2                   // скорочена лямбда (через placeholder _)
```

### У чому різниця між `def` та `val`?

| Критерій | Метод: `def square(x: Int): Int` | Лямбда: `val double: Int => Int` |
| -------- | --------------------------------- | --------------------------------- |
| **Рівень JVM** | Звичайний метод байткоду (без виділення пам'яті в heap) | Об'єкт-екземпляр трейту `Function1` (аналог `interface` у Java) |
| **Чи є значенням?** | **Ні.** Належить класу або об'єкту | **Так (First-class citizen).** Зберігається у змінній, передається як дані |
| **Параметри типів** | Підтримує дженерики: `def id[T](x: T): T` | Не підтримує власні type parameters |
| **Передача в `.map`** | Автоматично конвертується компілятором у функцію (**Eta-expansion**) | Вже є готовою функцією |

> **Що таке трейт `Function1`?**
> * **`trait`** — це аналог `interface` у Java.
> * Цифра **`1`** вказує на кількість вхідних параметрів (**арність**): `Function1[Вхід, Результат]`. Для 2 параметрів є `Function2`, для 0 — `Function0`. Запис типу `Int => Int` — це зручний синтаксичний цукор для `Function1[Int, Int]`.
> * Трейт має контракт `apply(x: A): B`. Тому виклик `double(3)` компілятор перетворює на `double.apply(3)`.

> **Правило інженера:** Для логіки та функцій сервісів завжди використовуйте **`def`**. Лямбди пишіть за місцем (inline) як аргументи комбінаторів (`list.map(x => x * 2)`) або передавайте сам метод (`list.map(square)`).

---

**Як читати синтаксис:**

| Запис | Читання |
| ----- | ------- |
| `def square(x: Int): Int` | метод `square`: приймає `Int`, повертає `Int` |
| `= x * x` | тіло — вираз; останнє значення = результат (без `return`) |
| `Int => Int` | тип функції: «з `Int` в `Int`» (стрілка читається зліва направо) |
| `x => x * 2` | лямбда: «параметр `x` подається у вираз `x * 2`» |
| `=>` (у лямбді) | «подати параметр у вираз праворуч» — не плутати з `=` (присвоєння) |
| `(_: Int) * 2` | те саме, що `x => x * 2`; `_` — місце параметра, `Int` — його тип |

```scala
square(5)        // 25 — звичайний виклик методу
double(3)        // 6  — виклик функції-значення (цукор для double.apply(3))
nums.map(square) // передача def-методу у функцію вищого порядку (HOF, через Eta-expansion)
nums.map(_ * 2)  // _ — скорочений запис для одного параметра
```

> Повний розбір функцій вищого порядку (Higher-Order Functions, HOF) — лекція 2 (див. [план курсу](course_structure.md)).

---

## 5. `class`, `object`, `case class`

```scala
class Greeter(val who: String):
  def greet(): String = s"Hello, $who" // метод екземпляра

// Об'єкт-компаньйон (має те саме ім'я й лежить у тому ж файлі):
object Greeter:
  def formal(who: String): String = s"Dear $who"      // заміна static-методу
  def apply(who: String): Greeter = new Greeter(who)  // фабрика: дозволяє писати Greeter("Alice") без new
```

> **Що таке Companion Object (об'єкт-компаньйон)?**
> Це синглтон-об'єкт (`object`), який має **точно таке саме ім'я**, що й клас, і лежить у **тому ж файлі**.
> * **Заміна `static`:** у Scala немає ключового слова `static`. Усі фабричні методи, утиліти, константи та парсери виносять у companion object.
> * **Спільний доступ:** клас і його компаньйон мають доступ до приватних полів (`private`) один одного.
> * **Фабрика `apply`:** метод `apply` у компаньйоні дозволяє створювати об'єкти без слова `new`: `Greeter("Alice")`. Для `case class` такий компаньйон з `apply` створюється автоматично.

### Що робить `case class` і чому це основа ФП?

`case class` — це основа моделювання незмінних даних (Domain Models / DTO). Це аналог `record` у сучасній Java, але зі значно більшими можливостями.

Коли ви оголошуєте `final case class Donation(amount: Double, donor: String)`, компілятор Scala **автоматично генерує «під капотом»**:

1. **Усі поля — публічні й незмінні (`val`):**
   `d.amount = 50` не скомпілюється — випадково змінити стан неможливо.
2. **Створення без ключового слова `new`:**
   Через автозгенерований метод `apply` у companion object:
   ```scala
   val d1 = Donation(100.0, "Alice") // просто і лаконічно
   ```
3. **Порівняння за значенням полів (`equals` / `hashCode`):**
   На відміну від звичайного класу Java, де `==` порівнює посилання в пам'яті:
   ```scala
   val d2 = Donation(100.0, "Alice")
   d1 == d2 // true! (структурна рівність за значенням)
   ```
4. **Метод `.copy(...)` (безстанове оновлення):**
   Компілятор сам генерує цей метод всередині `case class`. Під капотом параметри методу мають дефолтні значення поточних полів (`this.field`):
   ```scala
   // Що компілятор генерує під капотом:
   // def copy(amount: Double = this.amount, donor: String = this.donor): Donation = new Donation(amount, donor)

   val d3 = d1.copy(amount = 250.0) // d1 не змінився; donor взято автоматично з d1, а amount оновлено
   ```
   Завдяки **іменованим аргументам** (`amount = 250.0`) ми перевизначаємо лише потрібні поля, а всі інші залишаються без змін. У звичайному `class` такого методу немає.
5. **Людиночитабельний `toString`:**
   Автоматично виводить `Donation(100.0,Alice)`, а не `Donation@4f32a`.
6. **Готовність до Pattern Matching (екстрактор `unapply`):**
   Можна миттєво розбирати на складові у `match`:
   ```scala
   d1 match
     case Donation(amount, "Alice") => s"Донат від Аліси на $amount грн"
     case Donation(amount, donor)   => s"Донат від $donor"
   ```

> ⚠️ **Чому саме `final case class`?**
> Успадкування одного `case class` від іншого ламає математичну симетрію `equals` та вичерпність перевірок компілятора у `match`. Завжди ставте **`final`** (якщо тільки це не ієрархія `sealed trait`).

| Конструкція | Навіщо | Аналог у Java |
| ----------- | ------ | ------------- |
| `class` | Об'єкт з поведінкою (може мати мутабельний стан) | Звичайний `class` |
| `object` | Синглтон на рівні JVM (модуль утиліт або компаньйон) | Клас зі `static`-методами |
| `final case class` | Незмінна структура даних (DTO / Value Object) | `record` (або `@Value` у Lombok) |

---

## 6. `if` і `match` як **вирази**

У Scala `if` і `match` **повертають значення** (expression-oriented), а не лише керують потоком:

```scala
val label = if (n > 0) "plus" else "minus"

sealed trait Grade
case object Pass extends Grade
case object Fail extends Grade

def symbol(g: Grade): String = g match
  case Pass => "✓"
  case Fail => "✗"
```

> **Що означає `sealed` («запечатаний»)?**
> * **Усі нащадки — в одному файлі:** розширювати `sealed trait` (робити `extends`) дозволено **лише в тому самому файлі**, де він оголошений. Сторонній код не може додати новий варіант.
> * **Контроль вичерпності (Exhaustiveness Check):** оскільки всі варіанти відомі на етапі компіляції, компілятор видасть **warning/error**, якщо ви забудете обробити хоча б один випадок у `match` (наприклад, забули `case Fail`). Це захищає від рантайм-падінь (`MatchError`).
> * **Аналог у Java 17+:** `sealed interface ... permits Pass, Fail`. У ФП це основа моделювання **ADT** (Algebraic Data Types) — закритих наборів станів програми.

---

## 7. Колекції та конвеєр

```scala
val nums = List(1, 2, 3, 4, 5)

val result = nums
  .filter(_ % 2 == 0)
  .map(_ * 10)
  .sum

val range = (1 to 100).toVector // 1.to(100) — infix
```

| Операція | Що робить |
| -------- | --------- |
| `map(f)` | перетворити кожен елемент |
| `filter(p)` | залишити ті, що проходять умову |
| `sum` | згорнути числа в суму |

У [p01](p01_parallelism.md) той самий конвеєр + `.par` для паралелізму.

---

## 8. `Option` (короткий огляд)

`Option[A]` — це функціональна безпечна заміна `null` і захист від `NullPointerException`. Це `sealed trait` (контейнер), який має рівно два стани:
* **`Some(value)`** — значення **присутнє** (обгортка над реальними даними).
* **`None`** — значення **відсутнє** (синглтон-об'єкт, безпечне "нічого").

```scala
def parsePort(s: String): Option[Int] =
  s.toIntOption // повертає Some(число) або None при помилці

parsePort("8080")  // Some(8080) — результат є
parsePort("oops")  // None       — результату немає (замість винятку)

// Спосіб 1: Отримання значення з дефолтом
parsePort("8080").getOrElse(80) // 8080
parsePort("oops").getOrElse(80) // 80 (дефолт, якщо всередині None)

// Спосіб 2: Розбір через Pattern Matching
parsePort("8080") match
  case Some(port) => s"Слухаємо порт $port"
  case None       => "Порт не задано"
```

| Стан у Scala | Що означає | Аналог у Java (`java.util.Optional`) |
| ------------ | ---------- | ----------------------------------- |
| `Some(value)` | Значення є | `Optional.of(value)` |
| `None` | Значення відсутнє | `Optional.empty()` |

> Повний розбір обробки відсутності даних та помилок — лекція 3 (`Option`, `Either`, `Try`).

---

## 9. `Unit` і побічні ефекти

```scala
def pure(x: Int): Int = x * 2          // повертає результат обчислення
def noisy(x: Int): Unit = println(x)  // Unit ≈ void; зазвичай side effect
```

Чисті функції повертають **дані**, не `Unit`. Логування й `println` — на краю програми ([p03](p03_javish_donation_jar.md), `DonationApp`).

---

## 10. Обробка помилок через `Either` та розподіл (`partitionMap`)

### Що таке `Either[Error, Value]`?

`Either` — це стандартний функціональний тип для результату, який може бути або **помилкою**, або **успіхом**:

* **`Right(value)`** — успішний результат (**"Right is right"** — мнемонічне правило: «правий» означає «правильний/успіх»).
* **`Left(error)`** — бізнес-помилка або повідомлення про збій (ліва гілка).

| Тип | Що повертає при помилці | Коли використовувати |
| --- | ----------------------- | -------------------- |
| **`Option[A]`** | `None` (причина невідома) | Коли факт відсутності даних — норма, і причина не важлива |
| **`Either[E, A]`** | `Left(error)` (містить опис або тип помилки) | Коли клієнту треба точно знати, **що саме пішло не так** |

```scala
// Метод .toRight() перетворює Some(v) -> Right(v), а None -> Left(помилка):
def parseNumber(s: String): Either[String, Int] =
  s.toIntOption.toRight(s"Не число: '$s'")

parseNumber("42")   // Right(42)
parseNumber("oops") // Left("Не число: 'oops'")
```

---

### Залізничний розподіл за один прохід (`partitionMap`)

Коли є потік або список сирих даних і потрібно розділити його на **список помилок** та **список валідних значень**:

```scala
val rawItems = List("10", "abc", "25", "bad")

// ❌ Антипатерн (2 проходи по пам'яті O(2N) + зайва фільтрація):
// val fails = rawItems.map(parseNumber).filter(_.isLeft)
// val goods = rawItems.map(parseNumber).filter(_.isRight)

// ✅ Ідіоматично (1 прохід O(N), повертає кортеж двох розпакованих списків):
val (errors, numbers) = rawItems.partitionMap(parseNumber)
// errors  = List("Не число: 'abc'", "Не число: 'bad'")
// numbers = List(10, 25)
```

> **Railway-Oriented Programming (ROP):** `Either` — це рейки. Поки все добре, потяг їде зеленою колією (`Right`). Як тільки стається помилка, стрілка перемикає потік на червону колію (`Left`).
> Детальна теорія — у [Лекції 03](03_railway_oriented_programming.md) та практиках [p04](p04_transaction_pipeline.md) / [p05](p05_heart_disease_triage.md).

---

## Куди далі

| Потреба | Куди |
| ------- | ---- |
| Паралелізм, race condition | [p01](p01_parallelism.md) |
| Java-ish vs чисте ФП, shell | [p03](p03_javish_donation_jar.md) |
| Railway-Oriented та `partitionMap` | [p04](p04_transaction_pipeline.md), [Лекція 03](03_railway_oriented_programming.md) |
| Immutability, structural sharing | [Лекція 01](01_immutability_and_state.md) |
| Офіційний тур по Scala | [Scala Docs](https://docs.scala-lang.org/tour/tour-of-scala.html) |
