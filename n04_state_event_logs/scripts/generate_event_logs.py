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
        length = rng.randint(4, 12)
        alphabet = string.ascii_uppercase + string.digits
        return "".join(rng.choice(alphabet) for _ in range(length))

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


def apply_field_error(
    rng: random.Random,
    cells: list[str],
    event: dict,
    type_strategies: dict[str, list[str]],
    error_strategies: dict,
) -> str:
    fields = event.get("fields") or []
    if not fields:
        return "empty_event"
    field = rng.choice(fields)
    idx = COL_INDEX[field["col"]]
    tname = field["type"]
    candidates = type_strategies.get(tname, [])
    if not candidates:
        return "type_token_invalid"
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

    error_counts: Counter[str] = Counter()
    out_path.parent.mkdir(parents=True, exist_ok=True)

    with out_path.open("w", encoding=csv_spec.get("encoding", "utf-8"), newline="\n") as f:
        f.write(delimiter.join(csv_spec["header"]) + "\n")
        for _ in range(rows):
            event = pick_event(rng, variant)
            cells = build_valid_line(rng, event, types)
            line = format_line(event["code"], cells, delimiter)
            strategy_used = "valid"

            if rng.random() < error_rate:
                kind = choose_error_kind(rng, profile)
                if kind == "field_type_errors":
                    strategy_used = apply_field_error(
                        rng, cells, event, type_strategies, error_strategies
                    )
                    line = format_line(event["code"], cells, delimiter)
                else:
                    strategy_used = kind
                    line = inject_syntax_error(rng, line, kind, delimiter)

            error_counts[strategy_used] += 1
            f.write(line + "\n")

    manifest = {
        "variantId": variant["id"],
        "slug": variant["slug"],
        "rows": rows,
        "seed": seed if seed is not None else variant_seed(variant["id"], contract["generationDefaults"]["seedOffset"]),
        "errorRate": error_rate,
        "rowOutcomes": dict(error_counts),
        "csv": str(out_path.relative_to(ROOT)),
    }
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Generate n04 event log CSV files.")
    p.add_argument("--variant", type=int, help="Variant id 1..35")
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
