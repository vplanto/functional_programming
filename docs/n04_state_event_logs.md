# Самостійна робота: Журнали подій (CSV) та імпорт через Railway-Oriented Programming

> **Декларація курсу.** Академічна доброчесність та авторство матеріалів — у [DISCLAIMER.md](DISCLAIMER.md).

💡 **Місток курсу:** У [Варіантах завдань для моделювання стану](n03_state_modeling_tasks.md) ви проєктуєте **домен і стан** для одного з 35 бізнес-сценаріїв. У [Практиці 04: Залізничний платіжний конвеєр](p04_transaction_pipeline.md) та [Лекції 03](03_railway_oriented_programming.md) ви вчитеся будувати **відмовостійкий парсер** на `Either`. У **цій роботі** ці дві лінії зустрічаються: ви **імпортуєте зовнішній CSV-журнал** у типізовані події (ROP), **згортаєте** їх у стан і виконуєте **розрахунки та інваріанти** з вашого варіанта з переліку моделювання стану.

---

## Про завдання

Мета практикуму — реалізувати для **свого** варіанта (1…35) повний цикл:

1. **Генерація** великого журналу подій у форматі курсу (контракт журналів подій).
2. **Імпорт** рядків через залізнично орієнтоване програмування (без `throw` для бізнес-помилок валідації).
3. **Обчислення стану** та аналітики згідно з [Варіантами завдань для моделювання стану](n03_state_modeling_tasks.md).
4. **Автоматичні тести** (`ScalaTest`), включно з граничними випадками.

* **Вибір варіанта:** той самий номер, що в [Варіантах завдань для моделювання стану](n03_state_modeling_tasks.md) і в таблиці закріплення групи (Google Sheet від викладача).
* **Свобода реалізації:** пакети, імена типів, розбиття на модулі — на ваш розсуд; окремий пакет під номер варіанта бажаний.
* **Критерій готовності:** коректний парсинг і згортання журналу, інваріант домену, тести проходять (`sbt test`).

Опис домену (легенда, людські назви подій, розрахунки та інваріанти) — у [Варіантах завдань для моделювання стану](n03_state_modeling_tasks.md). Єдиний **зовнішній** формат журналу — CSV з кодами `event` і колонками `p1`…`p4`; канонічна специфікація кодів і полів для кожного варіанта — у каталозі `n04_state_event_logs/contract/` (див. нижче), а не дублюється тут по всіх 35 сценаріях.

---

## Репозиторій курсу: модуль `n04_state_event_logs`

| Шлях | Призначення |
|------|-------------|
| `n04_state_event_logs/contract/n04_contract.json` | Загальні правила CSV, примітивні типи, стратегії «брудних» рядків |
| `n04_state_event_logs/contract/n04_variants.draft.json` | 35 варіантів: коди подій, поля, `range`, `constants`, ключ інваріанту домену |
| `n04_state_event_logs/scripts/generate_event_logs.py` | Генератор CSV і manifest |
| `n04_state_event_logs/generated/` | Каталог для **вашого** згенерованого файлу (у git не комітиться, крім `.gitkeep`) |

Варіанти 1…35 у контракті відповідають тим самим сценаріям, що в [Варіантах завдань для моделювання стану](n03_state_modeling_tasks.md).

---

## Генерація журналу (обов’язково)

Журнал **не беруть** з чужого репозиторію — **генерують локально** після клону курсу:

```bash
cd n04_state_event_logs/scripts
python3 generate_event_logs.py --variant N --rows 500000
```

* `N` — номер варіанта (1…35).
* Результат: `n04_state_event_logs/generated/variant_NN_<slug>.csv`  
  (`NN` — два символи: `01`, `23`, …; `<slug>` — латинська мітка з контракту, див. таблицю нижче).
* Поруч з’являється `variant_NN_<slug>.manifest.json` (статистика помилок, seed) — для звіту необов’язковий, але корисний при налагодженні.

**Приклад:** варіант 23 → `variant_23_home_solar_battery.csv`, slug `home_solar_battery`.

У власному репозиторії для здачі додайте CSV у `resources/` / `testdata/` **або** чітко опишіть у README команду генерації та очікуване ім’я файлу.

---

## Формат CSV

Перший рядок — заголовок (завжди):

