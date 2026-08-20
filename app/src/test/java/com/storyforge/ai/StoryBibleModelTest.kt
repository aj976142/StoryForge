package com.storyforge.ai

import com.storyforge.ai.domain.model.Project
import com.storyforge.ai.domain.model.StoryBible
import com.storyforge.ai.domain.model.StoryBibleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StoryBibleModelTest {
    @Test
    fun storyBibleBelongsToItsProject() {
        val a = Project("a", "A", storyBible = StoryBible(characters = listOf(StoryBibleEntry("1", "Alice"))))
        val b = Project("b", "B")
        assertEquals("Alice", a.storyBible.characters.single().name)
        assertEquals(0, b.storyBible.characters.size)
        assertNotEquals(a.storyBible, b.storyBible)
    }

    @Test
    fun legacyProjectDefaultsToEmptyBible() {
        val project = Project("legacy", "Legacy")
        assertEquals(0, project.storyBible.characters.size)
        assertEquals(0, project.storyBible.locations.size)
        assertEquals(0, project.storyBible.themes.size)
    }
}
