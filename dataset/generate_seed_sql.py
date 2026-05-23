import csv
import json
import os
import sys
from collections import defaultdict
from datetime import datetime, timedelta

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STOPS_FILE = os.path.join(SCRIPT_DIR, "BusStopList.csv")
DIST_FILE = os.path.join(SCRIPT_DIR, "DistanceMatrix.csv")
ROUTES_FILE = os.path.join(SCRIPT_DIR, "BusRoutes.json")
OUTPUT_FILE = os.path.join(SCRIPT_DIR, "seed_data.sql")

NUM_ZONES = 3
NUM_VEHICLES = 20
NUM_VIAGENS = 30


def load_stops():
    stops = {}
    stop_names = []
    with open(STOPS_FILE, "r", encoding="utf-8") as f:
        reader = csv.reader(f)
        next(reader)
        for row in reader:
            fid = int(row[0])
            name = row[1]
            stops[name] = fid
            stop_names.append(name)
    return stops, stop_names


def load_distance_matrix(stop_names):
    print("Loading distance matrix...")
    idx = {name: i for i, name in enumerate(stop_names)}
    dist_to_center = [0.0] * len(stop_names)

    with open(DIST_FILE, "r", encoding="utf-8") as f:
        reader = csv.reader(f)
        header = next(reader)
        matrix_stops = header[1:]

        center_sums = [0.0] * len(matrix_stops)
        row_count = 0
        for row in reader:
            from_stop = row[0]
            for i, val in enumerate(row[1:]):
                d = float(val)
                center_sums[i] += d
            row_count += 1

    center_idx = center_sums.index(min(center_sums))
    center_stop = matrix_stops[center_idx]
    print(f"  Center stop: {center_stop} (sum of distances: {center_sums[center_idx]:.2f})")

    center_dists = {}
    with open(DIST_FILE, "r", encoding="utf-8") as f:
        reader = csv.reader(f)
        header = next(reader)
        col_idx = header.index(center_stop)
        next(reader)
        for row in reader:
            pass

    center_dists = {}
    with open(DIST_FILE, "r", encoding="utf-8") as f:
        reader = csv.reader(f)
        header = next(reader)
        matrix_stops = header[1:]
        center_col = header.index(center_stop)
        for row in reader:
            if row[0] == center_stop:
                for i, val in enumerate(row[1:]):
                    center_dists[matrix_stops[i]] = float(val)
                break

    return center_stop, center_dists


def assign_zones(stop_names, center_dists):
    print("Assigning zones...")
    stop_dists = []
    for name in stop_names:
        d = center_dists.get(name, 999.0)
        stop_dists.append((name, d))

    stop_dists.sort(key=lambda x: x[1])

    n = len(stop_dists)
    zone_size = n // NUM_ZONES
    zones = {}
    for i, (name, d) in enumerate(stop_dists):
        if i < zone_size:
            zones[name] = 1
        elif i < zone_size * 2:
            zones[name] = 2
        else:
            zones[name] = 3

    for z in range(1, NUM_ZONES + 1):
        count = sum(1 for v in zones.values() if v == z)
        dists_in_zone = [d for name, d in stop_dists if zones[name] == z]
        print(f"  Zona {z}: {count} paragens (dist: {dists_in_zone[0]:.2f} - {dists_in_zone[-1]:.2f})")

    return zones


def load_routes():
    with open(ROUTES_FILE, "r", encoding="utf-8") as f:
        return json.load(f)


def group_routes(routes):
    named_lines = {}
    ser_groups = defaultdict(list)

    for key, stops in routes.items():
        if key.startswith("SER_"):
            first = stops[0]["Stop_stn"]
            last = stops[-1]["Stop_stn"]
            ser_groups[(first, last)].append((key, stops))
        else:
            named_lines[key] = stops

    return named_lines, ser_groups


def sql_escape(s):
    return s.replace("'", "''")


