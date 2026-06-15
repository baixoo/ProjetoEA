"""
Generates seed_data.sql from the Porto STCP GTFS dataset.
Run:  python generate_seed.py
Output: src/main/resources/data.sql
"""
import csv
import random
import re
import unicodedata
from pathlib import Path
from collections import defaultdict

DATASETS_DIR = Path(__file__).resolve().parent.parent.parent / "datasetPorto"
OUTPUT_PATH = Path(__file__).parent / "src" / "main" / "resources" / "data.sql"

random.seed(42)

OPERATING_HOURS = 18
TURNAROUND_MINUTES = 20
MAX_ZONE_NUM = 3

ZONE_MACRO_MAP = {
    "PRT1": 1, "PRT2": 1, "PRT3": 1,
    "MTS1": 1, "MTS2": 1,
    "MAI1": 2, "MAI2": 2, "MAI3": 2, "MAI4": 2,
    "VCD8": 2,
    "VNG1": 3, "VNG2": 3, "VNG4": 3, "VNG5": 3,
    "GDM1": 3, "GDM2": 3,
    "VLG1": 3, "VLG2": 3, "VLG3": 3,
}

MACRO_ZONE_NAMES = {
    1: "Porto e Matosinhos",
    2: "Maia e Vila do Conde",
    3: "Vila Nova de Gaia, Gondomar e Valongo",
}

WEEKDAY_SERVICE_IDS = {"UTEIS", "ELECUTEIS"}


def sql_escape(value):
    return value.replace("'", "''")


def to_roman(value):
    numerals = [
        (1000, "M"), (900, "CM"), (500, "D"), (400, "CD"),
        (100, "C"), (90, "XC"), (50, "L"), (40, "XL"),
        (10, "X"), (9, "IX"), (5, "V"), (4, "IV"), (1, "I"),
    ]
    result = []
    remaining = value
    for arabic, roman in numerals:
        while remaining >= arabic:
            result.append(roman)
            remaining -= arabic
    return "".join(result)


def normalize_stop_group_key(name):
    if not name:
        return ""
    normalized = unicodedata.normalize("NFKD", name)
    normalized = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    normalized = normalized.upper().strip()
    normalized = re.sub(r"[^A-Z0-9]+", " ", normalized)
    normalized = re.sub(r"\s+", " ", normalized).strip()
    normalized = re.sub(r"\s+(?:I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII)$", "", normalized)
    return normalized or name.strip().upper()


def format_stop_display_name(name):
    if not name:
        return ""
    formatted = unicodedata.normalize("NFC", name)
    formatted = re.sub(r"\s+", " ", formatted.strip())
    formatted = re.sub(r"\.(?=[A-Za-zÀ-ÖØ-öø-ÿ])", ". ", formatted)
    formatted = re.sub(r"\s+", " ", formatted).strip()
    return formatted.lower().title()


def build_stop_display_names(stops):
    grouped = defaultdict(list)
    for stop_id, stop in stops.items():
        grouped[normalize_stop_group_key(stop["name"])].append((stop_id, stop))

    display_names = {}
    for entries in grouped.values():
        entries.sort(key=lambda item: (
            float(item[1]["lat"]),
            float(item[1]["lon"]),
            item[1]["code"] or "",
            item[0],
        ))
        if len(entries) == 1:
            stop_id, stop = entries[0]
            display_names[stop_id] = format_stop_display_name(stop["name"])
            continue

        for idx, (stop_id, stop) in enumerate(entries, start=1):
            display_names[stop_id] = f"{format_stop_display_name(stop['name'])} {to_roman(idx)}"

    return display_names


