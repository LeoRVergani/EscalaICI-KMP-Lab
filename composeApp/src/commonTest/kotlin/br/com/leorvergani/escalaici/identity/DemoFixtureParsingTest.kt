package br.com.leorvergani.escalaici.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException

class DemoFixtureParsingTest {
    @Test
    fun parsesMinimumValidPackageAndKeepsAllFields() {
        val pkg = parseDemoFixturePackage(minimumFixtureJson)

        assertEquals(1, pkg.schemaVersion)
        assertEquals("demo-test", pkg.workspace.workspaceId)
        assertEquals("DEMO", pkg.workspace.workspaceType)
        assertEquals("scenario-test", pkg.workspace.scenarioId)
        assertEquals(1, pkg.workspace.seedVersion)
        assertEquals(7, pkg.workspace.publicationRevision)
        assertEquals(false, pkg.workspace.externalEffectsAllowed)
        assertEquals(false, pkg.workspace.notificationsEnabled)

        val team = pkg.teams.single()
        assertEquals("team-test", team.id)
        assertEquals("demo-test", team.workspaceId)
        assertEquals("Team Test", team.name)
        assertEquals("TT", team.acronym)
        assertEquals(true, team.active)
        assertEquals(1, team.schemaVersion)

        val member = pkg.members.single()
        assertEquals("member-test", member.id)
        assertEquals("demo-test", member.workspaceId)
        assertEquals("Member Test", member.displayName)
        assertEquals("member.test", member.corporateLogin)
        assertEquals("member.test@example.invalid", member.emailNormalized)
        assertEquals(true, member.active)
        assertEquals(1, member.schemaVersion)

        val membership = pkg.memberTeamMemberships.single()
        assertEquals("membership-test", membership.id)
        assertEquals("demo-test", membership.workspaceId)
        assertEquals("member-test", membership.memberId)
        assertEquals("team-test", membership.teamId)
        assertEquals("2026-07-01", membership.startDate)
        assertEquals(null, membership.endDate)
        assertEquals(true, membership.active)
        assertEquals(true, membership.isPrimary)
        assertEquals(1, membership.schemaVersion)

        val managerAssignment = pkg.teamManagerAssignments.single()
        assertEquals("manager-test", managerAssignment.id)
        assertEquals("demo-test", managerAssignment.workspaceId)
        assertEquals("manager-test", managerAssignment.managerMemberId)
        assertEquals("team-test", managerAssignment.teamId)
        assertEquals("PRIMARY_MANAGER", managerAssignment.role)
        assertEquals(true, managerAssignment.permissions.viewTeamSchedule)
        assertEquals(true, managerAssignment.permissions.viewTeamMembers)
        assertEquals(false, managerAssignment.permissions.editTeamSchedule)
        assertEquals(true, managerAssignment.permissions.approveScheduleChanges)
        assertEquals(false, managerAssignment.permissions.publishSchedule)
        assertEquals(false, managerAssignment.permissions.manageTeamAssignments)
        assertEquals(true, managerAssignment.active)
        assertEquals("2026-07-01", managerAssignment.validFrom)
        assertEquals(null, managerAssignment.validTo)
        assertEquals("2026-07-01T09:00:00Z", managerAssignment.createdAt)
        assertEquals("2026-07-01T09:00:00Z", managerAssignment.updatedAt)
        assertEquals("demo-admin@example.invalid", managerAssignment.createdBy)
        assertEquals(1, managerAssignment.schemaVersion)

        val period = pkg.schedulePeriods.single()
        assertEquals("period-test", period.id)
        assertEquals("demo-test", period.workspaceId)
        assertEquals("team-test", period.teamId)
        assertEquals("Team Test - Period", period.name)
        assertEquals("2026-07-26", period.startDate)
        assertEquals("2026-08-25", period.endDate)
        assertEquals(true, period.active)
        assertEquals(7, period.publicationRevision)
        assertEquals(1, period.schemaVersion)

        val workAssignment = pkg.scheduleAssignments[0]
        assertEquals("assignment-work", workAssignment.id)
        assertEquals("demo-test", workAssignment.workspaceId)
        assertEquals("period-test", workAssignment.periodId)
        assertEquals("team-test", workAssignment.teamId)
        assertEquals("member-test", workAssignment.memberId)
        assertEquals("2026-07-26", workAssignment.date)
        assertEquals("WORK_SHIFT", workAssignment.assignmentType)
        assertEquals("Morning", workAssignment.shiftName)
        assertEquals("07:00", workAssignment.startTime)
        assertEquals("13:00", workAssignment.endTime)
        assertEquals(1, workAssignment.schemaVersion)

        val offAssignment = pkg.scheduleAssignments[1]
        assertEquals("OFF", offAssignment.assignmentType)
        assertEquals(null, offAssignment.shiftName)
        assertEquals(null, offAssignment.startTime)
        assertEquals(null, offAssignment.endTime)

        val pendingRequest = pkg.scheduleChangeRequests[0]
        assertEquals("request-pending", pendingRequest.id)
        assertEquals("demo-test", pendingRequest.workspaceId)
        assertEquals("member-test", pendingRequest.requesterMemberId)
        assertEquals("team-test", pendingRequest.requesterTeamId)
        assertEquals("manager-test", pendingRequest.assignedManagerMemberId)
        assertEquals("period-test", pendingRequest.schedulePeriodId)
        assertEquals(null, pendingRequest.assignmentId)
        assertEquals("SHIFT_CHANGE", pendingRequest.requestType)
        assertEquals("PENDING", pendingRequest.status)
        assertEquals("Pending reason", pendingRequest.reason)
        assertEquals("2026-07-27T09:00:00Z", pendingRequest.createdAt)
        assertEquals(null, pendingRequest.resolvedAt)
        assertEquals(null, pendingRequest.resolvedByMemberId)
        assertEquals(null, pendingRequest.resolutionNote)
        assertEquals(1, pendingRequest.schemaVersion)

        val rejectedRequest = pkg.scheduleChangeRequests[1]
        assertEquals("request-rejected", rejectedRequest.id)
        assertEquals("assignment-work", rejectedRequest.assignmentId)
        assertEquals("REJECTED", rejectedRequest.status)
        assertEquals("2026-07-28T10:00:00Z", rejectedRequest.resolvedAt)
        assertEquals("manager-test", rejectedRequest.resolvedByMemberId)
        assertEquals("Rejected reason", rejectedRequest.resolutionNote)

        val publication = pkg.publicationRecords.single()
        assertEquals("publication-test", publication.id)
        assertEquals("demo-test", publication.workspaceId)
        assertEquals(7, publication.publicationRevision)
        assertEquals("2026-07-25T18:00:00Z", publication.publishedAt)
        assertEquals("LOCAL_TEST_MODE", publication.publishedByMode)
        assertEquals(false, publication.dryRun)
        assertEquals(10, publication.countsCreated)
        assertEquals(2, publication.countsUpdated)
        assertEquals(1, publication.countsDeleted)
        assertEquals("DEMO_SEED", publication.source)
        assertEquals(1, publication.schemaVersion)
    }

