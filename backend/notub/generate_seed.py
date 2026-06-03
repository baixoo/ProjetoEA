"""
Generates seed_data.sql from the Singapore bus dataset (pickle + CSV).
Run:  python generate_seed.py
Output: seed_data.sql (next to this script)
"""
import pickle
import csv
import random
import math
from pathlib import Path

DATASETS_DIR = Path(r"C:\Users\paulo\Desktop\4Ano\2Semestre\pEA\datasets")
PICKLE_PATH = DATASETS_DIR / "BusRoutes.pickle"
STOP_LIST_PATH = DATASETS_DIR / "BusStopList.csv"
OUTPUT_PATH = Path(__file__).parent / "src" / "main" / "resources" / "data.sql"

random.seed(42)

NUM_ZONES = 4
AUTOCARROS_PER_LINE = 2
SG_LAT_MIN, SG_LAT_MAX = 1.25, 1.45
SG_LON_MIN, SG_LON_MAX = 103.65, 103.90

ZONE_NAMES = [
    "Zona Norte",
    "Zona Centro",
    "Zona Sul",
    "Zona Oeste",
]

LINE_FRIENDLY_NAMES = {
    "SER_52f1": "Linha 1",
    "SER_61a2": "Linha 2",
    "SER_83ea": "Linha 3",
    "SER_376e": "Linha 4",
    "SER_1253": "Linha 5",
    "SER_eef7": "Linha 6",
    "SER_f0cb": "Linha 7",
    "SER_79d6": "Linha 8",
    "SER_f5ca": "Linha 9",
    "SER_d4ee": "Linha 10",
    "SER_50bb": "Linha 11",
    "SER_dccb": "Linha 12",
    "SER_c837": "Linha 13",
    "SER_8d27": "Linha 14",
    "SER_3068": "Linha 15",
    "SER_16b3": "Linha 16",
    "SER_7688": "Linha 17",
    "SER_eb19": "Linha 18",
    "SER_92a6": "Linha 19",
    "SER_5538": "Linha 20",
    "SER_c791": "Linha 21",
    "SER_4568": "Linha 22",
    "SER_cc8e": "Linha 23",
    "SER_7b1a": "Linha 24",
    "SER_eb3b": "Linha 25",
    "SER_b8ae": "Linha 26",
    "SER_3346": "Linha 27",
    "SER_2397": "Linha 28",
    "SER_ff5a": "Linha 29",
    "SER_0e65": "Linha 30",
    "SER_533e": "Linha 31",
    "SER_0849": "Linha 32",
}


def load_pickle():
    with open(PICKLE_PATH, "rb") as f:
        return pickle.load(f)


