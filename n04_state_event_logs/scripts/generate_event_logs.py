#!/usr/bin/env python3
"""
Генерація CSV-журналів подій для n04 за контрактом у contract/.

Приклад:
  python3 generate_event_logs.py --variant 1 --rows 1000
  python3 generate_event_logs.py --all --rows 100000
"""
from __future__ import annotations

import argparse
import json
import random
import string
from collections import Counter
from collections.abc import Callable
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTRACT_DIR = ROOT / "contract"
DEFAULT_OUT = ROOT / "generated"

COLS = ("p1", "p2", "p3", "p4")
COL_INDEX = {name: i for i, name in enumerate(COLS)}


def load_contract() -> dict:
    contract = json.loads((CONTRACT_DIR / "n04_contract.json").read_text(encoding="utf-8"))
    variants_path = CONTRACT_DIR / "n04_variants.json"
    if not variants_path.is_file():
        variants_path = CONTRACT_DIR / "n04_variants.draft.json"
    if not variants_path.is_file():
        raise FileNotFoundError("contract/n04_variants.json (або n04_variants.draft.json)")
    variants = json.loads(variants_path.read_text(encoding="utf-8"))
    contract["variants"] = variants
    return contract


def build_type_strategy_map(error_strategies: dict) -> dict[str, list[str]]:
    mapping: dict[str, list[str]] = {}
    for name, spec in error_strategies.items():
        if spec.get("layer") != "parse":
            continue
        for t in spec.get("appliesToTypes", []):
            mapping.setdefault(t, []).append(name)
    return mapping


def qdecimal(value: Decimal, scale: int) -> str:
    exp = Decimal("1").scaleb(-scale)
    return str(value.quantize(exp, rounding=ROUND_HALF_UP))


def _int_bounds(field: dict, spec: dict) -> tuple[int, int]:
    override = field.get("range") or {}
    if "minExclusive" in spec and "min" not in override:
        lo = int(spec["minExclusive"]) + 1
    else:
        lo = int(override.get("min", spec.get("min", 0)))
    hi = int(override.get("max", spec.get("max", 10_000)))
    if lo > hi:
        lo, hi = hi, lo
    return lo, hi


def _decimal_bounds(field: dict, spec: dict, scale: int) -> tuple[Decimal, Decimal]:
    override = field.get("range") or {}
    if "minExclusive" in spec and "min" not in override:
        lo = Decimal(spec["minExclusive"]) + Decimal(1).scaleb(-scale)
    else:
        lo = Decimal(str(override.get("min", spec.get("min", "0"))))
    hi = Decimal(str(override.get("max", spec.get("max", "9999"))))
    if lo > hi:
        lo, hi = hi, lo
    return lo, hi


def _random_token(rng: random.Random) -> str:
    length = rng.randint(4, 12)
    alphabet = string.ascii_uppercase + string.digits
    return "".join(rng.choice(alphabet) for _ in range(length))


def attach_vocabularies(variant: dict) -> None:
    """
    Поле з "vocab": "<name>" бере значення зі словника variant["vocabularies"][name]:
    явний список "values" або "size" згенерованих токенів (логіни, коди товарів),
    щоб ті самі ідентифікатори повторювались у журналі.
    """
    vocabs: dict[str, list[str]] = {}
    for name, spec in (variant.get("vocabularies") or {}).items():
        if "values" in spec:
            vocabs[name] = list(spec["values"])
            continue
        vrng = random.Random(f"{variant['id']}:{name}")
        words: dict[str, None] = {}
        while len(words) < spec["size"]:
            words[_random_token(vrng)] = None
        vocabs[name] = list(words)
    for event in variant["events"]:
        for field in event.get("fields", []):
            if "vocab" in field:
                field["_vocab"] = vocabs[field["vocab"]]


