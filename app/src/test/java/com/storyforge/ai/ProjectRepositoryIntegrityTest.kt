package com.storyforge.ai

import com.storyforge.ai.data.local.JsonProjectStore
import com.storyforge.ai.data.repository.ProjectRepository
import com.storyforge.ai.domain.model.InputMode
import com.storyforge.ai.domain.model.OutputFormat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ProjectRepositoryIntegrityTest {
    @Test
    fun staleGenerationCannotOverwriteChangedIdea() = runTest {
        val directory = Files.createTempDirectory("storyforge-projects").toFile()
        val store = JsonProjectStore(directory)
        store.hydrate()
        val repo = ProjectRepository(store)
        val project = repo.createDraft(InputMode.TEXT)
        val started = repo.updateIdea(project.id, "a man who can enter dreams")!!
        val staleRevision = started.revision

        repo.updateIdea(project.id, "a woman who can stop time")

        val result = repo.updateGenerated(
            id = project.id,
            expectedRevision = staleRevision,
            text = "OLD STORY",
            title = "Old"
        )

        assertNull(result)
        assertTrue(repo.get(project.id)!!.generatedText.isBlank())
    }

    @Test
    fun changingFormatClearsGeneratedManuscript() = runTest {
        val directory = Files.createTempDirectory("storyforge-projects").toFile()
        val store = JsonProjectStore(directory)
        store.hydrate()
        val repo = ProjectRepository(store)
        var project = repo.createDraft(InputMode.TEXT)
        project = repo.updateIdea(project.id, "a lighthouse remembers sailors")!!
        project = repo.updateGenerated(project.id, project.revision, "STORY", "Lighthouse")!!
        assertEquals(OutputFormat.SHORT_STORY, project.format)
        assertTrue(project.generatedText.isNotBlank())

        val changed = repo.updateFormat(project.id, OutputFormat.MOVIE_SCREENPLAY)!!
        assertTrue(changed.generatedText.isBlank())
        assertTrue(changed.generatedForIdeaHash.isBlank())
    }
}