def generate_sql(stop_names, stops_fid, zones, named_lines, ser_groups):
    lines = []

    lines.append("-- ============================================")
    lines.append("-- NoTUB Seed Data")
    lines.append(f"-- Generated: {datetime.now().isoformat()}")
    lines.append("-- ============================================")
    lines.append("")
    lines.append("BEGIN;")
    lines.append("")

    # ---- 1. ZONAS ----
    lines.append("-- 1. Zonas")
    lines.append("DELETE FROM zona;")
    lines.append("ALTER SEQUENCE IF EXISTS zona_id_seq RESTART WITH 1;")
    zone_names = {1: "Zona 1 - Centro", 2: "Zona 2 - Intermedio", 3: "Zona 3 - Periferia"}
    for z in range(1, NUM_ZONES + 1):
        lines.append(f"INSERT INTO zona (id, num, nome) VALUES ({z}, {z}, '{sql_escape(zone_names[z])}');")
    lines.append(f"SELECT setval('zona_id_seq', {NUM_ZONES + 1});")
    lines.append("")

    # ---- 2. PARAGENS ----
    lines.append("-- 2. Paragens")
    lines.append("DELETE FROM pontos_de_passagem;")
    lines.append("DELETE FROM paragem;")
    lines.append("ALTER SEQUENCE IF EXISTS paragem_id_seq RESTART WITH 1;")

    stop_values = []
    for i, name in enumerate(stop_names, 1):
        z = zones.get(name, 3)
        stop_values.append(f"({i}, '{sql_escape(name)}', {z})")

    chunk_size = 500
    for i in range(0, len(stop_values), chunk_size):
        chunk = stop_values[i:i + chunk_size]
        lines.append("INSERT INTO paragem (id, nome, zona_id) VALUES")
        lines.append(",\n".join(chunk) + ";")

    lines.append(f"SELECT setval('paragem_id_seq', {len(stop_names) + 1});")
    lines.append("")

    # ---- 3. LINHAS ----
    lines.append("-- 3. Linhas")
    lines.append("DELETE FROM pontos_de_passagem;")
    lines.append("DELETE FROM viagem_veiculo;")
    lines.append("DELETE FROM trajeto;")
    lines.append("DELETE FROM linha;")
    lines.append("ALTER SEQUENCE IF EXISTS linha_id_seq RESTART WITH 1;")

    stop_id_map = {name: i for i, name in enumerate(stop_names, 1)}

    linha_id = 1
    trajeto_id = 1
    pdp_id = 1
    all_tragetos = []
    all_pdp = []

    for line_name, route_stops in sorted(named_lines.items()):
        lines.append(
            f"INSERT INTO linha (id, nome, identificador_servico) VALUES "
            f"({linha_id}, 'Linha {sql_escape(line_name)}', '{sql_escape(line_name)}');"
        )

        t = trajeto_id
        lines.append(
            f"INSERT INTO trajeto (id, direcao, linha_id) VALUES "
            f"({t}, 'IDA', {linha_id});"
        )
        for order, stop in enumerate(route_stops, 1):
            pid = stop_id_map.get(stop["Stop_stn"])
            if pid is None:
                continue
            ride_time = stop.get("Ride_time", "00:00:00")
            sub_str = stop.get("sub", "0 days 00:00:00")
            parts = sub_str.replace("0 days ", "").split(":")
            secs = int(parts[0]) * 3600 + int(parts[1]) * 60 + int(parts[2])
            all_pdp.append(f"({pdp_id}, {order}, '{ride_time}', {secs}, {t}, {pid})")
            pdp_id += 1
        trajeto_id += 1

        t = trajeto_id
        lines.append(
            f"INSERT INTO trajeto (id, direcao, linha_id) VALUES "
            f"({t}, 'VOLTA', {linha_id});"
        )
        rev_stops = list(reversed(route_stops))
        for order, stop in enumerate(rev_stops, 1):
            pid = stop_id_map.get(stop["Stop_stn"])
            if pid is None:
                continue
            ride_time = stop.get("Ride_time", "00:00:00")
            sub_str = stop.get("sub", "0 days 00:00:00")
            parts = sub_str.replace("0 days ", "").split(":")
            secs = int(parts[0]) * 3600 + int(parts[1]) * 60 + int(parts[2])
            rev_secs = max(secs - secs, 0)
            all_pdp.append(f"({pdp_id}, {order}, '{ride_time}', {rev_secs}, {t}, {pid})")
            pdp_id += 1
        trajeto_id += 1

        linha_id += 1

    # SER groups - take top groups by number of routes
    ser_sorted = sorted(ser_groups.items(), key=lambda x: -len(x[1]))
    ser_count = 0
    MAX_SER_LINES = 50

    for (origin, dest), route_list in ser_sorted:
        if ser_count >= MAX_SER_LINES:
            break
        if origin == dest:
            continue

        line_name_str = f"SER {origin}-{dest}"
        lines.append(
            f"INSERT INTO linha (id, nome, identificador_servico) VALUES "
            f"({linha_id}, '{sql_escape(line_name_str)}', 'SER_{origin}_{dest}');"
        )

        # Use the longest route as IDA
        longest = max(route_list, key=lambda x: len(x[1]))
        route_stops = longest[1]

        t = trajeto_id
        lines.append(
            f"INSERT INTO trajeto (id, direcao, linha_id) VALUES "
            f"({t}, 'IDA', {linha_id});"
        )
        for order, stop in enumerate(route_stops, 1):
            pid = stop_id_map.get(stop["Stop_stn"])
            if pid is None:
                continue
            sub_str = stop.get("sub", "0 days 00:00:00")
            parts = sub_str.replace("0 days ", "").split(":")
            secs = int(parts[0]) * 3600 + int(parts[1]) * 60 + int(parts[2])
            all_pdp.append(f"({pdp_id}, {order}, '00:00:00', {secs}, {t}, {pid})")
            pdp_id += 1
        trajeto_id += 1

        # VOLTA = reverse
        t = trajeto_id
        lines.append(
            f"INSERT INTO trajeto (id, direcao, linha_id) VALUES "
            f"({t}, 'VOLTA', {linha_id});"
        )
        rev = list(reversed(route_stops))
        for order, stop in enumerate(rev, 1):
            pid = stop_id_map.get(stop["Stop_stn"])
            if pid is None:
                continue
            sub_str = stop.get("sub", "0 days 00:00:00")
            parts = sub_str.replace("0 days ", "").split(":")
            secs = int(parts[0]) * 3600 + int(parts[1]) * 60 + int(parts[2])
            all_pdp.append(f"({pdp_id}, {order}, '00:00:00', {secs}, {t}, {pid})")
            pdp_id += 1
        trajeto_id += 1

        linha_id += 1
        ser_count += 1

    max_linha_id = linha_id
    lines.append(f"SELECT setval('linha_id_seq', {max_linha_id});")
    lines.append(f"SELECT setval('trajeto_id_seq', {trajeto_id});")
    lines.append("")

    # ---- 4. PONTOS DE PASSAGEM ----
    lines.append("-- 4. Pontos de Passagem")
    lines.append("ALTER SEQUENCE IF EXISTS pontos_de_passagem_id_seq RESTART WITH 1;")

    for i in range(0, len(all_pdp), chunk_size):
        chunk = all_pdp[i:i + chunk_size]
        lines.append("INSERT INTO pontos_de_passagem (id, ordem, hora_chegada, tempo_desde_inicio, trajeto_id, paragem_id) VALUES")
        lines.append(",\n".join(chunk) + ";")

    lines.append(f"SELECT setval('pontos_de_passagem_id_seq', {pdp_id});")
    lines.append("")

    # ---- 5. VEICULOS ----
    lines.append("-- 5. Veiculos (Autocarros de demonstracao)")
    lines.append("DELETE FROM viagem_veiculo;")
    lines.append("DELETE FROM autocarro;")
    lines.append("DELETE FROM veiculo;")
    lines.append("ALTER SEQUENCE IF EXISTS veiculo_id_seq RESTART WITH 1;")

    veiculo_id = 1
    autocarro_values = []
    for i in range(1, NUM_VEHICLES + 1):
        mat = f"{i:02d}-NO-{100 + i:02d}"
        seats = 40 + (i % 3) * 10
        occ = 5 + (i * 7) % 30
        lines.append(
            f"INSERT INTO veiculo (id, matricula, n_lugares, lotacao_atual, dtype) VALUES "
            f"({veiculo_id}, '{mat}', {seats}, {occ}, 'Autocarro');"
        )
        autocarro_values.append(f"({veiculo_id})")
        veiculo_id += 1

    lines.append(f"SELECT setval('veiculo_id_seq', {veiculo_id});")
    lines.append("INSERT INTO autocarro (id) VALUES")
    lines.append(",\n".join(autocarro_values) + ";")
    lines.append("")

    # ---- 6. VIAGENS VEICULO ----
    lines.append("-- 6. Viagens Veiculo (demonstracao)")
    lines.append("ALTER SEQUENCE IF EXISTS viagem_veiculo_id_seq RESTART WITH 1;")

    import random
    random.seed(42)
    vv_id = 1
    for i in range(NUM_VIAGENS):
        vid = (i % NUM_VEHICLES) + 1
        tid = (i % (trajeto_id - 1)) + 1
        trip = f"TRIP-{i + 1:04d}"
        lines.append(
            f"INSERT INTO viagem_veiculo (id, trip_id, veiculo_id, trajeto_id) VALUES "
            f"({vv_id}, '{trip}', {vid}, {tid});"
        )
        vv_id += 1

    lines.append(f"SELECT setval('viagem_veiculo_id_seq', {vv_id});")
    lines.append("")

    # ---- 7. TARIFAS ----
    lines.append("-- 7. Tarifas")
    lines.append("DELETE FROM tarifa;")
    lines.append("ALTER SEQUENCE IF EXISTS tarifa_id_seq RESTART WITH 1;")

    tipos = ["ADULTO", "CRIANCA", "ESTUDANTE", "SENIOR"]
    modalidades = ["MENSAL", "ANUAL", "SEMANAL"]

    tarifa_id = 1
    bilhete_precos = {
        "ADULTO": {1: 1.50, 2: 2.20, 3: 3.00},
        "CRIANCA": {1: 0.75, 2: 1.10, 3: 1.50},
        "ESTUDANTE": {1: 1.00, 2: 1.50, 3: 2.00},
        "SENIOR": {1: 0.75, 2: 1.10, 3: 1.50},
    }
    passe_precos = {
        "ADULTO": {"SEMANAL": {1: 12.00, 2: 18.00, 3: 25.00}, "MENSAL": {1: 40.00, 2: 55.00, 3: 70.00}, "ANUAL": {1: 400.00, 2: 550.00, 3: 700.00}},
        "CRIANCA": {"SEMANAL": {1: 6.00, 2: 9.00, 3: 12.50}, "MENSAL": {1: 20.00, 2: 27.50, 3: 35.00}, "ANUAL": {1: 200.00, 2: 275.00, 3: 350.00}},
        "ESTUDANTE": {"SEMANAL": {1: 8.00, 2: 12.00, 3: 16.50}, "MENSAL": {1: 25.00, 2: 35.00, 3: 45.00}, "ANUAL": {1: 250.00, 2: 350.00, 3: 450.00}},
        "SENIOR": {"SEMANAL": {1: 6.00, 2: 9.00, 3: 12.50}, "MENSAL": {1: 20.00, 2: 27.50, 3: 35.00}, "ANUAL": {1: 200.00, 2: 275.00, 3: 350.00}},
    }

    for tipo in tipos:
        for nz in range(1, NUM_ZONES + 1):
            v = bilhete_precos[tipo][nz]
            lines.append(
                f"INSERT INTO tarifa (id, valor, tipo_utilizador, nr_zonas) VALUES "
                f"({tarifa_id}, {v:.2f}, '{tipo}', {nz});"
            )
            tarifa_id += 1

    for tipo in tipos:
        for mod in modalidades:
            for nz in range(1, NUM_ZONES + 1):
                v = passe_precos[tipo][mod][nz]
                lines.append(
                    f"INSERT INTO tarifa (id, valor, tipo_utilizador, modalidade, nr_zonas) VALUES "
                    f"({tarifa_id}, {v:.2f}, '{tipo}', '{mod}', {nz});"
                )
                tarifa_id += 1

    lines.append(f"SELECT setval('tarifa_id_seq', {tarifa_id});")
    lines.append("")

    lines.append("COMMIT;")
    lines.append("")

    return "\n".join(lines)


def main():
    print("=== NoTUB Seed Data Generator ===\n")

    print("Loading stops...")
    stops_fid, stop_names = load_stops()
    print(f"  {len(stop_names)} stops loaded")

    center_stop, center_dists = load_distance_matrix(stop_names)

    zones = assign_zones(stop_names, center_dists)

    print("\nLoading routes...")
    routes = load_routes()
    print(f"  {len(routes)} routes loaded")

    named_lines, ser_groups = group_routes(routes)
    print(f"  Named lines: {len(named_lines)}")
    print(f"  SER groups: {len(ser_groups)}")

    print("\nGenerating SQL...")
    sql = generate_sql(stop_names, stops_fid, zones, named_lines, ser_groups)

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(sql)

    size_mb = os.path.getsize(OUTPUT_FILE) / (1024 * 1024)
    print(f"\nDone! Output: {OUTPUT_FILE} ({size_mb:.1f} MB)")


if __name__ == "__main__":
    main()