```text
event,p1,p2,p3,p4
```

* **`event`** — код події з контракту (`TOPUP`, `RIDE`, `GENERATE`, …).
* **`p1`…`p4`** — універсальні слоти параметрів; для кожного варіанта використовується лише частина (решта **порожня** — це норма).
* Роздільник — кома; кодування UTF-8.

Семантика полів (гроші, kWh, хвилини, токени) задається в `fields` відповідного варіанта в `n04_variants.draft.json`. Для валідних рядків генератор дотримується `range` у полях (реалістичні діапазони).

### Навмисно «брудні» рядки (~5%)

Генератор змішує коректні події з помилками (невідомий `event`, биті числа, обрізані колонки тощо). Це **частина завдання**: парсер має відносити такі рядки до **лівої рейки** `Either`, не ламаючи обробку валідних рядків.

### Підводний камінь парсера в Scala

`line.split(",")` **відкидає хвостові порожні поля**. Рядок `TOPUP,156.88,,,` може перетворитися на 2 колонки замість п’яти.

**Рекомендація:** `line.split(",", -1)` або доповнення масиву до 5 полів після `event`.

---

## Архітектура рішення (конспект)

### 1. Домен (ADT)

* **Події** — `enum` / sealed trait, відповідність кодам з CSV.
* **Стан** — незмінна структура після кожного кроку `fold`.
* **Помилки** — окремі типи для парсингу (`MalformedRow`, `UnknownEvent`, `InvalidAmount`, …) і для бізнес-правил (овердрафт, перевищення ліміту), за потреби.

Бізнес-легенда з завдання моделювання стану має відображатися в **іменах** і **поведінці** типів, а не в «магічних» рядках.

### 2. Smart constructors

Кожне поле з контракту (наприклад `money_positive`, `kcal_int`, `token`) парситься через функцію виду:

`String => Either[DomainError, T]`

Невалідні значення **не потрапляють** у подію.

### 3. Railway-парсер рядка

```text
(rawLine, rowIndex) => Either[ParseError, DomainEvent]
```

* Спочатку синтаксис: кількість колонок, відомий `event`.
* Далі — валідація полів за схемою **вашого** `id` у `n04_variants.draft.json`.
* Композиція через `for`-comprehension / `flatMap` на `Either` ([Лекція 03](03_railway_oriented_programming.md), [Практика 04](p04_transaction_pipeline.md)).

Пакетна обробка: `partitionMap` (або два проходи) → список помилок і список подій.

### 4. Згортання журналу

```text
events.foldLeft(initialState)(applyEvent)
```

`applyEvent` повертає `Either[BusinessError, State]` якщо подія **заборонена** інваріантом (наприклад поїздка без балансу); інакше — новий стан.

Ключ інваріанту домену в JSON контракту (`n03Invariant`) підказує, яке правило обов’язково покрити тестом.

### 5. Аналітика

Усі пункти «Що має вміти система» з **вашого** варіанта в переліку моделювання стану реалізуються як **чисті функції** від фінального (або проміжного) стану / журналу валідних подій.

---

## Приклад зчитування контракту (варіант 1)

**Домен (варіант 1):** гаманець велопрокату (баланс, поїздки, промо, компенсація).

**Коди в CSV:**

| `event` | Колонки | Зміст |
|---------|---------|--------|
| `TOPUP` | p1 | сума поповнення |
| `RIDE` | p1, p2 | хвилини, вартість поїздки |
| `PROMO` | p1 | промо-бонус |
| `COMPENSATION` | p1 | компенсація |

**Інваріант у контракті:** `forbid_ride_if_insufficient_balance`.

Повні типи полів і `range` — у блоці `"id": 1` файлу `n04_variants.draft.json`.

---

## Довідник: варіант → slug → ім’я CSV

Легенди та розрахунки — у [Варіантах завдань для моделювання стану](n03_state_modeling_tasks.md).  
Коди подій і поля — у `n04_variants.draft.json` (блок `"id": N`).

