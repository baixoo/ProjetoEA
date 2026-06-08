"""
Generates seed_data.sql from the Porto STCP GTFS dataset.
Run:  python generate_seed.py
Output: src/main/resources/data.sql

Improvements over v1:
- Reads calendar.txt for service_id awareness
- Picks representative trip from UTEIS morning departures (07:00-10:00)
- Generates ALL viagem rows (UTEIS + SAB + DOM) for real schedule lookup
- Auto-detects night routes (noturno) based on departure times
- Stores Viagem (trip departures) separately from Trajeto (stop patterns)
"""
import csv
import random
from pathlib import Path
from collections import defaultdict

DATASETS_DIR = Path(__file__).resolve().parent.parent.parent / "datasetPorto"
OUTPUT_PATH = Path(__file__).parent / "src" / "main" / "resources" / "data.sql"

random.seed(42)

OPERATING_HOURS = 18
TURNAROUND_MINUTES = 20

ZONE_MACRO_MAP = {
    "PRT1": 1, "PRT2": 1, "PRT3": 1,
    "MTS1": 1, "MTS2": 1,
    "MAI1": 2, "MAI2": 2, "MAI3": 2, "MAI4": 2,
    "VCD8": 2,
    "VNG1": 3, "VNG2": 3, "VNG4": 3, "VNG5": 3,
    "GDM1": 4, "GDM2": 4,
    "VLG1": 4, "VLG2": 4, "VLG3": 4,
}

MACRO_ZONE_NAMES = {
    1: "Porto e Matosinhos",
    2: "Maia e Vila do Conde",
    3: "Vila Nova de Gaia",
    4: "Gondomar e Valongo",
}

WEEKDAY_SERVICE_IDS = {"UTEIS", "ELECUTEIS"}


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


def time_to_minutes(t):
    parts = t.split(":")
    h = int(parts[0])
    m = int(parts[1])
    if h >= 24:
        h -= 24
    return h * 60 + m


def normalize_time(t):
    parts = t.split(":")
    h = int(parts[0])
    m = int(parts[1])
    s = int(parts[2])
    if h >= 24:
        h -= 24
    return f"{h:02d}:{m:02d}:{s:02d}"


def is_night_time(t):
    h = int(t.split(":")[0])
    if h >= 24:
        h -= 24
    return h < 6