def gen_valid_value(rng: random.Random, field: dict, types: dict) -> str:
    tname = field["type"]
    spec = types[tname]
    parse = spec["parse"]

    if parse == "enum":
        allowed = field.get("allowed") or []
        if not allowed:
            return "UNKNOWN"
        return rng.choice(allowed)

    if parse == "string":
        if "_vocab" in field:
            return rng.choice(field["_vocab"])
        return _random_token(rng)

    if parse == "int":
        lo, hi = _int_bounds(field, spec)
        return str(rng.randint(lo, hi))

    scale = int(spec.get("scale", 2))
    lo, hi = _decimal_bounds(field, spec, scale)
    span = hi - lo
    raw = lo + span * Decimal(rng.random())
    return qdecimal(raw, scale)


def pick_event(rng: random.Random, variant: dict) -> dict:
    events = variant["events"]
    weights = variant.get("eventWeights") or {}
    codes = [e["code"] for e in events]
    w = [weights.get(c, 1.0) for c in codes]
    code = rng.choices(codes, weights=w, k=1)[0]
    return next(e for e in events if e["code"] == code)


def build_valid_line(rng: random.Random, event: dict, types: dict) -> list[str]:
    cells = ["", "", "", ""]
    for field in event.get("fields", []):
        idx = COL_INDEX[field["col"]]
        cells[idx] = gen_valid_value(rng, field, types)
    return cells


class EntityPool:
    """Живі сутності варіанта (наприклад, файли), на які посилаються наступні події."""

    def __init__(self, key_fields: list[str]):
        self.key_fields = tuple(key_fields)
        self.items: list[dict[str, str]] = []
        self.index: dict[tuple[str, ...], int] = {}

    def key_of(self, values: dict[str, str]) -> tuple[str, ...]:
        return tuple(values[f] for f in self.key_fields)

    def contains(self, values: dict[str, str]) -> bool:
        return self.key_of(values) in self.index

    def get(self, values: dict[str, str]) -> dict[str, str]:
        return self.items[self.index[self.key_of(values)]]

    def add(self, values: dict[str, str]) -> None:
        self.index[self.key_of(values)] = len(self.items)
        self.items.append(values)

    def remove(self, values: dict[str, str]) -> None:
        pos = self.index.pop(self.key_of(values))
        last = self.items.pop()
        if pos < len(self.items):
            self.items[pos] = last
            self.index[self.key_of(last)] = pos

    def pick(self, rng: random.Random, origins: list[str] | None = None) -> dict[str, str] | None:
        if not origins:
            return rng.choice(self.items)
        for _ in range(MAX_KEY_ATTEMPTS):
            item = rng.choice(self.items)
            if item[ORIGIN] in origins:
                return item
        return None


MAX_KEY_ATTEMPTS = 20
ORIGIN = "_event"


def _gen_values(rng: random.Random, fields: list[dict], types: dict) -> dict[str, str]:
    return {f["name"]: gen_valid_value(rng, f, types) for f in fields}


def _fresh_key_values(
    rng: random.Random, fields: list[dict], types: dict, pool: EntityPool
) -> dict[str, str]:
    values = _gen_values(rng, fields, types)
    for _ in range(MAX_KEY_ATTEMPTS):
        if not pool.contains(values):
            break
        values = _gen_values(rng, fields, types)
    return values


def _near_miss_values(
    rng: random.Random, fields: list[dict], types: dict, pool: EntityPool
) -> dict[str, str]:
    """Посилання на існуючу сутність, у якої перше поле ключа збережено, а інше змінено (те саме ім'я, інший тип)."""
    by_name = {f["name"]: f for f in fields}
    values = {**_gen_values(rng, fields, types), **pool.pick(rng)}
    key_field = by_name[rng.choice(pool.key_fields[1:] or pool.key_fields)]
    for _ in range(MAX_KEY_ATTEMPTS):
        values[key_field["name"]] = gen_valid_value(rng, key_field, types)
        if not pool.contains(values):
            return values
    return _fresh_key_values(rng, fields, types, pool)