| № | slug | Типовий файл після генерації |
|---|------|------------------------------|
| 1 | `bike_share_wallet` | `variant_01_bike_share_wallet.csv` |
| 2 | `cafe_loyalty_card` | `variant_02_cafe_loyalty_card.csv` |
| 3 | `crypto_wallet_fees` | `variant_03_crypto_wallet_fees.csv` |
| 4 | `split_bill_trip` | `variant_04_split_bill_trip.csv` |
| 5 | `bank_cashback_account` | `variant_05_bank_cashback_account.csv` |
| 6 | `gift_certificate` | `variant_06_gift_certificate.csv` |
| 7 | `foodtruck_cash_register` | `variant_07_foodtruck_cash_register.csv` |
| 8 | `parcel_locker` | `variant_08_parcel_locker.csv` |
| 9 | `warehouse_pallets` | `variant_09_warehouse_pallets.csv` |
| 10 | `fleet_fuel_card` | `variant_10_fleet_fuel_card.csv` |
| 11 | `cargo_elevator` | `variant_11_cargo_elevator.csv` |
| 12 | `parking_lot` | `variant_12_parking_lot.csv` |
| 13 | `scooter_hub` | `variant_13_scooter_hub.csv` |
| 14 | `aircraft_baggage` | `variant_14_aircraft_baggage.csv` |
| 15 | `rpg_wallet` | `variant_15_rpg_wallet.csv` |
| 16 | `battle_pass` | `variant_16_battle_pass.csv` |
| 17 | `stamina` | `variant_17_stamina.csv` |
| 18 | `auction_house` | `variant_18_auction_house.csv` |
| 19 | `mmr_leaderboard` | `variant_19_mmr_leaderboard.csv` |
| 20 | `guild_treasury` | `variant_20_guild_treasury.csv` |
| 21 | `mobile_dual_currency` | `variant_21_mobile_dual_currency.csv` |
| 22 | `electricity_meter` | `variant_22_electricity_meter.csv` |
| 23 | `home_solar_battery` | `variant_23_home_solar_battery.csv` |
| 24 | `water_filtration` | `variant_24_water_filtration.csv` |
| 25 | `greenhouse_irrigation` | `variant_25_greenhouse_irrigation.csv` |
| 26 | `server_room_hvac` | `variant_26_server_room_hvac.csv` |
| 27 | `coffee_vending` | `variant_27_coffee_vending.csv` |
| 28 | `ev_charging` | `variant_28_ev_charging.csv` |
| 29 | `mobile_plan` | `variant_29_mobile_plan.csv` |
| 30 | `cloud_drive` | `variant_30_cloud_drive.csv` |
| 31 | `audiobook_credits` | `variant_31_audiobook_credits.csv` |
| 32 | `fitness_calories` | `variant_32_fitness_calories.csv` |
| 33 | `gym_membership` | `variant_33_gym_membership.csv` |
| 34 | `ects_tracker` | `variant_34_ects_tracker.csv` |
| 35 | `freelance_time` | `variant_35_freelance_time.csv` |

**Slug** — коротка латинська назва варіанта в контракті; входить у ім’я файлу, щоб однозначно зв’язати журнал із доменом.

---

## Тести (мінімальний набір)

1. **Короткий сценарій** — кілька рядків CSV або подій напряму → очікуваний стан і одна метрика з вашого варіанта моделювання стану.
2. **Невалідний рядок** — `Left`, стан не змінюється (або лишається попереднім за вашою моделлю).
3. **Заборонена операція** — валідний парсинг, але `applyEvent` повертає помилку інваріанту.
4. **Фрагмент великого журналу** — переконатися, що парсер обробляє файл з ~5% відхилених рядків без винятків.

---

## Як здавати роботу

1. Публічний **GitHub-репозиторій** з кодом і тестами.
2. У коментарі до здачі (Classroom):
   * номер варіанта та **назва з переліку моделювання стану** (українською);
   * **slug** і **ім’я згенерованого CSV** (наприклад: 23, `home_solar_battery`, `variant_23_home_solar_battery.csv`);
   * рядок з консолі після `sbt test`: `[info] All tests passed`.

---

## Корисні посилання

* [Варіанти завдань для моделювання стану](n03_state_modeling_tasks.md)
* [Практика 04: Залізничний платіжний конвеєр (еталон парсера)](p04_transaction_pipeline.md)
* [Лекція 03. Railway-Oriented Programming (теорія)](03_railway_oriented_programming.md)
* [Лекція 04. ADT і моделювання домену](04_algebraic_data_types.md)
