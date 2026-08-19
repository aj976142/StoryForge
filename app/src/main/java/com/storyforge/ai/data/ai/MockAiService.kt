package com.storyforge.ai.data.ai

import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Offline/demo writer. It never needs credentials and covers every StoryForge format. */
class MockAiService : AiService {
    override val providerId = "mock"
    override val displayName = "StoryForge Demo Writer"

    override fun generate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        if (request.idea.isBlank()) { emit(GenerationEvent.Failed("Add an idea before generating.", false)); return@flow }
        emit(GenerationEvent.Progress(20, "Reading your idea")); delay(120)
        emit(GenerationEvent.Progress(55, "Shaping ${request.format.displayName.lowercase()}")); delay(160)
        val text = compose(request.idea.trim(), request.format, request.preferences)
        emit(GenerationEvent.Chunk(text))
        emit(GenerationEvent.Progress(100, "Ready"))
        emit(GenerationEvent.Completed(text, suggestTitle(request.idea, request.format)))
    }

    override fun continueWriting(request: ContinueRequest): Flow<GenerationEvent> = flow {
        if (request.currentText.isBlank()) {
            emitAll(generate(GenerationRequest(request.idea, request.format, request.preferences)))
            return@flow
        }
        emit(GenerationEvent.Progress(50, "Continuing the draft")); delay(180)
        val addition = when (request.format) {
            OutputFormat.MOVIE_SCREENPLAY -> "\n\nINT. CONTINUOUS — LATER\n\nThe choice has consequences.\n\nPROTAGONIST\nWe keep going."
            OutputFormat.POETRY -> "\n\nAnd the thought returns,\nquieter now,\nclear enough to follow."
            OutputFormat.LYRICS -> "\n\n[VERSE 2]\nThe road keeps moving, but the heart remembers.\n\n[CHORUS]\nWe keep the words that matter."
            OutputFormat.YOUTUBE_SCRIPT -> "\n\n[MID-ROLL]\nHere's the part worth remembering: keep the original idea, then give it a shape people can follow."
            else -> "\n\nThe next moment grows naturally from the original idea. The story keeps its voice while moving forward, leaving room for the user to decide what happens next."
        }
        val text = request.currentText.trimEnd() + addition
        emit(GenerationEvent.Chunk(text)); emit(GenerationEvent.Progress(100, "Ready"))
        emit(GenerationEvent.Completed(text, suggestTitle(request.idea, request.format)))
    }

    private fun compose(idea: String, format: OutputFormat, p: WritingPreferences): String = when (format) {
        OutputFormat.NOVEL -> "Chapter One\n\n$idea\n\nThe idea becomes the opening scene. ${p.tone} detail gives the characters room to move, and the first decision creates a reason to turn the page."
        OutputFormat.MOVIE_SCREENPLAY -> "FADE IN:\n\nEXT. LOCATION — DAY\n\n$idea\n\nThe protagonist takes a breath and makes the first choice.\n\nPROTAGONIST\nWe start here.\n\nFADE OUT."
        OutputFormat.SHORT_STORY -> "The Beginning\n\n$idea\n\nThe thought becomes a situation, the situation becomes a choice, and the choice carries a cost. By the end, the protagonist understands something they could not see at the beginning."
        OutputFormat.YOUTUBE_SCRIPT -> "[HOOK]\n$idea\n\n[INTRO]\nLet's turn that raw thought into something clear and worth listening to.\n\n[MAIN POINT]\nStart with the idea, give it stakes, then take the audience through one useful turn.\n\n[ENDING]\nThat's the shape. Now make it yours."
        OutputFormat.ARTICLE -> "# $idea\n\n## The idea\n$idea\n\n## Why it matters\nThis section develops the central point clearly, without inventing unsupported facts.\n\n## Conclusion\nThe strongest version keeps the original meaning while making it easier to understand."
        OutputFormat.ESSAY -> "# $idea\n\n$idea\n\nThe central question deserves a clear position. The argument develops from the original thought, considers its implications, and closes by returning to what matters most."
        OutputFormat.POETRY -> "$idea\n\nA thought arrives quietly,\nwaiting between one breath and the next,\nasking to become something true."
        OutputFormat.LYRICS -> "[VERSE 1]\n$idea\nI can hear the beginning in the silence.\n\n[CHORUS]\nWe keep the words that matter,\nand turn the feeling into sound."
        OutputFormat.DIALOGUE -> "A: $idea\nB: Then say what you really mean.\nA: I'm trying.\nB: Good. Start there."
        OutputFormat.PROFESSIONAL -> "Subject: $idea\n\nHello,\n\n$idea\n\nI would like to move this forward with a clear next step. Please let me know what works best.\n\nRegards,"
        OutputFormat.POLISHED_WRITING -> "$idea\n\nThe thought is kept intact while the wording becomes clearer, smoother, and easier for a reader to follow."
    }

    private fun suggestTitle(idea: String, format: OutputFormat): String = idea.replace(Regex("[\\r\\n]+"), " ").trim().take(60).ifBlank { format.displayName }
}