def load_stop_ids():
    stops = []
    with open(STOP_LIST_PATH, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            stops.append(row["BUS_STOP"].strip())
    return stops


def generate_lat_lon(stop_code, all_stops_list):
    idx = all_stops_list.index(stop_code) if stop_code in all_stops_list else random.randint(0, len(all_stops_list))
    total = max(len(all_stops_list), 1)
    t = idx / total
    lat = SG_LAT_MIN + t * (SG_LAT_MAX - SG_LAT_MIN)
    lon = SG_LON_MIN + t * (SG_LON_MAX - SG_LON_MIN)
    lat += random.uniform(-0.005, 0.005)
    lon += random.uniform(-0.005, 0.005)
    return round(lat, 6), round(lon, 6)


def assign_zone(stop_code, all_stops_list):
    idx = all_stops_list.index(stop_code) if stop_code in all_stops_list else 0
    return (idx % NUM_ZONES) + 1


def parse_cumulative_minutes(timedelta_str):
    parts = timedelta_str.split(" ")
    time_part = parts[-1]
    h, m, s = time_part.split(":")
    return int(h) * 60 + int(m) + int(s) // 60


def parse_ride_time_minutes(time_str):
    h, m, s = time_str.split(":")
    return int(h) * 60 + int(m)


def main():
    print("Loading pickle...")
    routes = load_pickle()
    print(f"  Found {len(routes)} services: {list(routes.keys())}")

    print("Loading stop list...")
    all_stops = load_stop_ids()
    print(f"  Found {len(all_stops)} stops")

    lines = []
    lines.append("-- NoTUB Seed Data (auto-generated from Singapore bus dataset)")
    lines.append("-- DO NOT EDIT MANUALLY - regenerate with: python generate_seed.py")
    lines.append("")
    lines.append("BEGIN;")
    lines.append("")

    # ── Zonas ──
    lines.append("-- ═══ Zonas ═══")
    for i, name in enumerate(ZONE_NAMES):
        zid = i + 1
        lines.append(f"INSERT INTO zona (id, num, nome) VALUES ({zid}, {zid}, '{name}') ON CONFLICT (id) DO NOTHING;")
    lines.append("")

    # ── Collect all unique stops from routes ──
    all_route_stops = set()
    clean_routes = {}
    for svc, df in routes.items():
        stops_in_route = []
        for _, row in df.iterrows():
            stop = str(row["Stop_stn"]).strip()
            if not stop or " " in stop:
                continue
            stops_in_route.append({
                "stop": stop,
                "ride_time": str(row["Ride_time"]),
                "sub": str(row["sub"]),
            })
            all_route_stops.add(stop)
        if stops_in_route:
            clean_routes[svc] = stops_in_route

    sorted_stops = sorted(all_route_stops)

    # ── Paragens ──
    lines.append("-- ═══ Paragens ═══")
    stop_id_map = {}
    for idx, stop in enumerate(sorted_stops):
        pid = idx + 1
        stop_id_map[stop] = pid
        lat, lon = generate_lat_lon(stop, all_stops)
        zid = assign_zone(stop, sorted_stops)
        lines.append(
            f"INSERT INTO paragem (id, nome, latitude, longitude, zona_id) "
            f"VALUES ({pid}, '{stop}', {lat}, {lon}, {zid}) ON CONFLICT (id) DO NOTHING;"
        )
    lines.append("")

    # ── Linhas + Trajetos + PontosDePassagem ──
    lines.append("-- ═══ Linhas ═══")
    linha_id = 0
    trajeto_id = 0
    pdp_id = 0
    autocarro_id = 0

    for svc in sorted(clean_routes.keys()):
        route = clean_routes[svc]
        linha_id += 1
        friendly = LINE_FRIENDLY_NAMES.get(svc, f"Linha {linha_id}")
        lines.append(
            f"INSERT INTO linha (id, nome, identificador_servico) "
            f"VALUES ({linha_id}, '{friendly}', '{svc}') ON CONFLICT (id) DO NOTHING;"
        )

        # IDA
        trajeto_id += 1
        ida_trajeto_id = trajeto_id
        lines.append(
            f"INSERT INTO trajeto (id, direcao, linha_id) "
            f"VALUES ({trajeto_id}, 'IDA', {linha_id}) ON CONFLICT (id) DO NOTHING;"
        )

        base_minutes = parse_ride_time_minutes(route[0]["ride_time"])

        for ordem, stop_data in enumerate(route):
            pdp_id += 1
            stop = stop_data["stop"]
            paragem_fk = stop_id_map.get(stop)
            if paragem_fk is None:
                continue
            ride_time = stop_data["ride_time"]
            cumulative = parse_cumulative_minutes(stop_data["sub"])
            hora_chegada = ride_time
            lines.append(
            f"INSERT INTO pontos_de_passagem (id, ordem, hora_chegada, tempo_desde_inicio, trajeto_id, paragem_id) "
            f"VALUES ({pdp_id}, {ordem}, '{hora_chegada}', {cumulative}, {ida_trajeto_id}, {paragem_fk}) ON CONFLICT (id) DO NOTHING;"
            )

        # VOLTA (reversed stops)
        trajeto_id += 1
        vol_trajeto_id = trajeto_id
        lines.append(
            f"INSERT INTO trajeto (id, direcao, linha_id) "
            f"VALUES ({trajeto_id}, 'VOLTA', {linha_id}) ON CONFLICT (id) DO NOTHING;"
        )

        reversed_route = list(reversed(route))
        for ordem, stop_data in enumerate(reversed_route):
            pdp_id += 1
            stop = stop_data["stop"]
            paragem_fk = stop_id_map.get(stop)
            if paragem_fk is None:
                continue
            ride_time = stop_data["ride_time"]
            cumulative = parse_cumulative_minutes(stop_data["sub"])
            total_time = parse_cumulative_minutes(route[-1]["sub"])
            volta_cumulative = total_time - cumulative
            if volta_cumulative < 0:
                volta_cumulative = 0
            h = 8 + volta_cumulative // 60
            m = volta_cumulative % 60
            hora_chegada = f"{h:02d}:{m:02d}:00"
            lines.append(
            f"INSERT INTO pontos_de_passagem (id, ordem, hora_chegada, tempo_desde_inicio, trajeto_id, paragem_id) "
            f"VALUES ({pdp_id}, {ordem}, '{hora_chegada}', {volta_cumulative}, {vol_trajeto_id}, {paragem_fk}) ON CONFLICT (id) DO NOTHING;"
            )

        # Autocarros
        for i in range(AUTOCARROS_PER_LINE):
            autocarro_id += 1
            code = svc.split("_")[1][:3].upper() if "_" in svc else svc[:3].upper()
            matricula = f"SG-{code}-{autocarro_id:02d}"
            nlugares = random.choice([40, 50, 60])
            lines.append(
                f"INSERT INTO veiculo (id, matricula, n_lugares, lotacao_atual, dtype) "
                f"VALUES ({autocarro_id}, '{matricula}', {nlugares}, 0, 'Autocarro') ON CONFLICT (id) DO NOTHING;"
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
    lines.append(f"SELECT setval('zona_id_seq', {NUM_ZONES});")
    lines.append(f"SELECT setval('paragem_id_seq', {len(sorted_stops)});")
    lines.append(f"SELECT setval('linha_id_seq', {linha_id});")
    lines.append(f"SELECT setval('trajeto_id_seq', {trajeto_id});")
    lines.append(f"SELECT setval('pontos_de_passagem_id_seq', {pdp_id});")
    lines.append(f"SELECT setval('veiculo_id_seq', {autocarro_id});")
    lines.append(f"SELECT setval('tarifa_id_seq', {tarifa_id});")
    lines.append("")

    lines.append("COMMIT;")
    lines.append("")

    output = "\n".join(lines)
    OUTPUT_PATH.write_text(output, encoding="utf-8")
    print(f"\nDone! Written to {OUTPUT_PATH}")
    print(f"  Zones:      {NUM_ZONES}")
    print(f"  Paragens:   {len(sorted_stops)}")
    print(f"  Linhas:     {linha_id}")
    print(f"  Trajetos:   {trajeto_id}")
    print(f"  Pontos:     {pdp_id}")
    print(f"  Autocarros: {autocarro_id}")
    print(f"  Tarifas:    {tarifa_id}")


if __name__ == "__main__":
    main()