def build_entity_line(
    rng: random.Random, event: dict, types: dict, pools: dict[str, EntityPool]
) -> tuple[list[str], str, Callable[[], None] | None]:
    """
    Повертає (cells, outcome, commit). commit змінює пул і викликається лише для валідного рядка:
    якщо рядок зіпсовано, парсер студента його відкине, і стан домену не зміниться.
    """
    spec = event["entity"]
    pool = pools[spec["ref"]]
    fields = event.get("fields", [])

    if spec["op"] == "create":
        duplicate_rate = float(spec.get("duplicateRate", 0))
        if duplicate_rate and pool.items and rng.random() < duplicate_rate:
            target = pool.pick(rng)
            values = {**_gen_values(rng, fields, types), **{k: target[k] for k in pool.key_fields}}
        else:
            values = _fresh_key_values(rng, fields, types, pool)
        if pool.contains(values):
            outcome, commit = "duplicate", None
        else:
            values[ORIGIN] = event["code"]
            outcome, commit = "created", (lambda: pool.add(values))
    elif spec["op"] in ("remove", "touch"):
        target = None
        if pool.items and not rng.random() < float(spec.get("danglingRate", 0)):
            values = _gen_values(rng, fields, types)
            target = pool.pick(rng, spec.get("fromEvents"))
        if target is not None:
            values = {**values, **target}
            outcome = "resolved"
        elif pool.items and rng.random() < 0.5:
            values = _near_miss_values(rng, fields, types, pool)
            outcome = "dangling_near_miss"
        else:
            values = _fresh_key_values(rng, fields, types, pool)
            outcome = "dangling_unknown"
        if outcome != "resolved" and pool.contains(values):
            values = {**values, **pool.get(values)}
            outcome = "resolved"
        commit = None
        if outcome == "resolved" and spec["op"] == "remove":
            commit = lambda: pool.remove(values)
    else:
        raise ValueError(f"Невідома операція сутності: {spec['op']}")

    cells = ["", "", "", ""]
    for field in fields:
        cells[COL_INDEX[field["col"]]] = values[field["name"]]
    return cells, outcome, commit


def apply_field_error(
    rng: random.Random,
    cells: list[str],
    event: dict,
    type_strategies: dict[str, list[str]],
    error_strategies: dict,
) -> str:
    fields = event.get("fields") or []
    field = rng.choice(fields)
    idx = COL_INDEX[field["col"]]
    tname = field["type"]
    candidates = type_strategies.get(tname, [])
    if not candidates:
        raise ValueError(f"Немає parse-стратегії помилок для типу {tname} (errorStrategies.appliesToTypes)")
    strategy_name = rng.choice(candidates)
    spec = error_strategies[strategy_name]
    cells[idx] = rng.choice(spec["mutations"])
    return strategy_name


def inject_syntax_error(
    rng: random.Random,
    line: str,
    strategy: str,
    delimiter: str,
) -> str:
    if strategy == "unknown_event":
        return line.replace(line.split(delimiter, 1)[0], "NOT_AN_EVENT", 1)
    if strategy == "empty_event":
        parts = line.split(delimiter)
        parts[0] = ""
        return delimiter.join(parts)
    if strategy == "column_count":
        kind = rng.choice(["truncate", "extra_column"])
        parts = line.split(delimiter)
        if kind == "truncate" and len(parts) > 2:
            return delimiter.join(parts[:2])
        return line + delimiter + "EXTRA"
    return line


def choose_error_kind(rng: random.Random, profile: dict) -> str:
    w = profile.get("weights", {})
    kinds = list(w.keys())
    weights = [w[k] for k in kinds]
    return rng.choices(kinds, weights=weights, k=1)[0]


def format_line(event_code: str, cells: list[str], delimiter: str) -> str:
    return delimiter.join([event_code, *cells])


def variant_seed(variant_id: int, seed_offset: int) -> int:
    return variant_id * 1_000_003 + seed_offset


