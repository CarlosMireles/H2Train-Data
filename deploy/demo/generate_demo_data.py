#!/usr/bin/env python3
"""Genera eventos sintéticos deterministas para demostrar la API de datasets."""

from __future__ import annotations

import argparse
import json
import os
import random
import shutil
import stat
from collections import defaultdict
from dataclasses import asdict, dataclass
from datetime import date, datetime, time, timedelta, timezone
from pathlib import Path
from typing import Any, Iterable


@dataclass(frozen=True)
class DemoProfile:
    user_id: str
    calories: int
    steps: int
    sleep_hours: float
    workouts_per_week: int
    run_km_per_week: float
    calories_ingested: int
    water_ml: int
    glucose: int
    weight_kg: float
    sparse: bool = False


PROFILES = (
    DemoProfile("0f3a2c71-8d4e-4b96-a125-6e7f9c2d1b40", 2850, 8500, 7.2, 3, 15, 2400, 2200, 92, 72.0),
    DemoProfile("18c7e5a4-2b91-4d63-8f20-a6e4c9b17d52", 2250, 12500, 7.1, 4, 22, 2250, 2500, 88, 68.0),
    DemoProfile("2b64d8f1-73a5-49ce-b812-5f90a3d6e247", 2200, 7200, 5.3, 2, 8, 2100, 1800, 101, 84.0),
    DemoProfile("3d91a6c8-5e27-4b40-9f13-c7a2e8d56401", 2650, 9800, 6.8, 6, 18, 2600, 2600, 95, 76.0),
    DemoProfile("4e28b7d5-91c3-46fa-a804-2d6f9b13e750", 2750, 11200, 7.4, 5, 40, 2750, 3000, 90, 70.0),
    DemoProfile("5f73c9a2-4d18-48b6-927e-e1a650d83c24", 3100, 14500, 5.7, 6, 45, 2950, 3200, 97, 79.0),
    DemoProfile("6a19e4b7-82d5-43cf-b601-9e2c7f58a340", 1950, 5500, 8.0, 2, 5, 1900, 1600, 86, 63.0),
    DemoProfile("7c46f1d9-30a8-4e72-95b3-d8f2146a0c57", 2400, 11800, 5.5, 4, 25, 2300, 2400, 104, 81.0),
    DemoProfile("8d25a7e3-64b9-41f8-a730-3c9e5d12b684", 2900, 9000, 6.3, 3, 12, 2700, 2100, 99, 88.0),
    DemoProfile("9e81c4f6-27d3-4a59-b142-f6a0387d25c1", 2600, 10500, 7.0, 5, 12, 2500, 2800, 91, 74.0),
    DemoProfile("a4d62b18-95e7-438c-860f-1b73e9c5402d", 2300, 7800, 6.9, 3, 10, 2200, 1900, 108, 93.0, sparse=True),
    DemoProfile("b7f30e65-1c84-47a2-9d56-4e8b21f693ac", 2450, 9900, 7.6, 4, 28, 2350, 2300, 89, 66.0),
)