def load_stops():
    stops = {}
    with open(DATASETS_DIR / "stops.txt", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            stops[row["stop_id"]] = {
                "code": row["stop_code"],
                "name": row["stop_name"],
                "lat": row["stop_lat"],
                "lon": row["stop_lon"],
                "zone_id": row["zone_id"],
            }
    return stops


def load_routes():
    routes = {}
    with open(DATASETS_DIR / "routes.txt", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            routes[row["route_id"]] = {
                "short_name": row["route_short_name"],
                "long_name": row["route_long_name"],
                "color": row["route_color"],
            }
    return routes


def load_trips():
    trips = defaultdict(list)
    with open(DATASETS_DIR / "trips.txt", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            trips[(row["route_id"], row["direction_id"])].append({
                "trip_id": row["trip_id"],
                "service_id": row["service_id"],
                "headsign": row.get("trip_headsign", ""),
            })
    return trips


def load_stop_times():
    stop_times = defaultdict(list)
    with open(DATASETS_DIR / "stop_times.txt", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            stop_times[row["trip_id"]].append({
                "arrival": row["arrival_time"],
                "departure": row["departure_time"],
                "stop_id": row["stop_id"],
                "sequence": int(row["stop_sequence"]),
            })
    for trip_id in stop_times:
        stop_times[trip_id].sort(key=lambda x: x["sequence"])
    return stop_times


def time_to_seconds(t):
    parts = t.split(":")
    h = int(parts[0])
    m = int(parts[1])
    s = int(parts[2]) if len(parts) > 2 else 0
    return h * 3600 + m * 60 + s


def seconds_to_time(secs):
    h = secs // 3600
    m = (secs % 3600) // 60
    s = secs % 60
    if h >= 24:
        h -= 24
    return f"{h:02d}:{m:02d}:{s:02d}"


def normalize_time(t):
    return seconds_to_time(time_to_seconds(t))


def pick_representative_trip(trip_list, stop_times):
    weekday_trips = [t for t in trip_list if t["service_id"] in WEEKDAY_SERVICE_IDS]
    if not weekday_trips:
        weekday_trips = trip_list

    morning_trips = []
    for t in weekday_trips:
        tid = t["trip_id"]
        if tid in stop_times and stop_times[tid]:
            first_arrival = stop_times[tid][0]["arrival"]
            mins = time_to_seconds(first_arrival) // 60
            if 420 <= mins <= 600:
                morning_trips.append(t)

    candidates = morning_trips if morning_trips else weekday_trips

    best_trip = None
    best_count = 0
    for t in candidates:
        tid = t["trip_id"]
        if tid in stop_times:
            count = len(stop_times[tid])
            if count > best_count:
                best_count = count
                best_trip = tid

    if best_trip is None:
        for t in trip_list:
            tid = t["trip_id"]
            if tid in stop_times:
                count = len(stop_times[tid])
                if count > best_count:
                    best_count = count
                    best_trip = tid

    return best_trip


def main():
    print("Loading Porto GTFS dataset...")
    stops = load_stops()
    routes = load_routes()
    trips = load_trips()
    stop_times = load_stop_times()
    stop_display_names = build_stop_display_names(stops)

    print(f"  Stops: {len(stops)}")
    print(f"  Routes: {len(routes)}")
    print(f"  Route+Direction combos: {len(trips)}")

    lines = []
    lines.append("-- NoTUB Seed Data (auto-generated from Porto STCP GTFS dataset)")
    lines.append("-- DO NOT EDIT MANUALLY - regenerate with: python generate_seed.py")
    lines.append("")
    # ── Atualização de Esquema ──
    lines.append("-- ═══ Atualização de Esquema ═══")
    lines.append("DROP TABLE IF EXISTS horario CASCADE;")
    lines.append("DROP TABLE IF EXISTS ponto_passagem CASCADE;")
    lines.append("DROP TABLE IF EXISTS pontos_de_passagem CASCADE;")
    lines.append("DROP TABLE IF EXISTS servico CASCADE;")
    lines.append("DROP TABLE IF EXISTS viagem CASCADE;")
    lines.append("DROP TABLE IF EXISTS viagem_utilizador CASCADE;")
    lines.append("DROP TABLE IF EXISTS autocarro CASCADE;")
    lines.append(
        "CREATE TABLE IF NOT EXISTS servico ("
        "id BIGSERIAL PRIMARY KEY, "
        "nome VARCHAR(255) NOT NULL UNIQUE"
        ");"
    )
    lines.append(
        "CREATE TABLE IF NOT EXISTS ponto_passagem ("
        "id BIGSERIAL PRIMARY KEY, "
        "ordem INT NOT NULL, "
        "trajeto_id BIGINT NOT NULL REFERENCES trajeto(id) ON DELETE CASCADE, "
        "paragem_id BIGINT NOT NULL REFERENCES paragem(id)"
        ");"
    )
    lines.append(
        "CREATE TABLE IF NOT EXISTS horario ("
        "id BIGSERIAL PRIMARY KEY, "
        "hora TIME NOT NULL, "
        "ponto_passagem_id BIGINT NOT NULL REFERENCES ponto_passagem(id) ON DELETE CASCADE, "
        "servico_id BIGINT NOT NULL REFERENCES servico(id), "
        "gtfs_trip_id VARCHAR(255) NOT NULL"
        ");"
    )
    lines.append("")

    lines.append("BEGIN;")
    lines.append("")

    # ── Zonas ──
    lines.append("-- ═══ Zonas ═══")
    for zid in range(1, MAX_ZONE_NUM + 1):
        name = MACRO_ZONE_NAMES[zid]
        lines.append(
            f"INSERT INTO zona (id, num, nome) VALUES ({zid}, {zid}, '{sql_escape(name)}') ON CONFLICT (id) DO NOTHING;"
        )
    lines.append("")

    # ── Serviços ──
    lines.append("-- ═══ Serviços ═══")
    servico_map = {}
    servico_id_seq = 0
    for trip_list in trips.values():
        for t in trip_list:
            s_id = t["service_id"]
            if s_id not in servico_map:
                servico_id_seq += 1
                servico_map[s_id] = servico_id_seq
                lines.append(f"INSERT INTO servico (id, nome) VALUES ({servico_id_seq}, '{sql_escape(s_id)}') ON CONFLICT (id) DO NOTHING;")
    lines.append("")

    # ── Paragens ──
    lines.append("-- ═══ Paragens ═══")
    paragem_id_map = {}
    sorted_stop_ids = sorted(stops.keys())
    for idx, stop_id in enumerate(sorted_stop_ids):
        pid = idx + 1
        paragem_id_map[stop_id] = pid
        s = stops[stop_id]
        name = sql_escape(stop_display_names.get(stop_id, format_stop_display_name(s["name"])))
        lat = float(s["lat"])
        lon = float(s["lon"])
        zid = ZONE_MACRO_MAP.get(s["zone_id"], 1)
        lines.append(
            f"INSERT INTO paragem (id, nome, latitude, longitude, zona_id) "
            f"VALUES ({pid}, '{name}', {lat}, {lon}, {zid}) ON CONFLICT (id) DO NOTHING;"
        )
    lines.append("")

    # ── Linhas + Trajetos + PontosDePassagem + Horarios ──
    lines.append("-- ═══ Linhas, Trajetos e Horários ═══")
    linha_id = 0
    trajeto_id = 0
    pdp_id = 0
    autocarro_id = 0
    horario_id = 0
    linhas_usadas = 0
    trajetos_usados = 0
    total_horarios = 0

    for route_id in sorted(routes.keys()):
        route = routes[route_id]
        linha_id += 1
        linhas_usadas += 1
        short = route["short_name"]
        long_name = route["long_name"].replace("'", "''")

        lines.append(
            f"INSERT INTO linha (id, nome, identificador_servico) "
            f"VALUES ({linha_id}, '{short} - {long_name}', '{route_id}') ON CONFLICT (id) DO NOTHING;"
        )

        linha_viagens_uteis = 0
        linha_max_duracao = 0

        for direction in ["0", "1"]:
            direction_name = "IDA" if direction == "0" else "VOLTA"
            combo = (route_id, direction)
            if combo not in trips:
                continue

            trip_list = trips[combo]
            best_trip = pick_representative_trip(trip_list, stop_times)

            if best_trip is None or best_trip not in stop_times:
                continue
            if len(stop_times[best_trip]) < 2:
                continue

            trajeto_id += 1
            trajetos_usados += 1
            this_trajeto_id = trajeto_id
            lines.append(
                f"INSERT INTO trajeto (id, direcao, linha_id) "
                f"VALUES ({this_trajeto_id}, '{direction_name}', {linha_id}) ON CONFLICT (id) DO NOTHING;"
            )

            st_list = stop_times[best_trip]
            first_arrival_secs = time_to_seconds(st_list[0]["arrival"])
            trajeto_pdps = []

            for ordem, st in enumerate(st_list):
                pdp_id += 1
                paragem_fk = paragem_id_map.get(st["stop_id"])
                if paragem_fk is None:
                    pdp_id -= 1
                    continue

                current_secs = time_to_seconds(st["arrival"])
                diff_secs = current_secs - first_arrival_secs

                trajeto_pdps.append((pdp_id, diff_secs))

                lines.append(
                    f"INSERT INTO ponto_passagem (id, ordem, trajeto_id, paragem_id) "
                    f"VALUES ({pdp_id}, {ordem}, {this_trajeto_id}, {paragem_fk}) ON CONFLICT (id) DO NOTHING;"
                )

            last_arrival_secs = time_to_seconds(st_list[-1]["arrival"])
            duracao_min = (last_arrival_secs - first_arrival_secs) // 60
            if duracao_min > linha_max_duracao:
                linha_max_duracao = duracao_min

            # Gerar Horários a partir das Viagens
            for t in trip_list:
                tid = t["trip_id"]
                if tid not in stop_times or not stop_times[tid]:
                    continue

                service_id = t["service_id"]
                servico_fk = servico_map[service_id]

                if service_id in WEEKDAY_SERVICE_IDS:
                    linha_viagens_uteis += 1

                first_dep_secs = time_to_seconds(stop_times[tid][0]["departure"])

                for current_pdp_id, diff_secs in trajeto_pdps:
                    horario_id += 1
                    total_horarios += 1

                    hora_passagem_secs = first_dep_secs + diff_secs
                    hora_str = seconds_to_time(hora_passagem_secs)

                    lines.append(
                        f"INSERT INTO horario (id, hora, ponto_passagem_id, servico_id, gtfs_trip_id) "
                        f"VALUES ({horario_id}, '{hora_str}', {current_pdp_id}, {servico_fk}, '{sql_escape(tid)}') ON CONFLICT (id) DO NOTHING;"
                    )

        round_trip_min = max(60, linha_max_duracao * 2 + TURNAROUND_MINUTES)
        viagens_per_bus = max(1, (OPERATING_HOURS * 60) // round_trip_min)
        num_autocarros = max(2, (linha_viagens_uteis + viagens_per_bus - 1) // viagens_per_bus)

        for i in range(num_autocarros):
            autocarro_id += 1
            matricula = f"PT-{short[:3].upper()}-{autocarro_id:02d}"
            nlugares = random.choice([40, 50, 60])
            lines.append(
                f"INSERT INTO veiculo (id, matricula, n_lugares, lotacao_atual, tempo_atraso, tipo, linha_id) "
                f"VALUES ({autocarro_id}, '{matricula}', {nlugares}, 0, 0, 'AUTOCARRO', {linha_id}) ON CONFLICT (id) DO NOTHING;"
            )

    lines.append("")
    lines.append("-- ═══ Tarifas ═══")
    tarifa_id = 0

    BILHETE_PRICES = {
        "ADULTO":    [1.50, 2.20, 3.00, 3.80],
        "CRIANCA":   [0.75, 1.10, 1.50, 1.90],
        "ESTUDANTE": [1.00, 1.50, 2.00, 2.50],
        "SENIOR":    [0.75, 1.10, 1.50, 1.90],
    }

    PASSE_PRICES = {
        "H24":     {"ADULTO": [4.50, 6.50, 8.50, 10.00], "ESTUDANTE": [3.00, 4.50, 5.50, 6.50], "CRIANCA": [2.50, 3.50, 4.50, 5.00], "SENIOR": [2.50, 3.50, 4.50, 5.00]},
        "H48":     {"ADULTO": [8.00, 11.50, 15.00, 18.00], "ESTUDANTE": [5.50, 8.00, 10.00, 12.00], "CRIANCA": [4.00, 6.00, 7.50, 9.00], "SENIOR": [4.00, 6.00, 7.50, 9.00]},
        "H72":     {"ADULTO": [11.00, 16.00, 20.00, 24.00], "ESTUDANTE": [7.50, 11.00, 14.00, 16.00], "CRIANCA": [5.50, 8.00, 10.00, 12.00], "SENIOR": [5.50, 8.00, 10.00, 12.00]},
        "SEMANAL": {"ADULTO": [22.00, 32.00, 40.00, 48.00], "ESTUDANTE": [15.00, 22.00, 28.00, 32.00], "CRIANCA": [11.00, 16.00, 20.00, 24.00], "SENIOR": [11.00, 16.00, 20.00, 24.00]},
        "MENSAL":  {"ADULTO": [40.00, 55.00, 70.00, 85.00], "ESTUDANTE": [25.00, 35.00, 45.00, 55.00], "CRIANCA": [20.00, 28.00, 35.00, 43.00], "SENIOR": [20.00, 28.00, 35.00, 43.00]},
        "ANUAL":   {"ADULTO": [400.00, 550.00, 700.00, 850.00], "ESTUDANTE": [250.00, 350.00, 450.00, 550.00], "CRIANCA": [200.00, 280.00, 350.00, 430.00], "SENIOR": [200.00, 280.00, 350.00, 430.00]},
    }

    for tipo, prices in BILHETE_PRICES.items():
        for nr_z in range(1, MAX_ZONE_NUM + 1):
            tarifa_id += 1
            valor = prices[nr_z - 1]
            lines.append(
                f"INSERT INTO tarifa (id, valor, tipo_utilizador, modalidade, nr_zonas) "
                f"VALUES ({tarifa_id}, {valor}, '{tipo}', NULL, {nr_z}) ON CONFLICT (id) DO NOTHING;"
            )

    lines.append("")

    for modalidade, tipo_prices in PASSE_PRICES.items():
        for tipo, prices in tipo_prices.items():
            for nr_z in range(1, MAX_ZONE_NUM + 1):
                tarifa_id += 1
                valor = prices[nr_z - 1]
                lines.append(
                    f"INSERT INTO tarifa (id, valor, tipo_utilizador, modalidade, nr_zonas) "
                    f"VALUES ({tarifa_id}, {valor}, '{tipo}', '{modalidade}', {nr_z}) ON CONFLICT (id) DO NOTHING;"
                )

    lines.append("")

    # ── Sequences reset ──
    lines.append("-- ═══ Reset Sequences ═══")
    lines.append(f"SELECT setval('zona_id_seq', {MAX_ZONE_NUM});")
    lines.append(f"SELECT setval('servico_id_seq', {servico_id_seq});")
    lines.append(f"SELECT setval('paragem_id_seq', {len(stops)});")
    lines.append(f"SELECT setval('linha_id_seq', {linhas_usadas});")
    lines.append(f"SELECT setval('trajeto_id_seq', {trajetos_usados});")
    lines.append(f"SELECT setval('ponto_passagem_id_seq', {pdp_id});")
    lines.append(f"SELECT setval('veiculo_id_seq', {autocarro_id});")
    lines.append(f"SELECT setval('tarifa_id_seq', {tarifa_id});")
    lines.append("")

    lines.append("COMMIT;")
    lines.append("")

    output = "\n".join(lines)
    OUTPUT_PATH.write_text(output, encoding="utf-8")
    print(f"\nDone! Written to {OUTPUT_PATH}")
    print(f"  Zones:      {MAX_ZONE_NUM}")
    print(f"  Serviços:   {servico_id_seq}")
    print(f"  Paragens:   {len(stops)}")
    print(f"  Linhas:     {linhas_usadas}")
    print(f"  Trajetos:   {trajetos_usados}")
    print(f"  Pontos:     {pdp_id}")
    print(f"  Horários:   {total_horarios}")
    print(f"  Autocarros: {autocarro_id}")
    print(f"  Tarifas:    {tarifa_id}")

if __name__ == "__main__":
    main()
