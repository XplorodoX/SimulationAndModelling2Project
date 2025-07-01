import redis
import datetime
import time  # Import time for sleeping


def get_event_at_timestamp(r_conn, key, timestamp):
    """
    Ruft Ereignisse zu einem bestimmten Zeitpunkt aus einem Redis Sorted Set ab.
    Such nach exaktem Treffer und, falls nicht vorhanden, den nächstgegelegen Wert.
    """
    dt = datetime.datetime.fromtimestamp(timestamp)
    print(f"\nSuche nach Preisdaten zum Zeitpunkt: {dt.strftime('%Y-%m-%d %H:%M:%S')}")

    # Suche nach exaktem Zeitstempel
    # zrangebyscore returns (member, score) tuples
    exact_match = r_conn.zrangebyscore(key, timestamp, timestamp, withscores=True)

    if exact_match:
        print("Exakter Treffer gefunden.")
        return exact_match

    # Falls kein exakter Treffer, suche den nächstgelegenen Wert
    print("Kein exakter Treffer gefunden. Suche nächstgelegenen Wert...")

    # Suche den nächsten Wert nach dem Zeitstempel (inclusive timestamp)
    # start=0, num=1 gets the first element
    next_values = r_conn.zrangebyscore(key, timestamp, '+inf', withscores=True, start=0, num=1)

    # Suche den vorherigen Wert vor dem Zeitstempel (inclusive timestamp)
    # zrevrangebyscore searches in reverse order, so we need to adjust score range and limits
    prev_values = r_conn.zrevrangebyscore(key, timestamp, '-inf', withscores=True, start=0, num=1)

    candidates = []
    if next_values:
        candidates.append(next_values[0])
    if prev_values:
        candidates.append(prev_values[0])

    if not candidates:
        return []

    # Wähle den zeitlich nächstgelegenen Wert
    # The key for min function is the absolute difference between the candidate's score (timestamp) and the target timestamp
    closest = min(candidates, key=lambda x: abs(x[1] - timestamp))
    return [closest]


