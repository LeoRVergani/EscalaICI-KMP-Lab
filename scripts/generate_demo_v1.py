#!/usr/bin/env python3
"""Generate the deterministic demo-v1 publication package and manifest.

Schedule assignment IDs use:
assignment-demo-{memberId without the "member-demo-" prefix}-{YYYY-MM-DD}
Example: assignment-demo-soc-01-2026-07-26.
"""

from __future__ import annotations

import hashlib
import json
from datetime import date, timedelta
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SEED_PATH = ROOT / "fixtures/demo/demo-v1-seed.json"
PACKAGE_PATH = (
    ROOT
    / "composeApp/src/commonMain/composeResources/files/demo/demo-v1-publication-package.json"
)
MANIFEST_PATH = ROOT / "fixtures/demo/demo-v1-manifest.json"
WORKSPACE_ID = "demo-v1"
GENERATOR_VERSION = 1


class DemoGenerationError(RuntimeError):
    pass


def load_seed() -> dict:
    with SEED_PATH.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def parse_date(value: str) -> date:
    return date.fromisoformat(value)


def date_range(start: date, end: date):
    current = start
    while current <= end:
        yield current
        current += timedelta(days=1)


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False, sort_keys=False)
        handle.write("\n")


def assignment_id(member_id: str, assignment_date: date) -> str:
    prefix = "member-demo-"
    member_slug = member_id[len(prefix) :] if member_id.startswith(prefix) else member_id
    return f"assignment-demo-{member_slug}-{assignment_date.isoformat()}"


def require_single(items: list[dict], description: str) -> dict:
    if len(items) != 1:
        raise DemoGenerationError(f"Expected exactly one {description}, found {len(items)}")
    return items[0]


def build_workspace(seed: dict) -> dict:
    workspace = seed["workspace"]
    return {
        "workspaceId": workspace["workspaceId"],
        "workspaceType": workspace["workspaceType"],
        "scenarioId": workspace["scenarioId"],
        "seedVersion": seed["seedVersion"],
        "publicationRevision": seed["publicationRecord"]["publicationRevision"],
        "externalEffectsAllowed": workspace["externalEffectsAllowed"],
        "notificationsEnabled": workspace["notificationsEnabled"],
    }


def build_teams(seed: dict) -> list[dict]:
    return [
        {
            "id": team["id"],
            "workspaceId": WORKSPACE_ID,
            "name": team["name"],
            "acronym": team["acronym"],
            "active": True,
            "schemaVersion": 1,
        }
        for team in seed["teams"]
    ]


def build_members(seed: dict) -> list[dict]:
    return [
        {
            "id": member["id"],
            "workspaceId": WORKSPACE_ID,
            "displayName": member["displayName"],
            "corporateLogin": member["corporateLogin"],
            "emailNormalized": member["emailNormalized"],
            "active": True,
            "schemaVersion": 1,
        }
        for member in seed["members"]
    ]


def build_member_team_memberships(seed: dict) -> list[dict]:
    return [
        {
            "id": membership["id"],
            "workspaceId": WORKSPACE_ID,
            "memberId": membership["memberId"],
            "teamId": membership["teamId"],
            "startDate": membership["startDate"],
            "endDate": None,
            "active": True,
            "isPrimary": membership["isPrimary"],
            "schemaVersion": 1,
        }
        for membership in seed["memberTeamMemberships"]
    ]


def build_team_manager_assignments(seed: dict) -> list[dict]:
    return [
        {
            "id": assignment["id"],
            "workspaceId": WORKSPACE_ID,
            "managerMemberId": assignment["managerMemberId"],
            "teamId": assignment["teamId"],
            "role": assignment["role"],
            "permissions": assignment["permissions"],
            "active": True,
            "validFrom": assignment["validFrom"],
            "validTo": assignment["validTo"],
            "createdAt": "2026-07-01T09:00:00Z",
            "updatedAt": "2026-07-01T09:00:00Z",
            "createdBy": "demo-admin@example.invalid",
            "schemaVersion": 1,
        }
        for assignment in seed["teamManagerAssignments"]
    ]


