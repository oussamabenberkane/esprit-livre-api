#!/usr/bin/env python3
"""
Generate a single idempotent SQL file that seeds the Esprit Livre prod DB
with real data extracted from WooCommerce exports.

Inputs (relative to repo root):
  data/woocommerce-products-export-2026-04-02.csv
  data/woocommerce-orders-export-2026-04-02.xlsx
  data/woocommerce-customers-export-2026-04-02.xlsx  (unused, kept for reference)

Output:
  scripts/seed.sql  -- run with:
      docker cp seed.sql espritlivre-postgres:/tmp/seed.sql
      docker exec -i espritlivre-postgres psql -U el -d el-prod-db \\
          -v ON_ERROR_STOP=1 -f /tmp/seed.sql

Design:
- Wipes book / book_pack / jhi_order / order_item / jhi_like and their joins.
- PRESERVES existing authors and tags; merges new ones by name.
- Reuses Woo order number as jhi_order.unique_id so old orders are recognizable.
- Points all covers at /media/books/cover_woo<wooId>.jpg (placeholder fallback
  handled by BookResource.getBookCover until real images are dropped in place).
- Order lines matched to Book/BookPack by canonicalized title (UGS is empty in
  the Woo export). Unmatched lines are skipped; orders with zero matched lines
  are skipped entirely.
- Whole thing runs in a single BEGIN/COMMIT transaction.
"""
from __future__ import annotations

import csv
import html
import json
import os
import re
import sys
from dataclasses import dataclass, field
from datetime import datetime, timezone, timedelta
from decimal import Decimal, InvalidOperation
from pathlib import Path

import openpyxl  # only dep beyond stdlib

REPO = Path(__file__).resolve().parent.parent
PRODUCTS_CSV = REPO / "data" / "woocommerce-products-export-2026-04-02.csv"
ORDERS_XLSX = REPO / "data" / "woocommerce-orders-export-2026-04-02.xlsx"
OUT_SQL = REPO / "scripts" / "seed.sql"

ALGIERS_TZ = timezone(timedelta(hours=1))  # Algeria: UTC+1, no DST

# Languages/categories that are actually language markers in the Woo catalog
LANGUAGE_CATEGORY_MAP = {
    "français": "FR",
    "francais": "FR",
    "anglais": "EN",
    "arabe": "AR",
}

# Categories to drop entirely (Woo's uncategorized bucket, etc.)
DROPPED_CATEGORIES = {"non classé", "non classe"}

STATUS_MAP = {
    "Terminée": "DELIVERED",
    "En cours": "CONFIRMED",
    "En attente": "PENDING",
}


# --------------------------------------------------------------------------- #
# helpers
# --------------------------------------------------------------------------- #
def sql_str(v) -> str:
    if v is None:
        return "NULL"
    s = str(v).replace("'", "''")
    return f"'{s}'"


def sql_num(v) -> str:
    if v is None or v == "":
        return "NULL"
    try:
        d = Decimal(str(v).replace(",", "."))
        return str(d)
    except (InvalidOperation, ValueError):
        return "NULL"


def sql_int(v) -> str:
    if v is None or v == "":
        return "NULL"
    try:
        return str(int(float(str(v).replace(",", "."))))
    except ValueError:
        return "NULL"


def sql_bool(v) -> str:
    return "true" if v else "false"