if __name__ == "__main__":
    try:
        r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)
        r.ping()
        print("Erfolgreich mit Redis verbunden.")
    except redis.exceptions.ConnectionError as e:
        print(f"Verbindung zu Redis fehlgeschlagen: {e}")
        exit()

    # --- Definieren Sie hier den Schlüssel für Ihre Zeitreihen-Daten ---
    # Dieses wird ein Redis Sorted Set sein, in dem die Zeitstempel die Scores sind.
    PRICE_TIMESERIES_KEY = "price_events"

    # --- Datenpopulation (nur zur Demonstration) ---
    # Stellen Sie sicher, dass Sie relevante Daten in Ihr Sorted Set einfügen.
    # Hier werden einige Beispieldaten für den 1. Januar 2016 eingefügt.
    print(f"\nLösche bestehende Daten für '{PRICE_TIMESERIES_KEY}'...")
    r.delete(PRICE_TIMESERIES_KEY)
    print("Füge Beispieldaten in Redis Sorted Set ein...")

    base_time = datetime.datetime(2016, 1, 1, 0, 0, 0)  # Start from 2016-01-01 00:00:00

    # Add data every minute for an hour
    for i in range(60):  # 60 minutes
        current_dt = base_time + datetime.timedelta(minutes=i)
        current_ts = int(current_dt.timestamp())
        price_value = 100 + i * 0.5 + (i % 5)  # Example price calculation
        # Member should be a string, here we store "price:value"
        r.zadd(PRICE_TIMESERIES_KEY, {f"price:{price_value:.2f}": current_ts})

    # Add a few more data points outside the main range for testing proximity
    r.zadd(PRICE_TIMESERIES_KEY, {f"price:150.00": int((base_time + datetime.timedelta(minutes=65)).timestamp())})
    r.zadd(PRICE_TIMESERIES_KEY, {f"price:90.00": int((base_time - datetime.timedelta(minutes=5)).timestamp())})

    print(f"Es wurden {r.zcard(PRICE_TIMESERIES_KEY)} Einträge hinzugefügt.")
    print(f"Erster Eintrag im Sorted Set: {r.zrange(PRICE_TIMESERIES_KEY, 0, 0, withscores=True)}")
    print(f"Letzter Eintrag im Sorted Set: {r.zrange(PRICE_TIMESERIES_KEY, -1, -1, withscores=True)}")

    # --- Testfälle für die Suche ---

    # 1. Testfall: Exakter Treffer
    target_datetime_exact = datetime.datetime(2016, 1, 1, 0, 30, 0)  # An existing timestamp
    target_ts_exact = int(target_datetime_exact.timestamp())
    print("\n--- Testfall 1: Exakter Treffer ---")
    events_exact = get_event_at_timestamp(r, PRICE_TIMESERIES_KEY, target_ts_exact)
    if events_exact:
        print(f"Gefundene Preisdaten für {target_datetime_exact.strftime('%Y-%m-%d %H:%M:%S')}:")
        for member, score in events_exact:
            price_value = member.split(':')[1]  # Extract the numeric price value
            event_time = datetime.datetime.fromtimestamp(score)
            time_diff = abs(score - target_ts_exact)
            print(
                f"- Preis: {price_value}, Zeit: {event_time.strftime('%Y-%m-%d %H:%M:%S')}, Abweichung: {time_diff} Sekunden")
    else:
        print("Keine Preisdaten gefunden für diesen Zeitpunkt.")

    # 2. Testfall: Nächstgelegener Treffer (zwischen zwei Datenpunkten)
    target_datetime_between = datetime.datetime(2016, 1, 1, 0, 30, 45)  # Not an exact minute mark
    target_ts_between = int(target_datetime_between.timestamp())
    print("\n--- Testfall 2: Nächstgelegener Treffer ---")
    events_between = get_event_at_timestamp(r, PRICE_TIMESERIES_KEY, target_ts_between)
    if events_between:
        print(f"Gefundene Preisdaten für {target_datetime_between.strftime('%Y-%m-%d %H:%M:%S')}:")
        for member, score in events_between:
            price_value = member.split(':')[1]
            event_time = datetime.datetime.fromtimestamp(score)
            time_diff = abs(score - target_ts_between)
            print(
                f"- Preis: {price_value}, Zeit: {event_time.strftime('%Y-%m-%d %H:%M:%S')}, Abweichung: {time_diff} Sekunden")
    else:
        print("Keine Preisdaten gefunden für diesen Zeitpunkt.")

    # 3. Testfall: Vor dem ersten Datenpunkt
    target_datetime_before_first = datetime.datetime(2015, 12, 31, 23, 58, 0)
    target_ts_before_first = int(target_datetime_before_first.timestamp())
    print("\n--- Testfall 3: Vor dem ersten Datenpunkt ---")
    events_before_first = get_event_at_timestamp(r, PRICE_TIMESERIES_KEY, target_ts_before_first)
    if events_before_first:
        print(f"Gefundene Preisdaten für {target_datetime_before_first.strftime('%Y-%m-%d %H:%M:%S')}:")
        for member, score in events_before_first:
            price_value = member.split(':')[1]
            event_time = datetime.datetime.fromtimestamp(score)
            time_diff = abs(score - target_ts_before_first)
            print(
                f"- Preis: {price_value}, Zeit: {event_time.strftime('%Y-%m-%d %H:%M:%S')}, Abweichung: {time_diff} Sekunden")
    else:
        print("Keine Preisdaten gefunden für diesen Zeitpunkt.")

    # 4. Testfall: Nach dem letzten Datenpunkt
    target_datetime_after_last = datetime.datetime(2016, 1, 1, 1, 10, 0)
    target_ts_after_last = int(target_datetime_after_last.timestamp())
    print("\n--- Testfall 4: Nach dem letzten Datenpunkt ---")
    events_after_last = get_event_at_timestamp(r, PRICE_TIMESERIES_KEY, target_ts_after_last)
    if events_after_last:
        print(f"Gefundene Preisdaten für {target_datetime_after_last.strftime('%Y-%m-%d %H:%M:%S')}:")
        for member, score in events_after_last:
            price_value = member.split(':')[1]
            event_time = datetime.datetime.fromtimestamp(score)
            time_diff = abs(score - target_ts_after_last)
            print(
                f"- Preis: {price_value}, Zeit: {event_time.strftime('%Y-%m-%d %H:%M:%S')}, Abweichung: {time_diff} Sekunden")
    else:
        print("Keine Preisdaten gefunden für diesen Zeitpunkt.")

    # --- Your original target time ---
    target_datetime_original = datetime.datetime(2016, 1, 1, 1, 0, 0)
    target_ts_original = int(target_datetime_original.timestamp())
    print("\n--- Ihr ursprünglicher Zielzeitpunkt ---")
    events_original = get_event_at_timestamp(r, PRICE_TIMESERIES_KEY, target_ts_original)

    if events_original:
        print(f"Gefundene Preisdaten:")
        for member, score in events_original:
            price_value = member.split(':')[1]  # Extract the numeric price value
            event_time = datetime.datetime.fromtimestamp(score)
            time_diff = abs(score - target_ts_original)
            print(
                f"- Preis: {price_value}, Zeit: {event_time.strftime('%Y-%m-%d %H:%M:%S')}, Abweichung: {time_diff} Sekunden")
    else:
        print("Keine Preisdaten gefunden.")