EXPECTED_MATCHES = {
    "daily_calories_avg_gt_2500": [
        "0f3a2c71-8d4e-4b96-a125-6e7f9c2d1b40",
        "3d91a6c8-5e27-4b40-9f13-c7a2e8d56401",
        "4e28b7d5-91c3-46fa-a804-2d6f9b13e750",
        "5f73c9a2-4d18-48b6-927e-e1a650d83c24",
        "8d25a7e3-64b9-41f8-a730-3c9e5d12b684",
        "9e81c4f6-27d3-4a59-b142-f6a0387d25c1",
    ],
    "daily_steps_avg_gt_10000": [
        "18c7e5a4-2b91-4d63-8f20-a6e4c9b17d52",
        "4e28b7d5-91c3-46fa-a804-2d6f9b13e750",
        "5f73c9a2-4d18-48b6-927e-e1a650d83c24",
        "7c46f1d9-30a8-4e72-95b3-d8f2146a0c57",
        "9e81c4f6-27d3-4a59-b142-f6a0387d25c1",
    ],
    "daily_sleep_duration_avg_lt_21600": [
        "2b64d8f1-73a5-49ce-b812-5f90a3d6e247",
        "5f73c9a2-4d18-48b6-927e-e1a650d83c24",
        "7c46f1d9-30a8-4e72-95b3-d8f2146a0c57",
    ],
    "weekly_activity_count_avg_gt_4": [
        "3d91a6c8-5e27-4b40-9f13-c7a2e8d56401",
        "4e28b7d5-91c3-46fa-a804-2d6f9b13e750",
        "5f73c9a2-4d18-48b6-927e-e1a650d83c24",
        "9e81c4f6-27d3-4a59-b142-f6a0387d25c1",
    ],
    "weekly_running_distance_avg_gt_30000": [
        "4e28b7d5-91c3-46fa-a804-2d6f9b13e750",
        "5f73c9a2-4d18-48b6-927e-e1a650d83c24",
    ],
}

UTC = timezone.utc
WORKOUT_DAY_OFFSETS = (0, 1, 2, 4, 5, 6)
HEART_RATE_ZONES = (
    ("out_of_range", 12, 35),
    ("fat_burn", 24, 150),
    ("cardio", 18, 190),
    ("peak", 6, 95),
)


def daterange(start: date, end: date) -> Iterable[date]:
    current = start
    while current <= end:
        yield current
        current += timedelta(days=1)


def at_utc(day: date, hour: int, minute: int = 0) -> datetime:
    return datetime.combine(day, time(hour, minute), tzinfo=UTC)


def iso(value: datetime) -> str:
    return value.isoformat().replace("+00:00", "Z")


def envelope(
    profile: DemoProfile,
    provider: str,
    event_type: str,
    event_name: str,
    event_id: str,
    timestamp: datetime,
    fields: dict[str, Any],
) -> dict[str, Any]:
    event = {
        "athleteId": f"athlete-{profile.user_id}",
        "eventType": event_type,
        "eventName": event_name,
        "eventId": event_id,
        "timestamp": iso(timestamp),
        "sourceSystem": provider,
        **fields,
    }
    return {
        "messageId": f"demo-message-{event_id}",
        "schemaVersion": "1",
        "publishedAt": iso(timestamp + timedelta(minutes=2)),
        "userId": profile.user_id,
        "providerId": provider,
        "athleteId": f"athlete-{profile.user_id}",
        "eventType": event_type,
        "eventName": event_name,
        "eventId": event_id,
        "event": event,
    }


def jitter(rng: random.Random, base: float, spread: float, digits: int = 0) -> float | int:
    value = base + rng.uniform(-spread, spread)
    return round(value, digits) if digits else round(value)


def available_day(profile: DemoProfile, day: date, profile_index: int, divisor: int) -> bool:
    if profile.sparse and day.weekday() not in (0, 2, 4):
        return False
    return (day.toordinal() + profile_index * 7) % divisor != 0


