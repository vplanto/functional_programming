---
layout: default
hub: true
---

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
- [n02. Програма СРС (60 годин) та рекомендовані міжнародні курси](n02_recommended_courses.md)
- [n03. Варіанти завдань для моделювання стану](n03_state_modeling_tasks.md)
- [n04. Журнали подій (CSV) та імпорт через Railway-Oriented Programming](n04_state_event_logs.md)

### Практикуми та Лабораторні
- [p00. Практика 00: Сократівський тьютор (робота з ШІ)](p00_socratic_tutor.md)
- [p01. Практика 01: Магія паралелізму без болю](p01_parallelism.md)
- [p02. Шаблони Scala (довідник)](p02_scala_templates.md)
- [p03. Практика 03: Банка віляє донатами (java-ish Scala)](p03_javish_donation_jar.md)
- [p04. Практика 04: Залізничний платіжний конвеєр (Railway-Oriented Pipeline)](p04_transaction_pipeline.md)
- [p05. Практика 05: Медичний консиліум (MISD Pipeline на Cleveland Heart Disease)](p05_heart_disease_triage.md)
- [p06. Практика 06: Анатомія рушія та ботів (Розбір Engine у Cyber Tunnel Runner)](p06_engine_architecture.md)

### Модулі репозиторію коду
- [`00_hello_scala`](https://github.com/vplanto/functional_programming/tree/main/00_hello_scala) — стартовий шаблон і налаштування середовища ([n00](n00_env_and_git.md)).
- [`01_parallelism`](https://github.com/vplanto/functional_programming/tree/main/01_parallelism) — паралельні колекції та бенчмарки ([p01](p01_parallelism.md)).
- [`02_javish`](https://github.com/vplanto/functional_programming/tree/main/02_javish) — банка донатів: перехід від Java-стилю до ідіоматичної Scala ([p03](p03_javish_donation_jar.md)).
- [`03_transaction_pipeline`](https://github.com/vplanto/functional_programming/tree/main/03_transaction_pipeline) — залізничний платіжний конвеєр на `Either` ([p04](p04_transaction_pipeline.md)).
- [`04_heart_triage`](https://github.com/vplanto/functional_programming/tree/main/04_heart_triage) — кардіологічний MISD-консиліум ([p05](p05_heart_disease_triage.md)).
- [`23_runner`](https://github.com/vplanto/functional_programming/tree/main/23_runner) — наскрізний рушій «Cyber Tunnel Runner» ([n01](n01_cyber_tunnel_runner.md), [p06](p06_engine_architecture.md), [Лекція 07](07_actors_event_sourcing_and_exam.md)).
- [`n04_state_event_logs`](https://github.com/vplanto/functional_programming/tree/main/n04_state_event_logs) — контракти CSV, генератор журналів подій та валідація ([n04](n04_state_event_logs.md), [n03](n03_state_modeling_tasks.md)).

---

**Правила гри:**
1. **Лекції — це не начитка синтаксису**, а обговорення архітектури та інженерних компромісів.
2. **Практика — це основа:** подолання звички до `var` і мутацій на користь чистоти.
3. **Головне питання — «Як це працює?»:** згенерувати код через ШІ дозволено, але нездатність пояснити роботу під капотом — це провал (деталі у [p00. Сократівський тьютор](p00_socratic_tutor.md)).
