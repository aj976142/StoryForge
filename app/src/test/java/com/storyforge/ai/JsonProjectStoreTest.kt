package com.storyforge.ai

import com.storyforge.ai.data.local.JsonProjectStore
import com.storyforge.ai.data.repository.ProjectRepository
import com.storyforge.ai.domain.model.InputMode
import com.storyforge.ai.domain.model.OutputFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class JsonProjectStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun createEditSaveAndReopen() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val store = JsonProjectStore(folder.newFolder("projects"), io = dispatcher)
        store.hydrate()
        val repo = ProjectRepository(store)

        val draft = repo.createDraft(InputMode.TEXT)
        repo.updateIdea(draft.id, "an orchestra of rain")
        repo.updateFormat(draft.id, OutputFormat.NOVEL)
        repo.updateGenerated(draft.id, "Chapter One\nRain.", "Rain Orchestra")
        val saved = repo.save(store.getProject(draft.id)!!.copy(generatedText = "Chapter One\nRain, revised."))

        val reopened = repo.get(saved.id)
        assertNotNull(reopened)
        assertEquals("Rain Orchestra", reopened!!.title)
        assertEquals(OutputFormat.NOVEL, reopened.format)
        assertEquals("Chapter One\nRain, revised.", reopened.generatedText)

        repo.rename(saved.id, "Storm Score")
        assertEquals("Storm Score", repo.get(saved.id)?.title)

        repo.delete(saved.id)
        assertNull(repo.get(saved.id))
    }
}
