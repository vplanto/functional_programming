# Програма самостійної роботи (СРС) та рекомендовані курси

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів — у [DISCLAIMER.md](DISCLAIMER.md).

Згідно з навчальним планом дисципліни загальний обсяг становить **90 академічних годин**:

| Вид навантаження | Годин | Пар (аудиторні) |
| :--- | :---: | :---: |
| Лекції | **14** | 7 |
| Практичні заняття | **16** | 8 |
| Самостійна робота (СРС) | **60** | — |
| **Разом** | **90** | 15 |

Обсяг самостійної роботи здобувача — **60 академічних годин** (8 тем по 7–8 годин; див. таблицю нижче).

Самостійна робота розподілена між двома взаємопов'язаними напрямами:

1. **Інженерний напрям (30 годин):** реалізація та еволюція наскрізного проєкту «Cyber Tunnel Runner» — безстанового MISD-автопілота з backpressure (детальні вимоги — у [n01_cyber_tunnel_runner.md](n01_cyber_tunnel_runner.md)).
2. **Теоретично-сертифікаційний напрям (30 годин):** поглиблене опанування Scala, функціональних абстракцій та реактивних систем за програмами міжнародних відкритих курсів EPFL/Coursera та документації індустріальних фреймворків.

---

## 1. Структура 60 годин самостійної роботи за темами курсу

Навчальна програма передбачає **8 тем для самостійного опрацювання (разом 60 годин; орієнтовно 7–8 годин на тему)** — по одній на кожен навчальний тиждень. Кожна тема інтегрує теоретичне опрацювання лекційного матеріалу, виконання інженерного завдання проєкту та проходження модулів рекомендованих курсів.

