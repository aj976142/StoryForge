package com.storyforge.ai

import com.storyforge.ai.data.ai.AiProviderCatalog
import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.util.GenerationProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationProvenanceTest {
    @Test
    fun whitespace_only_idea_changes_do_not_break_context() {
        val first = GenerationProvenance.fingerprint("  A   boy finds a key.  ", OutputFormat.SHORT_STORY)
        val second = GenerationProvenance.fingerprint("A boy finds a key.", OutputFormat.SHORT_STORY)
        assertEquals(first, second)
    }

    @Test
    fun changing_format_changes_context_fingerprint() {
        val story = GenerationProvenance.fingerprint("A boy finds a key.", OutputFormat.SHORT_STORY)
        val screenplay = GenerationProvenance.fingerprint("A boy finds a key.", OutputFormat.MOVIE_SCREENPLAY)
        assertNotEquals(story, screenplay)
    }

    @Test
    fun different_ideas_have_different_context_fingerprints() {
        val first = GenerationProvenance.fingerprint("A boy finds a key.", OutputFormat.SHORT_STORY)
        val second = GenerationProvenance.fingerprint("A girl finds a key.", OutputFormat.SHORT_STORY)
        assertNotEquals(first, second)
    }

    @Test
    fun provider_catalog_has_a_custom_escape_hatch() {
        assertTrue(AiProviderCatalog.preset(AiProviderCatalog.CUSTOM)?.endpoint?.isEmpty() == true)
    }
}
