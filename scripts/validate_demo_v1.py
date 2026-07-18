#!/usr/bin/env python3
"""Validate the generated demo-v1 publication package and manifest."""

from __future__ import annotations

import hashlib
import json
from datetime import date
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
PACKAGE_PATH = (
    ROOT
    / "composeApp/src/commonMain/composeResources/files/demo/demo-v1-publication-package.json"
)
MANIFEST_PATH = ROOT / "fixtures/demo/demo-v1-manifest.json"
SCHEMA_PATH = ROOT / "docs/contracts/organization-approval-v1.schema.json"
WORKSPACE_ID = "demo-v1"
GESTOR_ID = "member-demo-gestor-seguranca"
ARRAY_KEYS = [
    "teams",
    "members",
    "memberTeamMemberships",
    "teamManagerAssignments",
    "scheduleChangeRequests",
    "schedulePeriods",
    "scheduleAssignments",
    "publicationRecords",
]


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def parse_date(value: str) -> date:
    return date.fromisoformat(value)


def add(condition: bool, errors: list[str], message: str) -> None:
    if not condition:
        errors.append(message)


def id_set(package: dict, key: str) -> set[str]:
    return {item["id"] for item in package.get(key, []) if "id" in item}


def validate_schema(package: dict, errors: list[str]) -> None:
    try:
        import jsonschema
    except ImportError:
        return

    schema = load_json(SCHEMA_PATH)
    validator = jsonschema.Draft202012Validator(schema)
    schema_errors = sorted(validator.iter_errors(package), key=lambda item: list(item.path))
    for error in schema_errors:
        path = "$" if not error.path else "$." + ".".join(str(part) for part in error.path)
        errors.append(f"schema {path}: {error.message}")


def validate_workspace_ids(package: dict, errors: list[str]) -> None:
    workspace = package.get("workspace", {})
    add(workspace.get("workspaceId") == WORKSPACE_ID, errors, "workspace.workspaceId must be demo-v1")
    for key in ARRAY_KEYS:
        for item in package.get(key, []):
            add(
                item.get("workspaceId") == WORKSPACE_ID,
                errors,
                f"{key}.{item.get('id', '<missing id>')} workspaceId must be demo-v1",
            )


def validate_unique_ids(package: dict, errors: list[str]) -> None:
    for key in ARRAY_KEYS:
        seen: set[str] = set()
        for item in package.get(key, []):
            item_id = item.get("id")
            if item_id in seen:
                errors.append(f"{key} has duplicate id {item_id}")
            seen.add(item_id)

    team_member_ids = [item.get("id") for key in ("teams", "members") for item in package.get(key, [])]
    duplicates = sorted({item_id for item_id in team_member_ids if team_member_ids.count(item_id) > 1})
    for item_id in duplicates:
        errors.append(f"teams+members global id duplicate: {item_id}")


def validate_references(package: dict, errors: list[str]) -> None:
    team_ids = id_set(package, "teams")
    member_ids = id_set(package, "members")
    period_ids = id_set(package, "schedulePeriods")
    assignment_ids = id_set(package, "scheduleAssignments")
    periods_by_id = {period["id"]: period for period in package.get("schedulePeriods", [])}

    for membership in package.get("memberTeamMemberships", []):
        add(membership.get("memberId") in member_ids, errors, f"{membership['id']} memberId missing")
        add(membership.get("teamId") in team_ids, errors, f"{membership['id']} teamId missing")

    for manager in package.get("teamManagerAssignments", []):
        add(manager.get("managerMemberId") in member_ids, errors, f"{manager['id']} managerMemberId missing")
        add(manager.get("teamId") in team_ids, errors, f"{manager['id']} teamId missing")

    for assignment in package.get("scheduleAssignments", []):
        add(assignment.get("memberId") in member_ids, errors, f"{assignment['id']} memberId missing")
        add(assignment.get("teamId") in team_ids, errors, f"{assignment['id']} teamId missing")
        add(assignment.get("periodId") in period_ids, errors, f"{assignment['id']} periodId missing")
        period = periods_by_id.get(assignment.get("periodId"))
        if period is not None:
            assignment_date = parse_date(assignment["date"])
            add(
                parse_date(period["startDate"]) <= assignment_date <= parse_date(period["endDate"]),
                errors,
                f"{assignment['id']} date is outside period range",
            )

    for request in package.get("scheduleChangeRequests", []):
        add(request.get("requesterMemberId") in member_ids, errors, f"{request['id']} requesterMemberId missing")
        add(request.get("requesterTeamId") in team_ids, errors, f"{request['id']} requesterTeamId missing")
        add(
            request.get("assignedManagerMemberId") in member_ids,
            errors,
            f"{request['id']} assignedManagerMemberId missing",
        )
        add(request.get("schedulePeriodId") in period_ids, errors, f"{request['id']} schedulePeriodId missing")
        if request.get("assignmentId") is not None:
            add(request.get("assignmentId") in assignment_ids, errors, f"{request['id']} assignmentId missing")