def strip_html(s: str) -> str:
    if not s:
        return ""
    # decode entities, drop tags
    s = html.unescape(s)
    s = re.sub(r"<[^>]+>", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s


def canonical_title(s: str) -> str:
    return re.sub(r"\s+", " ", strip_html(s or "").lower()).strip()


def contains_arabic(s: str) -> bool:
    return bool(re.search(r"[\u0600-\u06FF]", s or ""))


def parse_price(s) -> Decimal | None:
    if s is None or s == "":
        return None
    try:
        return Decimal(str(s).replace(",", "."))
    except InvalidOperation:
        return None


def normalize_phone(p: str | None) -> str | None:
    if not p:
        return None
    n = re.sub(r"[\s\-()]", "", str(p))
    if n.startswith("0") and len(n) == 10:
        return "+213" + n[1:]
    if n.startswith("+213"):
        return n
    if n.startswith("213") and len(n) == 12:
        return "+" + n
    return n


def strip_wilaya(code: str | None) -> str | None:
    if not code:
        return None
    code = str(code).strip()
    if code.upper().startswith("DZ-"):
        return code[3:]
    return code


def parse_order_date(s) -> str | None:
    if not s:
        return None
    s = str(s).strip()
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d"):
        try:
            dt = datetime.strptime(s, fmt).replace(tzinfo=ALGIERS_TZ)
            return dt.isoformat(sep=" ")
        except ValueError:
            continue
    return None


# --------------------------------------------------------------------------- #
# parsing: products
# --------------------------------------------------------------------------- #
@dataclass
class ProductRow:
    woo_id: str
    type: str  # 'simple' or 'woosb'
    raw_title: str
    clean_title: str
    description: str
    price: Decimal | None
    stock: int
    cover_url: str
    language: str  # FR / EN / AR
    category_tags: list[str] = field(default_factory=list)
    etiquette_tags: list[str] = field(default_factory=list)
    author_name: str | None = None
    woosb_child_ids: list[str] = field(default_factory=list)


def parse_tokens(s: str | None) -> list[str]:
    if not s:
        return []
    return [t.strip() for t in s.split(",") if t.strip()]


def parse_woosb_ids(raw: str | None) -> list[str]:
    if not raw:
        return []
    raw = raw.strip()
    # Format: {"nred":{"id":"1956","sku":"","qty":"1",...}, "other":{...}}
    try:
        data = json.loads(raw)
        if isinstance(data, dict):
            return [str(v.get("id")).strip() for v in data.values() if isinstance(v, dict) and v.get("id")]
    except json.JSONDecodeError:
        pass
    # Fallback: legacy "id:1956, id:1315"
    return re.findall(r"id[: ]+(\d+)", raw)


def load_products() -> list[ProductRow]:
    rows: list[ProductRow] = []
    with PRODUCTS_CSV.open(newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for r in reader:
            if str(r.get("Publié", "")).strip() not in ("1", "true", "True"):
                continue
            woo_id = r["ID"].strip()
            raw_title = (r.get("Nom") or "").strip()
            clean_title = strip_html(raw_title)
            if not clean_title:
                print(f"skip product {woo_id}: empty title", file=sys.stderr)
                continue

            description = strip_html(r.get("Description") or "")[:1000]
            price = parse_price(r.get("Tarif régulier"))
            stock = 0
            try:
                stock = int(float(str(r.get("Stock") or "0").replace(",", ".")))
            except ValueError:
                stock = 0

            img_field = (r.get("Images") or "").strip().split(",")[0].strip()
            # Use a stable, woo-id-based cover path so images can be dropped in later
            ext = ".jpg"
            m = re.search(r"\.(jpg|jpeg|png|webp|gif)(\?|$)", img_field, re.IGNORECASE)
            if m:
                ext = "." + m.group(1).lower()
            prod_type = (r.get("Type") or "simple").strip()
            if prod_type == "woosb":
                cover_url = f"/media/book-packs/pack_woo{woo_id}{ext}"
            else:
                cover_url = f"/media/books/cover_woo{woo_id}{ext}"

            # categories
            cats_raw = parse_tokens(r.get("Catégories"))
            language = "AR" if contains_arabic(clean_title) else "FR"
            category_tags: list[str] = []
            for c in cats_raw:
                cl = c.lower()
                if cl in LANGUAGE_CATEGORY_MAP:
                    language = LANGUAGE_CATEGORY_MAP[cl]
                    continue
                if cl in DROPPED_CATEGORIES:
                    continue
                category_tags.append(c)

            # étiquettes
            ets_raw = parse_tokens(r.get("Étiquettes"))
            etiquette_tags: list[str] = []
            author_name: str | None = None
            for e in ets_raw:
                m = re.match(r"^AUTEUR\s*:\s*(.+)$", e, re.IGNORECASE)
                if m:
                    author_name = m.group(1).strip()
                else:
                    etiquette_tags.append(e)

            rows.append(
                ProductRow(
                    woo_id=woo_id,
                    type=r.get("Type", "simple").strip(),
                    raw_title=raw_title,
                    clean_title=clean_title,
                    description=description,
                    price=price,
                    stock=stock,
                    cover_url=cover_url,
                    language=language,
                    category_tags=category_tags,
                    etiquette_tags=etiquette_tags,
                    author_name=author_name,
                    woosb_child_ids=parse_woosb_ids(r.get("Méta : woosb_ids")),
                )
            )
    return rows


# --------------------------------------------------------------------------- #
# parsing: orders
# --------------------------------------------------------------------------- #
@dataclass
class OrderLine:
    sku: str | None
    name: str
    quantity: int
    unit_price: Decimal | None


@dataclass
class OrderRow:
    woo_num: str
    status_fr: str
    created_at: str | None
    full_name: str
    phone: str | None
    email: str | None
    street: str | None
    city: str | None
    wilaya: str | None
    postal_code: str | None
    total: Decimal | None
    shipping_cost: Decimal | None
    lines: list[OrderLine] = field(default_factory=list)


def load_orders() -> list[OrderRow]:
    wb = openpyxl.load_workbook(ORDERS_XLSX, read_only=True, data_only=True)
    ws = wb["Commandes"]
    rows_iter = ws.iter_rows(values_only=True)
    header = next(rows_iter)
    col = {h: i for i, h in enumerate(header)}

    C_NUM = col["Numéro de commande"]
    C_STATUS = col["État de la commande"]
    C_DATE = col["Date de commande"]
    C_FN = col["Prénom (Facturation)"]
    C_LN = col["Nom de famille (Facturation)"]
    C_ADDR = col["Adresse 1 & 2 (Facturation)"]
    C_CITY = col["Ville (Facturation)"]
    C_WIL = col["Code de l\u2019état (Facturation)"]
    C_POSTAL = col["Code postal (Facturation)"]
    C_EMAIL = col["E-mail (Facturation)"]
    C_PHONE = col["Téléphone (Facturation)"]
    C_TOTAL = col["Montant total de la commande"]
    C_SHIP = col["Montant de la livraison"]
    C_SKU = col["UGS"]
    C_ITEM = col["Nom de l\u2019élément"]
    C_QTY = col["Quantité (- Remboursement)"]
    C_PRICE = col["Prix du produit"]

    grouped: dict[str, OrderRow] = {}

    for r in rows_iter:
        num = str(r[C_NUM]).strip() if r[C_NUM] is not None else None
        if not num:
            continue

        def _s(v):
            if v is None:
                return None
            s = str(v).strip()
            return s or None

        order = grouped.get(num)
        if order is None:
            first = _s(r[C_FN]) or ""
            last = _s(r[C_LN]) or ""
            full_name = (first + " " + last).strip() or first or "N/A"
            order = OrderRow(
                woo_num=num,
                status_fr=_s(r[C_STATUS]) or "",
                created_at=parse_order_date(r[C_DATE]),
                full_name=full_name,
                phone=normalize_phone(r[C_PHONE]),
                email=_s(r[C_EMAIL]),
                street=_s(r[C_ADDR]),
                city=_s(r[C_CITY]),
                wilaya=strip_wilaya(_s(r[C_WIL])),
                postal_code=_s(r[C_POSTAL]),
                total=parse_price(r[C_TOTAL]),
                shipping_cost=parse_price(r[C_SHIP]),
            )
            grouped[num] = order

        qty = r[C_QTY]
        try:
            qty_i = int(float(str(qty).replace(",", "."))) if qty is not None else 0
        except ValueError:
            qty_i = 0
        if qty_i <= 0:
            continue

        line = OrderLine(
            sku=_s(r[C_SKU]),
            name=_s(r[C_ITEM]) or "",
            quantity=qty_i,
            unit_price=parse_price(r[C_PRICE]),
        )
        if not line.name:
            continue
        order.lines.append(line)

    return list(grouped.values())


# --------------------------------------------------------------------------- #
# SQL generation
# --------------------------------------------------------------------------- #
def emit_sql(products: list[ProductRow], orders: list[OrderRow]) -> str:
    out: list[str] = []
    w = out.append

    w("-- Generated by scripts/seed_from_woo.py")
    w("-- Idempotent seed of WooCommerce data into Esprit Livre prod DB.")
    w("-- Preserves existing authors + tags; wipes books/packs/orders/likes and reinserts.")
    w("SET client_encoding = 'UTF8';")
    w("BEGIN;")
    w("")

    # ------------------------------------------------------------------ wipe
    w("-- 1. Wipe existing book/order data (preserves author + tag rows)")
    for stmt in (
        "DELETE FROM rel_book_pack__books",
        "DELETE FROM rel_tag__book",
        "DELETE FROM order_item",
        "DELETE FROM jhi_order",
        "DELETE FROM jhi_like",
        "DELETE FROM book_pack",
        "DELETE FROM book",
    ):
        w(stmt + ";")
    w("")

    # --------------------------------------------------------------- authors
    w("-- 2. Merge authors (insert only if name not already present, case-insensitive)")
    authors_needed = sorted({p.author_name for p in products if p.author_name})
    for name in authors_needed:
        w(
            "INSERT INTO author(id, name, profile_picture_url, active, created_by, created_date)\n"
            f"SELECT nextval('author_seq'), {sql_str(name)}, '/media/authors/default.webp', true, 'woo-import', now()\n"
            f"WHERE NOT EXISTS (SELECT 1 FROM author WHERE lower(name) = lower({sql_str(name)}));"
        )
    w("")

    # ------------------------------------------------------------------ tags
    w("-- 3. Merge tags (CATEGORY and ETIQUETTE, matched case-insensitively on name_fr + type)")
    cat_tags: set[str] = set()
    et_tags: set[str] = set()
    for p in products:
        for t in p.category_tags:
            cat_tags.add(t)
        for t in p.etiquette_tags:
            et_tags.add(t)
    for t in sorted(cat_tags):
        w(
            "INSERT INTO tag(id, type, name_fr, name_en, active)\n"
            f"SELECT nextval('tag_seq'), 'CATEGORY', {sql_str(t)}, {sql_str(t)}, true\n"
            f"WHERE NOT EXISTS (SELECT 1 FROM tag WHERE type='CATEGORY' AND lower(name_fr) = lower({sql_str(t)}));"
        )
    for t in sorted(et_tags):
        w(
            "INSERT INTO tag(id, type, name_fr, name_en, active)\n"
            f"SELECT nextval('tag_seq'), 'ETIQUETTE', {sql_str(t)}, {sql_str(t)}, true\n"
            f"WHERE NOT EXISTS (SELECT 1 FROM tag WHERE type='ETIQUETTE' AND lower(name_fr) = lower({sql_str(t)}));"
        )
    w("")

    # -------------------------------------------------------- books + packs
    w("-- 4. Temp mapping tables so order_item lookups can resolve by woo id / title")
    w("CREATE TEMP TABLE _book_map(woo_id TEXT PRIMARY KEY, canon_title TEXT, book_id BIGINT NOT NULL) ON COMMIT DROP;")
    w("CREATE TEMP TABLE _pack_map(woo_id TEXT PRIMARY KEY, canon_title TEXT, pack_id BIGINT NOT NULL) ON COMMIT DROP;")
    w("CREATE TEMP TABLE _order_map(woo_num TEXT PRIMARY KEY, order_id BIGINT NOT NULL) ON COMMIT DROP;")
    w("")

    w("-- 5. Insert books (Woo 'simple' products) and bundles ('woosb') as book_packs")
    simple_products = [p for p in products if p.type != "woosb"]
    bundle_products = [p for p in products if p.type == "woosb"]

    for p in simple_products:
        price = str(p.price) if p.price is not None else "NULL"
        # description could be empty; title may contain apostrophes
        w(
            "WITH ins AS (\n"
            "  INSERT INTO book(id, title, author_id, price, stock_quantity, cover_image_url, description, active, language, delivery_fee, automatic_delivery_fee, created_at, updated_at)\n"
            f"  VALUES (nextval('book_seq'), {sql_str(p.clean_title)}, "
            + (
                f"(SELECT id FROM author WHERE lower(name)=lower({sql_str(p.author_name)}) LIMIT 1)"
                if p.author_name
                else "NULL"
            )
            + f", {price}, {p.stock}, {sql_str(p.cover_url)}, {sql_str(p.description)}, true, '{p.language}', NULL, true, now(), now())\n"
            "  RETURNING id\n"
            ")\n"
            f"INSERT INTO _book_map(woo_id, canon_title, book_id) SELECT {sql_str(p.woo_id)}, {sql_str(canonical_title(p.clean_title))}, id FROM ins;"
        )
    w("")

    # rel_tag__book
    w("-- 6. Link books to tags")
    for p in simple_products:
        for t in p.category_tags:
            w(
                "INSERT INTO rel_tag__book(tag_id, book_id) SELECT "
                f"(SELECT id FROM tag WHERE type='CATEGORY' AND lower(name_fr)=lower({sql_str(t)}) LIMIT 1), "
                f"(SELECT book_id FROM _book_map WHERE woo_id={sql_str(p.woo_id)});"
            )
        for t in p.etiquette_tags:
            w(
                "INSERT INTO rel_tag__book(tag_id, book_id) SELECT "
                f"(SELECT id FROM tag WHERE type='ETIQUETTE' AND lower(name_fr)=lower({sql_str(t)}) LIMIT 1), "
                f"(SELECT book_id FROM _book_map WHERE woo_id={sql_str(p.woo_id)});"
            )
    w("")

    # book_packs
    w("-- 7. Insert book_packs (Woo 'woosb' bundle products)")
    for p in bundle_products:
        price = str(p.price) if p.price is not None else "0"
        # BookPack requires cover_url NOT NULL; reuse the same pattern
        w(
            "WITH ins AS (\n"
            "  INSERT INTO book_pack(id, title, description, cover_url, price, created_date, last_modified_date, active, delivery_fee, automatic_delivery_fee)\n"
            f"  VALUES (nextval('book_pack_seq'), {sql_str(p.clean_title)}, {sql_str(p.description)}, {sql_str(p.cover_url)}, {price}, now(), now(), true, NULL, true)\n"
            "  RETURNING id\n"
            ")\n"
            f"INSERT INTO _pack_map(woo_id, canon_title, pack_id) SELECT {sql_str(p.woo_id)}, {sql_str(canonical_title(p.clean_title))}, id FROM ins;"
        )
        # bundle children → rel_book_pack__books (only for children we know)
        for child_woo in p.woosb_child_ids:
            w(
                "INSERT INTO rel_book_pack__books(book_pack_id, books_id) SELECT "
                f"(SELECT pack_id FROM _pack_map WHERE woo_id={sql_str(p.woo_id)}), "
                f"(SELECT book_id FROM _book_map WHERE woo_id={sql_str(child_woo)}) "
                "WHERE EXISTS (SELECT 1 FROM _book_map WHERE woo_id="
                f"{sql_str(child_woo)});"
            )
    w("")

    # -------------------------------------------------------- orders + items
    w("-- 8. Insert orders and order_items (unresolved items skipped, empty orders skipped)")

    # Build title → (kind, woo_id) index for line resolution
    title_to_ref: dict[str, tuple[str, str]] = {}
    for p in simple_products:
        title_to_ref.setdefault(canonical_title(p.clean_title), ("book", p.woo_id))
    for p in bundle_products:
        title_to_ref.setdefault(canonical_title(p.clean_title), ("pack", p.woo_id))

    # Woo UGS → ref (none present in this export, but kept for completeness)
    sku_to_ref: dict[str, tuple[str, str]] = {}

    skipped_orders_no_match = 0
    skipped_orders_bad_status = 0
    total_lines_unmatched = 0
    total_lines_matched = 0
    emitted_orders = 0
    emitted_lines = 0

    for o in orders:
        status = STATUS_MAP.get(o.status_fr)
        if not status:
            skipped_orders_bad_status += 1
            continue
        if not o.phone:
            continue

        # Resolve lines first so we can skip orders with zero matches
        resolved: list[tuple[str, str, OrderLine]] = []
        for line in o.lines:
            ref = None
            if line.sku and line.sku in sku_to_ref:
                ref = sku_to_ref[line.sku]
            if ref is None:
                ref = title_to_ref.get(canonical_title(line.name))
            if ref is None:
                total_lines_unmatched += 1
                continue
            total_lines_matched += 1
            resolved.append((ref[0], ref[1], line))

        if not resolved:
            skipped_orders_no_match += 1
            continue

        emitted_orders += 1
        emitted_lines += len(resolved)

        total_sql = str(o.total) if o.total is not None else "NULL"
        ship_sql = str(o.shipping_cost) if o.shipping_cost is not None else "NULL"
        created_sql = f"'{o.created_at}'" if o.created_at else "now()"

        w(
            "WITH ins AS (\n"
            "  INSERT INTO jhi_order(id, unique_id, status, total_amount, shipping_cost, shipping_provider, shipping_method, is_stopdesk, full_name, phone, email, street_address, wilaya, city, postal_code, created_at, created_by, updated_at, active, delivery_fee_method)\n"
            f"  VALUES (nextval('order_seq'), {sql_str(o.woo_num)}, '{status}', {total_sql}, {ship_sql}, 'YALIDINE', 'HOME_DELIVERY', false, {sql_str(o.full_name)}, {sql_str(o.phone)}, {sql_str(o.email)}, {sql_str(o.street)}, {sql_str(o.wilaya)}, {sql_str(o.city)}, {sql_str(o.postal_code)}, {created_sql}, 'woo-import', {created_sql}, true, 'FIXED')\n"
            "  RETURNING id\n"
            ")\n"
            f"INSERT INTO _order_map(woo_num, order_id) SELECT {sql_str(o.woo_num)}, id FROM ins;"
        )

        for kind, woo_id, line in resolved:
            unit = str(line.unit_price) if line.unit_price is not None else "NULL"
            total_price = (
                str(line.unit_price * line.quantity) if line.unit_price is not None else "NULL"
            )
            if kind == "book":
                ref_book = f"(SELECT book_id FROM _book_map WHERE woo_id={sql_str(woo_id)})"
                ref_pack = "NULL"
                item_type = "BOOK"
            else:
                ref_book = "NULL"
                ref_pack = f"(SELECT pack_id FROM _pack_map WHERE woo_id={sql_str(woo_id)})"
                item_type = "PACK"
            w(
                "INSERT INTO order_item(id, quantity, unit_price, total_price, item_type, product_title_snapshot, order_id, book_id, book_pack_id)\n"
                f"VALUES (nextval('order_item_seq'), {line.quantity}, {unit}, {total_price}, '{item_type}', {sql_str(strip_html(line.name)[:255])}, "
                f"(SELECT order_id FROM _order_map WHERE woo_num={sql_str(o.woo_num)}), {ref_book}, {ref_pack});"
            )
    w("")

    w("COMMIT;")
    w("")
    w("-- Summary (from Python generator, for reference):")
    w(f"--   products parsed         : {len(products)}")
    w(f"--   simple books            : {len(simple_products)}")
    w(f"--   book packs              : {len(bundle_products)}")
    w(f"--   distinct authors merged : {len(authors_needed)}")
    w(f"--   category tags           : {len(cat_tags)}")
    w(f"--   etiquette tags          : {len(et_tags)}")
    w(f"--   orders parsed           : {len(orders)}")
    w(f"--   orders emitted          : {emitted_orders}")
    w(f"--   orders skipped (no match): {skipped_orders_no_match}")
    w(f"--   orders skipped (bad status): {skipped_orders_bad_status}")
    w(f"--   order lines matched     : {total_lines_matched}")
    w(f"--   order lines unmatched   : {total_lines_unmatched}")

    print("Summary:", file=sys.stderr)
    print(f"  products parsed           : {len(products)}", file=sys.stderr)
    print(f"  simple books              : {len(simple_products)}", file=sys.stderr)
    print(f"  book packs                : {len(bundle_products)}", file=sys.stderr)
    print(f"  authors merged            : {len(authors_needed)}", file=sys.stderr)
    print(f"  category tags             : {len(cat_tags)}", file=sys.stderr)
    print(f"  etiquette tags            : {len(et_tags)}", file=sys.stderr)
    print(f"  orders parsed             : {len(orders)}", file=sys.stderr)
    print(f"  orders emitted            : {emitted_orders}", file=sys.stderr)
    print(f"  orders skipped (no match) : {skipped_orders_no_match}", file=sys.stderr)
    print(f"  orders skipped (status)   : {skipped_orders_bad_status}", file=sys.stderr)
    print(f"  lines matched             : {total_lines_matched}", file=sys.stderr)
    print(f"  lines unmatched           : {total_lines_unmatched}", file=sys.stderr)

    return "\n".join(out) + "\n"


def main() -> int:
    products = load_products()
    orders = load_orders()
    sql = emit_sql(products, orders)
    OUT_SQL.write_text(sql, encoding="utf-8")
    print(f"Wrote {OUT_SQL} ({len(sql)//1024} KB)", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