def generate_daily_events(
    profile: DemoProfile,
    profile_index: int,
    start: date,
    end: date,
    rng: random.Random,
) -> list[dict[str, Any]]:
    events: list[dict[str, Any]] = []
    profile_timestamp = at_utc(start, 6)
    events.append(envelope(
        profile,
        "fitbit",
        "USER_STATE",
        "UserProfile",
        f"{profile.user_id}-profile",
        profile_timestamp,
        {
            "timezone": "Europe/Madrid",
            "gender": "female" if profile_index % 2 == 0 else "male",
            "height": 160 + profile_index * 2,
            "weight": profile.weight_kg,
        },
    ))

    for day in daterange(start, end):
        compact_date = day.strftime("%Y%m%d")
        if available_day(profile, day, profile_index, 31):
            steps = int(jitter(rng, profile.steps, 420))
            calories = int(jitter(rng, profile.calories, 75))
            events.append(envelope(
                profile,
                "fitbit",
                "ACTIVITY",
                "ActivitySummary",
                f"{profile.user_id}-summary-{compact_date}",
                at_utc(day, 18),
                {
                    "steps": steps,
                    "distanceMeters": round(steps * 0.78, 2),
                    "caloriesOut": calories,
                    "date": day.isoformat(),
                },
            ))

        if day.weekday() != 6 and available_day(profile, day, profile_index, 29):
            sleep_hours = float(jitter(rng, profile.sleep_hours, 0.22, 2))
            sleep_start = at_utc(day, 22)
            events.append(envelope(
                profile,
                "fitbit",
                "HEALTH",
                "Sleep",
                f"{profile.user_id}-sleep-{compact_date}",
                sleep_start,
                {
                    "startTime": iso(sleep_start),
                    "duration": round(sleep_hours * 3600),
                },
            ))

        if day.weekday() in (0, 2, 4, 6) and available_day(profile, day, profile_index, 23):
            events.append(envelope(
                profile,
                "fitbit",
                "BODY_COMPOSITION",
                "Nutrition",
                f"{profile.user_id}-nutrition-{compact_date}",
                at_utc(day, 20),
                {
                    "calories": int(jitter(rng, profile.calories_ingested, 90)),
                    "water": int(jitter(rng, profile.water_ml, 120)),
                },
            ))

        if day.weekday() in (1, 4):
            events.append(envelope(
                profile,
                "fitbit",
                "HEALTH",
                "BloodGlucose",
                f"{profile.user_id}-glucose-{compact_date}",
                at_utc(day, 8),
                {"averageGlucose": int(jitter(rng, profile.glucose, 5))},
            ))

        if day.day == 1:
            weight = float(jitter(rng, profile.weight_kg, 0.8, 1))
            height_m = (160 + profile_index * 2) / 100
            events.append(envelope(
                profile,
                "fitbit",
                "BODY_COMPOSITION",
                "BodyComposition",
                f"{profile.user_id}-body-{compact_date}",
                at_utc(day, 7),
                {
                    "weight": weight,
                    "bodyMassIndex": round(weight / (height_m * height_m), 1),
                    "bodyFatPercentage": round(18 + profile_index * 0.8, 1),
                },
            ))

    final_timestamp = at_utc(end, 23, 59)
    events.append(envelope(
        profile,
        "fitbit",
        "USER_STATE",
        "UserProfile",
        f"{profile.user_id}-profile-final",
        final_timestamp,
        {
            "timezone": "Europe/Madrid",
            "gender": "female" if profile_index % 2 == 0 else "male",
            "height": 160 + profile_index * 2,
            "weight": profile.weight_kg,
        },
    ))
    return events


def first_complete_monday(start: date) -> date:
    days_until_monday = (7 - start.weekday()) % 7
    return start + timedelta(days=days_until_monday)


def workout_types(profile: DemoProfile) -> list[str]:
    run_count = min(
        profile.workouts_per_week,
        4 if profile.run_km_per_week >= 30 else 2,
    )
    remaining = profile.workouts_per_week - run_count
    return ["run"] * run_count + ["ride", "strength", "walk", "ride"][:remaining]


