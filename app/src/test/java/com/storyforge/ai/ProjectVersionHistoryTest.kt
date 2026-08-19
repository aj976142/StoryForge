package com.storyforge.ai

import com.storyforge.ai.domain.model.Project
import com.storyforge.ai.domain.model.ProjectVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectVersionHistoryTest {
    @Test
    fun versionsStayOwnedByTheirProject() {
        val first = Project(id = "one", title = "One", generatedText = "First")
        val second = Project(id = "two", title = "Two", generatedText = "Second")
        val firstWithVersion = first.copy(versions = listOf(ProjectVersion("v1", first.title, first.generatedText)))
        assertTrue(firstWithVersion.versions.all { it.text != second.generatedText })
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun restoringAVersionUsesItsOriginalText() {
        val project = Project(
            id = "one",
            title = "Draft",
            generatedText = "New",
            versions = listOf(ProjectVersion("old", "Draft", "Original"))
        )
        val version = project.versions.first { it.id == "old" }
        assertEquals("Original", version.text)
        assertEquals("Draft", version.title)
    }
}