    @Test
    fun mapsFixtureCollectionsToDomainModels() {
        val pkg = parseDemoFixturePackage(minimumFixtureJson)

        val member = pkg.toMembers().single()
        assertEquals("member.test@example.invalid", member.email)
        assertEquals("Member Test", member.scaleName)
        assertEquals("Member Test", member.displayName)
        assertEquals("member-test", member.id)
        assertEquals(true, member.active)
        assertEquals("demo-test", member.workspaceId)
        assertEquals(7, member.publicationRevision)

        val team = pkg.toTeams().single()
        assertEquals("team-test", team.teamId)
        assertEquals("Team Test", team.name)
        assertEquals("Team Test", team.displayName)
        assertEquals("demo-test", team.workspaceId)
        assertEquals(7, team.publicationRevision)

        val membership = pkg.toMemberships().single()
        assertEquals("membership-test", membership.id)
        assertEquals("member-test", membership.memberId)
        assertEquals("team-test", membership.teamId)
        assertEquals(null, membership.roleId)
        assertEquals("2026-07-01", membership.startDate)
        assertEquals(null, membership.endDate)
        assertEquals(true, membership.active)
        assertEquals(true, membership.isPrimary)
        assertEquals("demo-test", membership.workspaceId)
        assertEquals(7, membership.publicationRevision)
    }

    @Test
    fun mapsLoginByMemberId() {
        val pkg = parseDemoFixturePackage(minimumFixtureJson)

        assertEquals(mapOf("member-test" to "member.test"), pkg.loginByMemberId())
    }

    @Test
    fun invalidJsonMissingRequiredFieldThrows() {
        val invalidJson = minimumFixtureJson.replace("\"workspaceType\": \"DEMO\",", "")

        assertFailsWith<SerializationException> {
            parseDemoFixturePackage(invalidJson)
        }
    }

