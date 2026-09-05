# Навчальний курс: Прикладне функціональне програмування та реактивні системи

Ласкаво просимо до курсу! Цей курс розроблено для того, щоб перевернути ваше уявлення про програмування та підготувати до створення високонавантажених, безстанових (stateless) і реактивних мікросервісів. 

Курс багато в чому сформовано під впливом класичних курсів Мартіна Одерскі: "Введення в реактивне програмування" та "Принципи реактивного програмування".

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів — у [DISCLAIMER.md](DISCLAIMER.md).

## 📖 Основні документи
- [Наскрізна ідеологія курсу](ideology.md) — філософія, місток від ООП до ФП.
- [Структура та цілі курсу](course_structure.md) — мета, стек технологій та організація навчального процесу.

## 📚 Матеріали курсу

### Лекції
- [00. Перший контакт, доброчесність та ШІ](00_intro_and_ai.md)
- [01. Основи ФП, Незмінність (Immutability) та парадокс "копіювання"](01_immutability_and_state.md)
- [02. Функції як дані: Смерть циклу `for` та анатомія згортки (`fold`)](02_first_class_functions.md)
- [03. Залізничне програмування (Railway-Oriented): Смерть `null` та `Exceptions`](03_railway_oriented_programming.md)
- [04. Моделювання предметної області: Алгебраїчні типи даних (ADT) та Pattern Matching](04_algebraic_data_types.md)
- [05. Від синхронного I/O до асинхронності: криза Thread-per-Request, ефекти та анатомія Future](05_async_futures_and_non_blocking.md)
- [06. Реактивні потоки, Backpressure та траблшутинг: чому реактивність важко дебажити](06_reactive_streams_and_troubleshooting.md)
- [07. Фінал: Модель Акторів (Pekko/Akka), патерн Functional Core / Imperative Shell та путівник до іспиту](07_actors_event_sourcing_and_exam.md)

### Самостійна робота
- [n00. Налаштування середовища та Git](n00_env_and_git.md)
- [n01. Наскрізний проєкт «Cyber Tunnel Runner»](n01_cyber_tunnel_runner.md)

### Практикуми та Лабораторні
- [p00. Практика 00: Магія паралелізму без болю](p00_parallelism.md)
- [p01. Практика 01: Шаблони Scala (довідник)](p01_scala_templates.md)
- [p02. Практика 02: Банка віляє донатами (java-ish Scala)](p02_javish_donation_jar.md)
- [p03. Практика 03: Залізничний платіжний конвеєр (Railway-Oriented Pipeline)](p03_transaction_pipeline.md)
- [p04. Практика 04: Медичний консиліум (MISD Pipeline на Cleveland Heart Disease)](p04_heart_disease_triage.md)
- [p05. Практика 05: Анатомія рушія та ботів (Розбір Engine у Cyber Tunnel Runner)](p05_engine_architecture.md)

---
**Правила гри:**