| № | Тема робочої програми (СРС) | Годин | Рекомендований онлайн-модуль / Сертифікаційний трек | Форма звітності та результат |
| :---: | :--- | :---: | :--- | :--- |
| **1** | **Тема 1.** Незмінність (Immutability), Structural Sharing та ціна GC у JVM. Парадокс «копіювання» проти мутації. | **8** | Coursera (EPFL): *Functional Programming Principles in Scala* — Week 1–2 (вирази, рекурсія, незмінні колекції). | [p03: Банка віляє донатами](p03_javish_donation_jar.md): рефакторинг `DonationJar` з `var` на чисті функції. Порівняльний звіт про алокації. |
| **2** | **Тема 2.** Функції вищого порядку, згортка (`fold`/`reduce`), хвостова рекурсія та паралелізм колекцій. | **7** | Coursera (EPFL): *Functional Programming Principles in Scala* — Week 3–4 + *Parallel Programming* — Week 1 (work stealing, `par`). | [p01: Паралелізм](p01_parallelism.md): benchmark послідовного vs `par` на фільтрації ризиків. Звіт про speedup і overhead. |
| **3** | **Тема 3.** Залізничне програмування (Railway-Oriented): `Option`, `Either`, `Try` як контейнери обчислювальних ефектів. | **8** | Coursera (EPFL): *Functional Program Design in Scala* — Week 1–2 (for-comprehensions, `Either`, property-based testing). | [p04: Платіжний конвеєр](p04_transaction_pipeline.md): пайплайн валідації без `null` і `throw`. Покриття гілок помилок у звіті. |
| **4** | **Тема 4.** Алгебраїчні типи даних (ADT), Pattern Matching, Smart Constructors та моделювання домену. | **7** | Coursera (EPFL): *Functional Program Design in Scala* — Week 3–4 (типи-суми, інваріанти) + [Scala 3 Book](https://docs.scala-lang.org/scala3/book/domain-modeling.html) (Domain Modeling, ADT). | [p05: Медичний консиліум](p05_heart_disease_triage.md): MISD-арбітр на ADT. У проєкті `23_runner`: доменні `enum` для `Lane`, `Action`, `CellItem`. |
| **5** | **Тема 5.** Асинхронність: криза Thread-per-Request, анатомія `Future`, `ExecutionContext`, неблокуючий I/O. | **8** | Coursera (EPFL): *Principles of Reactive Programming* — Week 1–2 (асинхронні обчислення, callback hell → `Future`). | У `23_runner`: винесення `bot.decide` у виділений `ExecutionContext`. Звіт про відсоток `Timeout` у режимі `obstacles`. |
| **6** | **Тема 6.** Реактивні потоки, Backpressure, Reactive Streams та траблшутинг розподілених збоїв. | **7** | Coursera (EPFL): *Principles of Reactive Programming* — Week 3–4 (Reactive Streams, backpressure) + [Reactive Streams Specification](https://www.reactive-streams.org/). | Режим `--mode=benchmark` у [n01](n01_cyber_tunnel_runner.md): аналіз fallback `KeepRunning` при експоненційному прискоренні тіків. Графік `Timeouts %` vs дистанція. |
| **7** | **Тема 7.** Модель акторів, Event Sourcing, MISD-таксономія Фліна та мажоритарний арбітр. | **8** | Lightbend / Apache Pekko: *Akka Typed Essentials* + [Akka Documentation: Actor Model](https://doc.akka.io/libraries/akka-core/current/typed/index.html). | Тріада ботів (Reflex / Greedy / Strategic) + [p06: Анатомія Engine](p06_engine_architecture.md). Демо replay у [viewer](viewer/index.html). |
| **8** | **Тема 8.** Functional Core / Imperative Shell, детермінованість, підготовка до іспиту та фінальний бенчмарк. | **7** | Coursera: *Programming Languages, Part A* (UW) — Week 1–2 (SML, лямбда-числення) **або** [Scala 3 Book](https://docs.scala-lang.org/scala3/book/fp-modeling.html) (FP Modeling, pure functions). | Фінальний забіг: `sbt "run --bot=MyBot --mode=benchmark --seed=2026 --out=final_run.json"`. Архітектурна співбесіда до іспиту. |
| | **РАЗОМ** | **60** | | |

---

## 2. Рекомендовані міжнародні онлайн-курси та треки

Для виконання теоретичної та практичної частини самостійної роботи студентам пропонуються відкриті онлайн-курси від EPFL, провідних університетів та професійних консорціумів. Курс багато в чому сформовано під впливом програм Мартіна Одерскі — саме їх ми рекомендуємо як базовий трек.

### Трек A. Функціональне програмування на Scala (EPFL / Coursera)

* **Functional Programming in Scala Specialization** — *École polytechnique fédérale de Lausanne (Coursera)*
  - **Ключові теми:** Рекурсія та незмінність, функції вищого порядку, for-comprehensions, типи-суми, паралельні колекції, Spark (оглядово).
  - **Корисність для курсу:** Пряма підтримка Лекцій 01–04 та практик p00–p04. Дає математичну строгість без абстрактної теорії категорій.
  - **Посилання:** [Coursera: Functional Programming in Scala Specialization](https://www.coursera.org/specializations/scala)

* **The Scala 3 Book** — *Scala Center (офіційна документація, безкоштовно)*
  - **Ключові теми:** Scala 3 syntax, `enum`, контекстні параметри (`given`/`using`), opaque types, algebraic data types, domain modeling.
  - **Корисність для курсу:** Актуалізує синтаксис під стек курсу (Scala 3 + sbt). Корисний для [p02: Шаблони Scala](p02_scala_templates.md) та ADT у `23_runner`.
  - **Посилання:** [Scala Documentation: The Scala 3 Book](https://docs.scala-lang.org/scala3/book/introduction.html)

### Трек B. Реактивне програмування та асинхронність

* **Principles of Reactive Programming** — *EPFL (Coursera)*
  - **Ключові теми:** `Future` та `Promise`, Reactive Streams, backpressure, Rx-оператори, інтеграція з Akka (історичний контекст).
  - **Корисність для курсу:** Теоретична база для Лекцій 05–06 та режиму `benchmark` у Cyber Tunnel Runner, де час на рішення стискається до мікросекунд.
  - **Посилання:** [Coursera: Principles of Reactive Programming](https://www.coursera.org/learn/reactive)

* **Akka / Apache Pekko Documentation & Tutorials** — *Lightbend / Apache Software Foundation*
  - **Ключові теми:** Модель акторів, ізоляція стану, mailbox, supervision, typed actors, event-driven persistence (оглядово).
  - **Корисність для курсу:** Підготовка до Лекції 07 та архітектури MISD-консиліуму ботів. Pekko — форк Akka після зміни ліцензії; концепції ідентичні.
  - **Посилання:** [Apache Pekko Documentation](https://pekko.apache.org/docs/pekko/current/) та [Akka Documentation](https://doc.akka.io/)

### Трек C. Теоретичні основи функціонального мислення

* **Programming Languages, Part A** — *University of Washington (Coursera)*
  - **Ключові теми:** Стандартна ML (SML), лямбда-числення, рекурсія, замикання, типізація, семантика функцій.
  - **Корисність для курсу:** Дає «чисту» математичну базу для студентів 113 спеціальності. Допомагає на іспиті пояснювити *чому* `flatMap` — це монадичне зв'язування, а не «магічний синтаксис Scala».
  - **Посилання:** [Coursera: Programming Languages, Part A](https://www.coursera.org/learn/programming-languages)

* **Structure and Interpretation of Computer Programs (SICP)** — *MIT OpenCourseWare*
  - **Ключові теми:** Абстракція даних, метalinguistic abstraction, потоки (streams) як нескінченні структури, інтерпретатори.
  - **Корисність для курсу:** Історичний фундамент ФП. Корисний для глибокого розуміння Лекції 02 (згортки, потоки) та Лекції 06 (нескінченні подійні потоки).
  - **Посилання:** [MIT OCW: SICP](https://ocw.mit.edu/courses/6-001-structure-and-interpretation-of-computer-programs-spring-2005/)

### Трек D. Інженерія реактивних систем (прикладний рівень)

* **Rock the JVM — Scala & Functional Programming** — *Daniel Ciocîrlan (free YouTube / paid Udemy)*
  - **Ключові теми:** Прагматичний Scala 3, Cats (оглядово), Akka Streams, Kafka-інтеграція, production patterns.
  - **Корисність для курсу:** Швидкий «місток» від академічного курсу до індустріальних вакансій Scala-розробника. Добре доповнює [p06](p06_engine_architecture.md).
  - **Посилання:** [Rock the JVM YouTube Channel](https://www.youtube.com/rockthejvm)

* **Reactive Architecture Foundations** — *IBM Cognitive Class (безкоштовно; матеріали Lightbend)*
  - **Ключові теми:** Reactive Manifesto, reactive microservices, Domain-Driven Design, resilience, elasticity, message-driven architecture.
  - **Корисність для курсу:** Архітектурна рамка для фінальної лекції та іспитової співбесіди про Event Sourcing у Лекції 07.
  - **Посилання:** [IBM Cognitive Class: Reactive Architecture Foundations](https://cognitiveclass.ai/learn/reactive-architecture-foundations)

---

## 3. Визнання результатів неформальної освіти (Перезарахування)

Згідно з робочою програмою дисципліни, здобувач може зарахувати проходження онлайн-матеріалів за простою формулою:

> **Формула зарахування:** **1 тема СРС = 1 завершений тижневий модуль курсу** (або практичний квест/бейдж). Загальний ліміт перезарахування за неформальну освіту — **до 20 балів максимум**.

1. **Що зараховується:**
   - Будь-який **один тиждень (Week/Module)** або окремий короткий курс із рекомендованих платформ (Coursera, edX, Lightbend, IBM SkillsBuild тощо).
   - Не потрібно проходити всю спеціалізацію — достатньо закрити конкретний тематичний блок (наприклад, 1 модуль по `Either`, 1 лабу по Akka Typed чи 1 тиждень Parallel Programming).
   - Наявність індустріального сертифіката (Lightbend Akka, Databricks Scala тощо) автоматично закриває максимум — **20 балів**.

2. **Як підтвердити:**
   - Посилання на цифровий сертифікат, публічний профіль (бейдж) або скріншот успішно складеного тесту/лабораторної з платформи.
   - Короткий коментар (3–5 хв на парі): як пройдений матеріал пов'язаний із вашим кодом у проєкті Cyber Tunnel Runner або практиках p01–p06.

---

## 4. Рекомендована література для самостійного опрацювання

Під час виконання самостійної роботи обов'язковим є опрацювання ключових розділів академічних монографій:

1. **Martin Odersky, Lex Spoon, Bill Venners. Programming in Scala (5th ed.).** Artima, 2024.
   - *Розділи для СРС:* Глава 2 (неперемінність), Глава 8–9 (функції вищого порядку, for-comprehensions), Глава 15 (case classes, pattern matching), Глава 24–25 (`Future`, паралельні колекції).
2. **Paul Chiusano, Rúnar Bjarnason. Functional Programming in Scala.** Manning, 2014.
   - *Розділи для СРС:* Глава 2–5 (чисті функції, згортки, монада `Option`), Глава 6–8 (`Either`, `Validated`, паралельний `parMap`), Глава 14–15 (streaming, IO — оглядово).
3. **Debasish Ghosh. Functional and Reactive Domain Modeling.** Manning, 2016.
   - *Розділи для СРС:* Розділ 2 (Domain modeling з ADT), Розділ 4 (Railway-oriented business workflows), Розділ 6 (Event sourcing та CQRS).
4. **Vaughn Vernon. Reactive Messaging Patterns with the Actor Model.** Addison-Wesley, 2015.
   - *Розділи для СРС:* Розділ 2 (Actor model fundamentals), Розділ 5 (Sagas), Розділ 11 (Event-driven architecture).
5. **Reactive Manifesto** — *Jonas Bonér et al., 2013.*
   - *Завдання СРС:* Порівняти чотири риси (Responsive, Resilient, Elastic, Message Driven) з архітектурою Engine у `23_runner` та режимом `benchmark`.