def generate_file(
    contract: dict,
    variant: dict,
    rows: int,
    out_path: Path,
    manifest_path: Path,
    seed: int | None,
) -> None:
    csv_spec = contract["csv"]
    delimiter = csv_spec["delimiter"]
    types = contract["primitiveTypes"]
    error_strategies = contract["errorStrategies"]
    profile = contract.get("defaultInjectionProfile", {})
    error_rate = float(contract["generationDefaults"]["syntaxErrorRate"])

    type_strategies = build_type_strategy_map(error_strategies)
    rng = random.Random(seed if seed is not None else variant_seed(variant["id"], contract["generationDefaults"]["seedOffset"]))

    attach_vocabularies(variant)
    pools = {
        name: EntityPool(spec["key"]) for name, spec in (variant.get("entities") or {}).items()
    }

    error_counts: Counter[str] = Counter()
    entity_counts: dict[str, Counter[str]] = {}
    out_path.parent.mkdir(parents=True, exist_ok=True)

    with out_path.open("w", encoding=csv_spec.get("encoding", "utf-8"), newline="\n") as f:
        f.write(delimiter.join(csv_spec["header"]) + "\n")
        for _ in range(rows):
            event = pick_event(rng, variant)
            commit = None
            if "entity" in event:
                cells, entity_outcome, commit = build_entity_line(rng, event, types, pools)
            else:
                cells = build_valid_line(rng, event, types)
            line = format_line(event["code"], cells, delimiter)
            strategy_used = "valid"

            if rng.random() < error_rate:
                kind = choose_error_kind(rng, profile)
                if kind == "field_type_errors" and not event.get("fields"):
                    kind = "unknown_event"
                if kind == "field_type_errors":
                    strategy_used = apply_field_error(
                        rng, cells, event, type_strategies, error_strategies
                    )
                    line = format_line(event["code"], cells, delimiter)
                else:
                    strategy_used = kind
                    line = inject_syntax_error(rng, line, kind, delimiter)

            if "entity" in event and strategy_used == "valid":
                entity_counts.setdefault(event["code"], Counter())[entity_outcome] += 1
                if commit is not None:
                    commit()

            error_counts[strategy_used] += 1
            f.write(line + "\n")

    out_abs = out_path.resolve()
    manifest = {
        "variantId": variant["id"],
        "slug": variant["slug"],
        "rows": rows,
        "seed": seed if seed is not None else variant_seed(variant["id"], contract["generationDefaults"]["seedOffset"]),
        "errorRate": error_rate,
        "rowOutcomes": dict(error_counts),
        "csv": str(out_abs.relative_to(ROOT) if out_abs.is_relative_to(ROOT) else out_abs),
    }
    if pools:
        manifest["entityRefs"] = {code: dict(c) for code, c in entity_counts.items()}
        manifest["liveEntitiesAtEnd"] = {name: len(p.items) for name, p in pools.items()}
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Generate n04 event log CSV files.")
    p.add_argument("--variant", type=int, help="Variant id 1..40")
    p.add_argument("--all", action="store_true", help="Generate all variants")
    p.add_argument("--rows", type=int, default=None, help="Rows per file (default from contract)")
    p.add_argument("--seed", type=int, default=None, help="Override RNG seed")
    p.add_argument("--out-dir", type=Path, default=DEFAULT_OUT, help="Output directory")
    return p.parse_args()


def main() -> None:
    args = parse_args()
    if not args.all and args.variant is None:
        raise SystemExit("Вкажіть --variant N або --all")

    contract = load_contract()
    rows = args.rows or int(contract["generationDefaults"]["rowsPerFile"])
    variants = {v["id"]: v for v in contract["variants"]}

    targets: list[int]
    if args.all:
        targets = sorted(variants)
    else:
        if args.variant not in variants:
            raise SystemExit(f"Невідомий variant id: {args.variant}")
        targets = [args.variant]

    for vid in targets:
        v = variants[vid]
        fname = f"variant_{vid:02d}_{v['slug']}.csv"
        out_csv = args.out_dir / fname
        manifest = args.out_dir / f"variant_{vid:02d}_{v['slug']}.manifest.json"
        seed = args.seed if args.seed is not None else None
        print(f"Generating {out_csv} ({rows} rows)...")
        generate_file(contract, v, rows, out_csv, manifest, seed)
    print("Done.")


if __name__ == "__main__":
    main()