def build_schedule_periods(seed: dict) -> list[dict]:
    period_range = seed["schedulePeriodRange"]
    teams_by_id = {team["id"]: team for team in seed["teams"]}
    periods = []
    for period in seed["schedulePeriods"]:
        team = teams_by_id[period["teamId"]]
        periods.append(
            {
                "id": period["id"],
                "workspaceId": WORKSPACE_ID,
                "teamId": period["teamId"],
                "name": (
                    f"{team['name']} - "
                    f"{period_range['startDate']} a {period_range['endDate']}"
                ),
                "startDate": period_range["startDate"],
                "endDate": period_range["endDate"],
                "active": True,
                "publicationRevision": seed["publicationRecord"]["publicationRevision"],
                "schemaVersion": 1,
            }
        )
    return periods


def build_schedule_assignments(seed: dict, schedule_periods: list[dict]) -> list[dict]:
    period_range = seed["schedulePeriodRange"]
    start = parse_date(period_range["startDate"])
    end = parse_date(period_range["endDate"])
    periods_by_team = {period["teamId"]: period for period in schedule_periods}
    assignments = []

    soc_pattern = seed["schedulePatterns"]["team-demo-soc"]
    cycle = soc_pattern["restCycle"]
    for day in date_range(start, end):
        day_index = (day - start).days
        is_rest_day = day_index % cycle["cycleLengthDays"] == cycle["restDayIndexInCycle"]
        for shift in soc_pattern["shifts"]:
            off = is_rest_day
            assignments.append(
                {
                    "id": assignment_id(shift["memberId"], day),
                    "workspaceId": WORKSPACE_ID,
                    "periodId": periods_by_team["team-demo-soc"]["id"],
                    "teamId": "team-demo-soc",
                    "memberId": shift["memberId"],
                    "date": day.isoformat(),
                    "assignmentType": "OFF" if off else "WORK_SHIFT",
                    "shiftName": None if off else shift["shiftName"],
                    "startTime": None if off else shift["startTime"],
                    "endTime": None if off else shift["endTime"],
                    "schemaVersion": 1,
                }
            )

    security_pattern = seed["schedulePatterns"]["team-demo-seguranca"]
    work_weekdays = set(security_pattern["workWeekdays"])
    for day in date_range(start, end):
        off = day.isoweekday() not in work_weekdays
        for member_id in security_pattern["members"]:
            assignments.append(
                {
                    "id": assignment_id(member_id, day),
                    "workspaceId": WORKSPACE_ID,
                    "periodId": periods_by_team["team-demo-seguranca"]["id"],
                    "teamId": "team-demo-seguranca",
                    "memberId": member_id,
                    "date": day.isoformat(),
                    "assignmentType": "OFF" if off else "WORK_SHIFT",
                    "shiftName": None if off else security_pattern["shiftName"],
                    "startTime": None if off else security_pattern["startTime"],
                    "endTime": None if off else security_pattern["endTime"],
                    "schemaVersion": 1,
                }
            )

    return sorted(assignments, key=lambda item: (item["teamId"], item["memberId"], item["date"]))


def build_schedule_change_requests(
    seed: dict,
    schedule_periods: list[dict],
    schedule_assignments: list[dict],
    team_manager_assignments: list[dict],
) -> list[dict]:
    start = parse_date(seed["schedulePeriodRange"]["startDate"])
    periods_by_team = {period["teamId"]: period for period in schedule_periods}
    assignments_by_member_date = {
        (assignment["memberId"], assignment["date"]): assignment
        for assignment in schedule_assignments
    }
    managers_by_team: dict[str, list[dict]] = {}
    for manager_assignment in team_manager_assignments:
        managers_by_team.setdefault(manager_assignment["teamId"], []).append(manager_assignment)

    requests = []
    for request in seed["scheduleChangeRequests"]:
        target = request["targetAssignment"]
        target_date = start + timedelta(days=target["dayIndex"])
        target_key = (target["memberId"], target_date.isoformat())
        target_assignment = assignments_by_member_date.get(target_key)
        if target_assignment is None:
            raise DemoGenerationError(
                f"Request {request['id']} targets missing assignment "
                f"{target['memberId']} on {target_date.isoformat()}"
            )
        if target_assignment["assignmentType"] != "WORK_SHIFT":
            raise DemoGenerationError(
                f"Request {request['id']} targets {target_assignment['id']}, "
                f"but it is {target_assignment['assignmentType']} instead of WORK_SHIFT"
            )

        requester_team_id = request["requesterTeamId"]
        manager = require_single(
            managers_by_team.get(requester_team_id, []),
            f"manager assignment for team {requester_team_id}",
        )

        requests.append(
            {
                "id": request["id"],
                "workspaceId": WORKSPACE_ID,
                "requesterMemberId": request["requesterMemberId"],
                "requesterTeamId": requester_team_id,
                "assignedManagerMemberId": manager["managerMemberId"],
                "schedulePeriodId": periods_by_team[requester_team_id]["id"],
                "assignmentId": target_assignment["id"],
                "requestType": request["requestType"],
                "status": request["status"],
                "reason": request["reason"],
                "createdAt": request["createdAt"],
                "resolvedAt": request.get("resolvedAt"),
                "resolvedByMemberId": request.get("resolvedByMemberId"),
                "resolutionNote": request.get("resolutionNote"),
                "schemaVersion": 1,
            }
        )
    return requests


