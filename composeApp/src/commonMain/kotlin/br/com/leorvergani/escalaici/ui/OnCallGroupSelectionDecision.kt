package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.OnCallAssignment
import br.com.leorvergani.escalaici.model.OnCallGroup

internal sealed interface OnCallGroupSelectionDecision {
    data object NoGroups : OnCallGroupSelectionDecision
    data class RequiresSelection(val groups: List<OnCallGroup>) : OnCallGroupSelectionDecision
    data class UseGroup(val group: OnCallGroup, val skippedSelection: Boolean) : OnCallGroupSelectionDecision
}

internal fun decideOnCallGroupSelection(
    groups: List<OnCallGroup>,
    selectedGroupId: String?
): OnCallGroupSelectionDecision {
    val activeGroups = groups.filter { it.active }.sortedBy { it.name.lowercase() }
    if (activeGroups.isEmpty()) return OnCallGroupSelectionDecision.NoGroups
    if (activeGroups.size == 1) {
        return OnCallGroupSelectionDecision.UseGroup(activeGroups.single(), skippedSelection = true)
    }
    val selected = activeGroups.firstOrNull { it.id == selectedGroupId }
    return if (selected != null) {
        OnCallGroupSelectionDecision.UseGroup(selected, skippedSelection = false)
    } else {
        OnCallGroupSelectionDecision.RequiresSelection(activeGroups)
    }
}

internal fun onCallAssignmentsForGroup(
    assignments: List<OnCallAssignment>,
    groupId: String
): List<OnCallAssignment> =
    assignments.filter { it.groupId == groupId }
