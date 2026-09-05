# Практика 01: Шаблони Scala (довідник)

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів — у [DISCLAIMER.md](DISCLAIMER.md).

Короткий **текстовий довідник шаблонів**: куди подивитись, коли в [p00](p00_parallelism.md) чи [p02](p02_javish_donation_jar.md) зустрічається незнайомий синтаксис. Окремого проєкту для запуску немає — лише шпаргалка. Повна теорія — на лекціях 2–4.

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
| `var` | **camelCase** (рідко) | `balance`, `currentJar` — лише в shell |
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

## 1. Точка входу (анотація `@main` у Scala 3)

У Scala 3 застарілий `extends App` **не використовується**, оскільки він базується на механізмі `DelayedInit` і може викликати непередбачувані `NullPointerException` через змінений порядок ініціалізації полів. Замість нього стандартом є анотація `@main`:

```scala
object Workshop:
  @main def runWorkshop(): Unit = {
    println("Старт")
  }

  def main(args: Array[String]): Unit = runWorkshop()
```

| Java | Scala 3 |
| ---- | ------- |
| `public static void main(String[] args)` | `@main def run(): Unit` або `def main(...)` |

Ім'я `object` чи `@main` методу та команда запуску — [Naming convention](#naming-convention) вище.

---

## 2. `val` і `var`

```scala
val pi = 3.14   // незмінна — основа ФП
var counter = 0 // мутація — рідко; зазвичай лише в Imperative Shell
```

---

## 3. Типи

Scala **строго типізована**, але тип часто **виводиться** компілятором:

```scala
val n = 42             // Int
val rate: Double = 0.5 // явний тип — коли потрібна ясність
```

---

## 4. Функції

```scala
def square(x: Int): Int = x * x           // останній вираз = return
val double: Int => Int = x => x * 2       // лямбда; тип функції після :
val short = (_: Int) * 2                   // _ — placeholder параметра
```

**Як читати:**

| Запис | Читання |
| ----- | ------- |
| `def square(x: Int): Int` | функція `square`: приймає `Int`, повертає `Int` |
| `= x * x` | тіло — вираз; останнє значення = результат (без `return`) |
| `Int => Int` | тип функції: «з `Int` в `Int`» (стрілка читається зліва направо) |
| `x => x * 2` | лямбда: «параметр `x` подається в вираз `x * 2`» |
| `=>` (у лямбді) | «подати параметр у вираз праворуч» — не плутати з `=` (присвоєння) |
| `(_: Int) * 2` | те саме, що `x => x * 2`; `_` — місце параметра, `Int` — його тип |

```scala
square(5)        // 25 — звичний виклик
double(3)        // 6   — виклик значення-функції
nums.map(_ * 2)  // _ — один параметр (елемент списку)
```

> Повний розбір функцій вищого порядку — лекція 2 (див. [план курсу](course_structure.md)).

---

## 5. `class`, `object`, `case class`

```scala
class Greeter(val who: String) {
  def greet(): String = s"Hello, $who"
}

object Greeter { // синглтон: утиліти, фабрики, companion
  def formal(who: String): String = s"Dear $who"
}

final case class Point(x: Int, y: Int) // незмінний запис даних
val p = Point(1, 2)
val moved = p.copy(x = 3)              // нова копія, p не змінився
```

| Конструкція | Навіщо |
| ----------- | ------ |
| `class` | об'єкт з поведінкою (може мутувати, якщо є `var`) |
| `object` | один екземпляр на JVM; модуль без стану |
| `case class` | незмінні дані + `copy` / `equals` / `hashCode` |

---

## 6. `if` і `match` як **вирази**

У Scala `if` і `match` **повертають значення** (expression-oriented), а не лише керують потоком:

```scala
val label = if (n > 0) "plus" else "minus"

sealed trait Grade
case object Pass extends Grade
case object Fail extends Grade

def symbol(g: Grade): String = g match {
  case Pass => "✓"
  case Fail => "✗"
}
```

`sealed` + `match` — перший контакт з pattern matching (у [p00](p00_parallelism.md) ви вже пробували `RiskLevel`). Теорія exhaustiveness і ADT — лекції 2–3.

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

У [p00](p00_parallelism.md) той самий конвеєр + `.par` для паралелізму.

---

## 8. `Option` (teaser)

```scala
def parsePort(s: String): Option[Int] =
  s.toIntOption // Scala 2.13+

parsePort("8080")  // Some(8080)
parsePort("oops")  // None

parsePort("8080").getOrElse(0) // 8080 — або значення за замовчуванням
```

`Option` — спосіб сказати «значення може бути, а може й ні» без `null`. Повний розбір — лекція 3 (`Option`, `Either`, `Try`).

---

## 9. `Unit` і побічні ефекти

```scala
def pure(x: Int): Int = x * 2          // повертає результат обчислення
def noisy(x: Int): Unit = println(x)  // Unit ≈ void; зазвичай side effect
```

Чисті функції повертають **дані**, не `Unit`. Логування й `println` — на краю програми ([p02](p02_javish_donation_jar.md), `DonationApp`).

---

## 10. Залізничний розподіл за один прохід (`partitionMap`)

Коли потрібно розділити колекцію на **успішні дані** та **помилки**:

```scala
val rawItems = List("10", "abc", "25", "bad")

// Функція валідації повертає Either[Error, Value]:
def parseNumber(s: String): Either[String, Int] =
  s.toIntOption.toRight(s"Не число: '$s'")

// ❌ Антипатерн (2 проходи по пам'яті + ручне розпакування):
// val fails = rawItems.map(parseNumber).filter(_.isLeft)
// val goods = rawItems.map(parseNumber).filter(_.isRight)

// ✅ Ідіоматично (1 прохід O(N), повертає кортеж двох розпакованих списків):
val (errors, numbers) = rawItems.partitionMap(parseNumber)
// errors  = List("Не число: 'abc'", "Не число: 'bad'")
// numbers = List(10, 25)
```

Детальна теорія — у [Лекції 03](03_railway_oriented_programming.md) та практиках [p03](p03_transaction_pipeline.md) / [p04](p04_heart_disease_triage.md).

---

## Куди далі

| Потреба | Куди |
| ------- | ---- |
| Паралелізм, race condition | [p00](p00_parallelism.md) |
| Java-ish vs чисте ФП, shell | [p02](p02_javish_donation_jar.md) |
| Railway-Oriented та `partitionMap` | [p03](p03_transaction_pipeline.md), [Лекція 03](03_railway_oriented_programming.md) |
| Immutability, structural sharing | [Лекція 01](01_immutability_and_state.md) |
| Офіційний тур по Scala | [Scala Docs](https://docs.scala-lang.org/tour/tour-of-scala.html) |