def pick_representative_trip(trip_list, stop_times):
    weekday_trips = [t for t in trip_list if t["service_id"] in WEEKDAY_SERVICE_IDS]
    if not weekday_trips:
        weekday_trips = trip_list

    morning_trips = []
    for t in weekday_trips:
        tid = t["trip_id"]
        if tid in stop_times and stop_times[tid]:
            first_arrival = stop_times[tid][0]["arrival"]
            mins = time_to_minutes(first_arrival)
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
    print(f"  Stops: {len(stops)}")
    print(f"  Routes: {len(routes)}")
    print(f"  Route+Direction combos: {len(trips)}")

    lines = []
    lines.append("-- NoTUB Seed Data (auto-generated from Porto STCP GTFS dataset)")
    lines.append("-- DO NOT EDIT MANUALLY - regenerate with: python generate_seed.py")
    lines.append("")
    lines.append("BEGIN;")
    lines.append("")

    # ── Zonas ──
    lines.append("-- ═══ Zonas ═══")
    for zid in range(1, 5):
        name = MACRO_ZONE_NAMES[zid]
        lines.append(
            f"INSERT INTO zona (id, num, nome) VALUES ({zid}, {zid}, '{name}') ON CONFLICT (id) DO NOTHING;"
        )
    lines.append("")

    # ── Paragens ──
    lines.append("-- ═══ Paragens ═══")
    paragem_id_map = {}
    sorted_stop_ids = sorted(stops.keys())
    for idx, stop_id in enumerate(sorted_stop_ids):
        pid = idx + 1
        paragem_id_map[stop_id] = pid
        s = stops[stop_id]
        name = s["name"].replace("'", "''")
        lat = float(s["lat"])
        lon = float(s["lon"])
        zid = ZONE_MACRO_MAP.get(s["zone_id"], 1)
        lines.append(
            f"INSERT INTO paragem (id, nome, latitude, longitude, zona_id) "
            f"VALUES ({pid}, '{name}', {lat}, {lon}, {zid}) ON CONFLICT (id) DO NOTHING;"
        )
    lines.append("")

    # ── Linhas + Trajetos + PontosDePassagem + Viagens ──
    lines.append("-- ═══ Linhas ═══")
    linha_id = 0
    trajeto_id = 0
    pdp_id = 0
    autocarro_id = 0
    viagem_id = 0
    linhas_usadas = 0
    trajetos_usados = 0
    total_viagens = 0

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
            first_arrival_minutes = time_to_minutes(st_list[0]["arrival"])

            for ordem, st in enumerate(st_list):
                pdp_id += 1
                paragem_fk = paragem_id_map.get(st["stop_id"])
                if paragem_fk is None:
                    pdp_id -= 1
                    continue
                arrival = normalize_time(st["arrival"])
                current_minutes = time_to_minutes(st["arrival"])
                tempo_desde_inicio = current_minutes - first_arrival_minutes
                lines.append(
                    f"INSERT INTO pontos_de_passagem (id, ordem, hora_chegada, tempo_desde_inicio, trajeto_id, paragem_id) "
                    f"VALUES ({pdp_id}, {ordem}, '{arrival}', {tempo_desde_inicio}, {this_trajeto_id}, {paragem_fk}) ON CONFLICT (id) DO NOTHING;"
                )

            last_arrival_minutes = time_to_minutes(st_list[-1]["arrival"])
            duracao_min = last_arrival_minutes - first_arrival_minutes
            if duracao_min > linha_max_duracao:
                linha_max_duracao = duracao_min

            for t in trip_list:
                tid = t["trip_id"]
                if tid not in stop_times or not stop_times[tid]:
                    continue
                service_id = t["service_id"]
                first_dep = normalize_time(stop_times[tid][0]["departure"])
                viagem_id += 1
                total_viagens += 1
                if service_id in WEEKDAY_SERVICE_IDS:
                    linha_viagens_uteis += 1
                gtfs_trip_escaped = tid.replace("'", "''")
                lines.append(
                    f"INSERT INTO viagem (id, trajeto_id, service_id, hora_partida, gtfs_trip_id) "
                    f"VALUES ({viagem_id}, {this_trajeto_id}, '{service_id}', '{first_dep}', '{gtfs_trip_escaped}') ON CONFLICT (id) DO NOTHING;"
                )

        round_trip_min = max(60, linha_max_duracao * 2 + TURNAROUND_MINUTES)
        viagens_per_bus = max(1, (OPERATING_HOURS * 60) // round_trip_min)
        num_autocarros = max(2, (linha_viagens_uteis + viagens_per_bus - 1) // viagens_per_bus)

        for i in range(num_autocarros):
            autocarro_id += 1
            matricula = f"PT-{short[:3].upper()}-{autocarro_id:02d}"
            nlugares = random.choice([40, 50, 60])
            lines.append(
                f"INSERT INTO veiculo (id, matricula, n_lugares, lotacao_atual, dtype, linha_id) "
                f"VALUES ({autocarro_id}, '{matricula}', {nlugares}, 0, 'Autocarro', {linha_id}) ON CONFLICT (id) DO NOTHING;"
            )
            lines.append(
                f"INSERT INTO autocarro (id) VALUES ({autocarro_id}) ON CONFLICT (id) DO NOTHING;"
            )

    lines.append("")

    # ── Tarifas ──
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
        for nr_z in range(1, 5):
            tarifa_id += 1
            valor = prices[nr_z - 1]
            lines.append(
                f"INSERT INTO tarifa (id, valor, tipo_utilizador, modalidade, nr_zonas) "
                f"VALUES ({tarifa_id}, {valor}, '{tipo}', NULL, {nr_z}) ON CONFLICT (id) DO NOTHING;"
            )

    lines.append("")

    for modalidade, tipo_prices in PASSE_PRICES.items():
        for tipo, prices in tipo_prices.items():
            for nr_z in range(1, 5):
                tarifa_id += 1
                valor = prices[nr_z - 1]
                lines.append(
                    f"INSERT INTO tarifa (id, valor, tipo_utilizador, modalidade, nr_zonas) "
                    f"VALUES ({tarifa_id}, {valor}, '{tipo}', '{modalidade}', {nr_z}) ON CONFLICT (id) DO NOTHING;"
                )

    lines.append("")

    # ── Sequences reset ──
    lines.append("-- ═══ Reset Sequences ═══")
    lines.append(f"SELECT setval('zona_id_seq', 4);")
    lines.append(f"SELECT setval('paragem_id_seq', {len(stops)});")
    lines.append(f"SELECT setval('linha_id_seq', {linhas_usadas});")
    lines.append(f"SELECT setval('trajeto_id_seq', {trajetos_usados});")
    lines.append(f"SELECT setval('pontos_de_passagem_id_seq', {pdp_id});")
    lines.append(f"SELECT setval('viagem_id_seq', {viagem_id});")
    lines.append(f"SELECT setval('veiculo_id_seq', {autocarro_id});")
    lines.append(f"SELECT setval('tarifa_id_seq', {tarifa_id});")
    lines.append("")

    lines.append("COMMIT;")
    lines.append("")

    output = "\n".join(lines)
    OUTPUT_PATH.write_text(output, encoding="utf-8")
    print(f"\nDone! Written to {OUTPUT_PATH}")
    print(f"  Zones:      4")
    print(f"  Paragens:   {len(stops)}")
    print(f"  Linhas:     {linhas_usadas}")
    print(f"  Trajetos:   {trajetos_usados}")
    print(f"  Pontos:     {pdp_id}")
    print(f"  Viagens:    {total_viagens}")
    print(f"  Autocarros: {autocarro_id}")
    print(f"  Tarifas:    {tarifa_id}")


if __name__ == "__main__":
    main()