    private val minimumFixtureJson = """
        {
          "schemaVersion": 1,
          "workspace": {
            "workspaceId": "demo-test",
            "workspaceType": "DEMO",
            "scenarioId": "scenario-test",
            "seedVersion": 1,
            "publicationRevision": 7,
            "externalEffectsAllowed": false,
            "notificationsEnabled": false
          },
          "teams": [
            {
              "id": "team-test",
              "workspaceId": "demo-test",
              "name": "Team Test",
              "acronym": "TT",
              "active": true,
              "schemaVersion": 1
            }
          ],
          "members": [
            {
              "id": "member-test",
              "workspaceId": "demo-test",
              "displayName": "Member Test",
              "corporateLogin": "member.test",
              "emailNormalized": "member.test@example.invalid",
              "active": true,
              "schemaVersion": 1
            }
          ],
          "memberTeamMemberships": [
            {
              "id": "membership-test",
              "workspaceId": "demo-test",
              "memberId": "member-test",
              "teamId": "team-test",
              "startDate": "2026-07-01",
              "endDate": null,
              "active": true,
              "isPrimary": true,
              "schemaVersion": 1
            }
          ],
          "teamManagerAssignments": [
            {
              "id": "manager-test",
              "workspaceId": "demo-test",
              "managerMemberId": "manager-test",
              "teamId": "team-test",
              "role": "PRIMARY_MANAGER",
              "permissions": {
                "viewTeamSchedule": true,
                "viewTeamMembers": true,
                "editTeamSchedule": false,
                "approveScheduleChanges": true,
                "publishSchedule": false,
                "manageTeamAssignments": false
              },
              "active": true,
              "validFrom": "2026-07-01",
              "validTo": null,
              "createdAt": "2026-07-01T09:00:00Z",
              "updatedAt": "2026-07-01T09:00:00Z",
              "createdBy": "demo-admin@example.invalid",
              "schemaVersion": 1
            }
          ],
          "scheduleChangeRequests": [
            {
              "id": "request-pending",
              "workspaceId": "demo-test",
              "requesterMemberId": "member-test",
              "requesterTeamId": "team-test",
              "assignedManagerMemberId": "manager-test",
              "schedulePeriodId": "period-test",
              "assignmentId": null,
              "requestType": "SHIFT_CHANGE",
              "status": "PENDING",
              "reason": "Pending reason",
              "createdAt": "2026-07-27T09:00:00Z",
              "resolvedAt": null,
              "resolvedByMemberId": null,
              "resolutionNote": null,
              "schemaVersion": 1
            },
            {
              "id": "request-rejected",
              "workspaceId": "demo-test",
              "requesterMemberId": "member-test",
              "requesterTeamId": "team-test",
              "assignedManagerMemberId": "manager-test",
              "schedulePeriodId": "period-test",
              "assignmentId": "assignment-work",
              "requestType": "SCHEDULE_CORRECTION",
              "status": "REJECTED",
              "reason": "Rejected request",
              "createdAt": "2026-07-28T09:00:00Z",
              "resolvedAt": "2026-07-28T10:00:00Z",
              "resolvedByMemberId": "manager-test",
              "resolutionNote": "Rejected reason",
              "schemaVersion": 1
            }
          ],
          "schedulePeriods": [
            {
              "id": "period-test",
              "workspaceId": "demo-test",
              "teamId": "team-test",
              "name": "Team Test - Period",
              "startDate": "2026-07-26",
              "endDate": "2026-08-25",
              "active": true,
              "publicationRevision": 7,
              "schemaVersion": 1
            }
          ],
          "scheduleAssignments": [
            {
              "id": "assignment-work",
              "workspaceId": "demo-test",
              "periodId": "period-test",
              "teamId": "team-test",
              "memberId": "member-test",
              "date": "2026-07-26",
              "assignmentType": "WORK_SHIFT",
              "shiftName": "Morning",
              "startTime": "07:00",
              "endTime": "13:00",
              "schemaVersion": 1
            },
            {
              "id": "assignment-off",
              "workspaceId": "demo-test",
              "periodId": "period-test",
              "teamId": "team-test",
              "memberId": "member-test",
              "date": "2026-07-27",
              "assignmentType": "OFF",
              "shiftName": null,
              "startTime": null,
              "endTime": null,
              "schemaVersion": 1
            }
          ],
          "publicationRecords": [
            {
              "id": "publication-test",
              "workspaceId": "demo-test",
              "publicationRevision": 7,
              "publishedAt": "2026-07-25T18:00:00Z",
              "publishedByMode": "LOCAL_TEST_MODE",
              "dryRun": false,
              "countsCreated": 10,
              "countsUpdated": 2,
              "countsDeleted": 1,
              "source": "DEMO_SEED",
              "schemaVersion": 1
            }
          ]
        }
    """.trimIndent()
}