def validate_manager_and_memberships(package: dict, errors: list[str]) -> None:
    member_ids = id_set(package, "members")
    add(GESTOR_ID in member_ids, errors, f"{GESTOR_ID} must exist")

    memberships = package.get("memberTeamMemberships", [])
    gestor_memberships = [
        item for item in memberships if item.get("memberId") == GESTOR_ID and item.get("active") is True
    ]
    add(len(gestor_memberships) == 1, errors, f"{GESTOR_ID} must have exactly 1 active membership")
    if gestor_memberships:
        add(
            gestor_memberships[0].get("teamId") == "team-demo-seguranca",
            errors,
            f"{GESTOR_ID} active membership must be team-demo-seguranca",
        )

    gestor_manager_assignments = [
        item for item in package.get("teamManagerAssignments", []) if item.get("managerMemberId") == GESTOR_ID
    ]
    add(len(gestor_manager_assignments) == 2, errors, f"{GESTOR_ID} must have 2 manager assignments")

    for member in package.get("members", []):
        member_id = member["id"]
        if member_id == GESTOR_ID:
            continue
        primary_active = [
            item
            for item in memberships
            if item.get("memberId") == member_id
            and item.get("active") is True
            and item.get("isPrimary") is True
        ]
        add(len(primary_active) == 1, errors, f"{member_id} must have exactly 1 active primary membership")


def validate_schedule_rules(package: dict, errors: list[str]) -> None:
    soc_assignments_by_member: dict[str, list[dict]] = {}
    for assignment in package.get("scheduleAssignments", []):
        if assignment.get("teamId") == "team-demo-soc":
            soc_assignments_by_member.setdefault(assignment["memberId"], []).append(assignment)
        if (
            assignment.get("teamId") == "team-demo-seguranca"
            and assignment.get("assignmentType") == "WORK_SHIFT"
        ):
            weekday = parse_date(assignment["date"]).isoweekday()
            add(weekday not in (6, 7), errors, f"{assignment['id']} is WORK_SHIFT on weekend")

    for member_id, assignments in soc_assignments_by_member.items():
        consecutive = 0
        for assignment in sorted(assignments, key=lambda item: item["date"]):
            if assignment.get("assignmentType") == "WORK_SHIFT":
                consecutive += 1
                add(
                    consecutive <= 6,
                    errors,
                    f"{member_id} has more than 6 consecutive SOC WORK_SHIFT days",
                )
            else:
                consecutive = 0


def walk_strings(value: Any):
    if isinstance(value, dict):
        for nested in value.values():
            yield from walk_strings(nested)
    elif isinstance(value, list):
        for nested in value:
            yield from walk_strings(nested)
    elif isinstance(value, str):
        yield value


def validate_email_domains(package: dict, errors: list[str]) -> None:
    for value in walk_strings(package):
        if "@" in value:
            add(
                value.endswith("@example.invalid"),
                errors,
                f"email-like string must use example.invalid: {value}",
            )


def validate_workspace_flags(package: dict, errors: list[str]) -> None:
    workspace = package.get("workspace", {})
    add(workspace.get("externalEffectsAllowed") is False, errors, "externalEffectsAllowed must be false")
    add(workspace.get("notificationsEnabled") is False, errors, "notificationsEnabled must be false")
    add(workspace.get("publicationRevision") == 1, errors, "publicationRevision must be 1")


def validate_manifest(package: dict, manifest: dict, errors: list[str]) -> None:
    counts = manifest.get("counts", {})
    for key in ARRAY_KEYS:
        add(
            counts.get(key) == len(package.get(key, [])),
            errors,
            f"manifest counts.{key}={counts.get(key)} but package has {len(package.get(key, []))}",
        )

    actual_sha256 = hashlib.sha256(PACKAGE_PATH.read_bytes()).hexdigest()
    add(
        manifest.get("sha256") == actual_sha256,
        errors,
        "manifest sha256 does not match current package file",
    )


def main() -> int:
    package = load_json(PACKAGE_PATH)
    manifest = load_json(MANIFEST_PATH)
    errors: list[str] = []

    validate_schema(package, errors)
    validate_workspace_ids(package, errors)
    validate_unique_ids(package, errors)
    validate_references(package, errors)
    validate_manager_and_memberships(package, errors)
    validate_schedule_rules(package, errors)
    validate_email_domains(package, errors)
    validate_workspace_flags(package, errors)
    validate_manifest(package, manifest, errors)

    if errors:
        print("demo-v1 validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print("demo-v1 validation OK")
    print(
        "counts: "
        f"teams={len(package['teams'])}, "
        f"members={len(package['members'])}, "
        f"memberTeamMemberships={len(package['memberTeamMemberships'])}, "
        f"teamManagerAssignments={len(package['teamManagerAssignments'])}, "
        f"schedulePeriods={len(package['schedulePeriods'])}, "
        f"scheduleAssignments={len(package['scheduleAssignments'])}, "
        f"scheduleChangeRequests={len(package['scheduleChangeRequests'])}, "
        f"publicationRecords={len(package['publicationRecords'])}"
    )
    print(f"sha256: {manifest['sha256']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