def build_publication_records(seed: dict, counts_created: int) -> list[dict]:
    record = seed["publicationRecord"]
    return [
        {
            "id": record["id"],
            "workspaceId": WORKSPACE_ID,
            "publicationRevision": record["publicationRevision"],
            "publishedAt": record["publishedAt"],
            "publishedByMode": record["publishedByMode"],
            "dryRun": record["dryRun"],
            "countsCreated": counts_created,
            "countsUpdated": 0,
            "countsDeleted": 0,
            "source": record["source"],
            "schemaVersion": 1,
        }
    ]


def build_package(seed: dict) -> dict:
    workspace = build_workspace(seed)
    teams = build_teams(seed)
    members = build_members(seed)
    memberships = build_member_team_memberships(seed)
    manager_assignments = build_team_manager_assignments(seed)
    periods = build_schedule_periods(seed)
    assignments = build_schedule_assignments(seed, periods)
    requests = build_schedule_change_requests(seed, periods, assignments, manager_assignments)

    counts_created = (
        len(teams)
        + len(members)
        + len(memberships)
        + len(manager_assignments)
        + len(periods)
        + len(assignments)
        + len(requests)
    )
    publication_records = build_publication_records(seed, counts_created)

    return {
        "schemaVersion": 1,
        "workspace": workspace,
        "teams": teams,
        "members": members,
        "memberTeamMemberships": memberships,
        "teamManagerAssignments": manager_assignments,
        "scheduleChangeRequests": requests,
        "schedulePeriods": periods,
        "scheduleAssignments": assignments,
        "publicationRecords": publication_records,
    }


def build_manifest(seed: dict, package: dict, package_sha256: str) -> dict:
    counts = {
        "teams": len(package["teams"]),
        "members": len(package["members"]),
        "memberTeamMemberships": len(package["memberTeamMemberships"]),
        "teamManagerAssignments": len(package["teamManagerAssignments"]),
        "schedulePeriods": len(package["schedulePeriods"]),
        "scheduleAssignments": len(package["scheduleAssignments"]),
        "scheduleChangeRequests": len(package["scheduleChangeRequests"]),
        "publicationRecords": len(package["publicationRecords"]),
    }
    return {
        "workspaceId": WORKSPACE_ID,
        "scenarioId": seed["workspace"]["scenarioId"],
        "seedVersion": seed["seedVersion"],
        "publicationRevision": seed["publicationRecord"]["publicationRevision"],
        "generatorVersion": GENERATOR_VERSION,
        "generated": True,
        "generatedAt": seed["publicationRecord"]["publishedAt"],
        "sourceFile": str(PACKAGE_PATH.relative_to(ROOT)),
        "sha256": package_sha256,
        "counts": counts,
        "schemaVersion": 1,
    }


def main() -> int:
    seed = load_seed()
    package = build_package(seed)
    write_json(PACKAGE_PATH, package)
    package_sha256 = hashlib.sha256(PACKAGE_PATH.read_bytes()).hexdigest()
    manifest = build_manifest(seed, package, package_sha256)
    write_json(MANIFEST_PATH, manifest)
    print(f"Wrote {PACKAGE_PATH.relative_to(ROOT)}")
    print(f"Wrote {MANIFEST_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