def generate_workout_events(
    profile: DemoProfile,
    start: date,
    end: date,
    rng: random.Random,
) -> list[dict[str, Any]]:
    events: list[dict[str, Any]] = []
    monday = first_complete_monday(start)
    types = workout_types(profile)
    run_count = types.count("run")
    while monday + timedelta(days=6) <= end:
        for workout_index, activity_type in enumerate(types):
            day = monday + timedelta(days=WORKOUT_DAY_OFFSETS[workout_index])
            timestamp = at_utc(day, 7, 15)
            compact_date = day.strftime("%Y%m%d")
            if activity_type == "run":
                distance = profile.run_km_per_week * 1000 / run_count
                duration = distance / 2.8
            elif activity_type == "ride":
                distance = 22000 + rng.randint(-1500, 1500)
                duration = 3600 + rng.randint(-240, 240)
            elif activity_type == "walk":
                distance = 5000 + rng.randint(-500, 500)
                duration = 4200 + rng.randint(-300, 300)
            else:
                distance = 0
                duration = 2700 + rng.randint(-180, 180)
            event_id = f"{profile.user_id}-workout-{compact_date}-{workout_index}"
            events.append(envelope(
                profile,
                "strava",
                "ACTIVITY",
                "Workout",
                event_id,
                timestamp,
                {
                    "activityType": activity_type,
                    "startTime": iso(timestamp),
                    "endTime": iso(timestamp + timedelta(seconds=round(duration))),
                    "duration": round(duration),
                    "distanceMeters": round(distance, 2),
                    "calories": round(duration / 8.5),
                },
            ))

        heart_rate_day = monday
        for zone, minutes, calories in HEART_RATE_ZONES:
            timestamp = at_utc(heart_rate_day, 8)
            events.append(envelope(
                profile,
                "fitbit",
                "PHYSIOLOGICAL",
                "HeartRate",
                f"{profile.user_id}-heart-{heart_rate_day:%Y%m%d}-{zone}",
                timestamp,
                {
                    "zone": zone,
                    "minutes": minutes + (profile.workouts_per_week // 2),
                    "calories": calories + profile.workouts_per_week * 3,
                },
            ))
        monday += timedelta(weeks=1)
    return events


def generate_events(
    start: date,
    end: date,
    seed: int,
    profiles: tuple[DemoProfile, ...] = PROFILES,
) -> list[dict[str, Any]]:
    if start > end:
        raise ValueError("La fecha inicial no puede ser posterior a la fecha final")
    events: list[dict[str, Any]] = []
    for index, profile in enumerate(profiles):
        rng = random.Random(seed + index * 1009)
        events.extend(generate_daily_events(profile, index, start, end, rng))
        events.extend(generate_workout_events(profile, start, end, rng))
    events.sort(key=lambda item: (
        item["event"]["timestamp"],
        item["userId"],
        item["eventId"],
    ))
    validate_unique_ids(events)
    return events


def validate_unique_ids(events: list[dict[str, Any]]) -> None:
    event_ids = [event["eventId"] for event in events]
    message_ids = [event["messageId"] for event in events]
    if len(event_ids) != len(set(event_ids)):
        raise ValueError("Se han generado identificadores de evento duplicados")
    if len(message_ids) != len(set(message_ids)):
        raise ValueError("Se han generado identificadores de mensaje duplicados")


def average(values: list[float]) -> float:
    return sum(values) / len(values) if values else 0.0


def actual_matches(events: list[dict[str, Any]]) -> dict[str, list[str]]:
    calories: dict[str, list[float]] = defaultdict(list)
    steps: dict[str, list[float]] = defaultdict(list)
    sleep: dict[str, list[float]] = defaultdict(list)
    workout_count: dict[tuple[str, date], float] = defaultdict(float)
    running_distance: dict[tuple[str, date], float] = defaultdict(float)

    for item in events:
        user_id = item["userId"]
        event_name = item["eventName"]
        payload = item["event"]
        timestamp = datetime.fromisoformat(payload["timestamp"].replace("Z", "+00:00"))
        day = timestamp.date()
        week = day - timedelta(days=day.weekday())
        if event_name == "ActivitySummary":
            calories[user_id].append(float(payload["caloriesOut"]))
            steps[user_id].append(float(payload["steps"]))
        elif event_name == "Sleep":
            sleep[user_id].append(float(payload["duration"]))
        elif event_name == "Workout":
            workout_count[(user_id, week)] += 1
            if payload["activityType"] == "run":
                running_distance[(user_id, week)] += float(payload["distanceMeters"])

    users = sorted({event["userId"] for event in events})
    activity_by_user: dict[str, list[float]] = defaultdict(list)
    run_by_user: dict[str, list[float]] = defaultdict(list)
    for (user_id, _), value in workout_count.items():
        activity_by_user[user_id].append(value)
    for (user_id, _), value in running_distance.items():
        run_by_user[user_id].append(value)

    return {
        "daily_calories_avg_gt_2500": [
            user for user in users if average(calories[user]) > 2500
        ],
        "daily_steps_avg_gt_10000": [
            user for user in users if average(steps[user]) > 10000
        ],
        "daily_sleep_duration_avg_lt_21600": [
            user for user in users if average(sleep[user]) < 21600
        ],
        "weekly_activity_count_avg_gt_4": [
            user for user in users if average(activity_by_user[user]) > 4
        ],
        "weekly_running_distance_avg_gt_30000": [
            user for user in users if average(run_by_user[user]) > 30000
        ],
    }


def validate_scenarios(events: list[dict[str, Any]]) -> None:
    matches = actual_matches(events)
    if matches != EXPECTED_MATCHES:
        raise ValueError(
            "Los escenarios generados no producen los resultados esperados: "
            + json.dumps(matches, sort_keys=True)
        )


def reset_demo_data(root: Path) -> None:
    for child_name in ("events", "datamarts"):
        child = root / child_name
        if child.exists():
            shutil.rmtree(child)
    root.mkdir(parents=True, exist_ok=True)


def write_events(root: Path, events: list[dict[str, Any]]) -> None:
    grouped: dict[tuple[str, str], list[dict[str, Any]]] = defaultdict(list)
    for event in events:
        grouped[(event["providerId"], event["eventType"])].append(event)
    for (provider, event_type), group in sorted(grouped.items()):
        target = root / "events" / provider / event_type / "events.jsonl"
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("w", encoding="utf-8", newline="\n") as output:
            for event in group:
                output.write(json.dumps(event, separators=(",", ":"), sort_keys=True))
                output.write("\n")


def write_manifest(
    root: Path,
    events: list[dict[str, Any]],
    start: date,
    end: date,
    seed: int,
) -> Path:
    manifest = {
        "dataset": "h2train-demo",
        "description": "Datos sintéticos deterministas para demostrar la API de datasets.",
        "startDate": start.isoformat(),
        "endDate": end.isoformat(),
        "seed": seed,
        "subjectCount": len(PROFILES),
        "eventCount": len(events),
        "subjects": [asdict(profile) for profile in PROFILES],
        "expectedMatches": EXPECTED_MATCHES,
    }
    target = root.parent / "demo" / "manifest.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(
        json.dumps(manifest, indent=2, sort_keys=True, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    return target


def make_demo_storage_writable(storage_root: Path) -> None:
    paths = [storage_root, *storage_root.rglob("*")]
    for path in paths:
        mode = stat.S_IRUSR | stat.S_IWUSR | stat.S_IRGRP | stat.S_IWGRP | stat.S_IROTH | stat.S_IWOTH
        if path.is_dir():
            mode |= stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH
        try:
            path.chmod(mode)
        except OSError:
            pass


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(os.getenv("DEMO_DATALAKE_ROOT", "/var/lib/h2train/datalake")),
        help="Raíz aislada del datalake de demostración.",
    )
    parser.add_argument(
        "--start-date",
        type=date.fromisoformat,
        default=date.fromisoformat(os.getenv("DEMO_START_DATE", "2026-01-01")),
    )
    parser.add_argument(
        "--end-date",
        type=date.fromisoformat,
        default=date.fromisoformat(os.getenv("DEMO_END_DATE", "2026-06-01")),
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=int(os.getenv("DEMO_RANDOM_SEED", "20260615")),
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    events = generate_events(args.start_date, args.end_date, args.seed)
    validate_scenarios(events)
    reset_demo_data(args.root)
    write_events(args.root, events)
    manifest = write_manifest(args.root, events, args.start_date, args.end_date, args.seed)
    make_demo_storage_writable(args.root.parent)
    print(
        f"Datos de demostración generados: sujetos={len(PROFILES)} "
        f"eventos={len(events)} manifiesto={manifest}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
