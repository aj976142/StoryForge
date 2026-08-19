package com.storyforge.ai.data.ai

import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Offline/demo writer. It never needs credentials and covers every StoryForge format. */
class MockAiService : AiService {
    override val providerId = "mock"
    override val displayName = "StoryForge Demo Writer"

    override fun generate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        if (request.idea.isBlank()) {
            emit(GenerationEvent.Failed("Add an idea before generating.", false))
            return@flow
        }
        emit(GenerationEvent.Progress(20, "Reading your idea"))
        delay(120)
        emit(GenerationEvent.Progress(55, "Shaping ${request.format.displayName.lowercase()}"))
        delay(160)
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
        emit(GenerationEvent.Progress(50, "Finding the next beat"))
        delay(180)
        val addition = continuation(request.currentText, request.format)
        val text = request.currentText.trimEnd() + addition
        emit(GenerationEvent.Chunk(text))
        emit(GenerationEvent.Progress(100, "Ready"))
        emit(GenerationEvent.Completed(text, suggestTitle(request.idea, request.format)))
    }

    private fun continuation(current: String, format: OutputFormat): String = when (format) {
        OutputFormat.MOVIE_SCREENPLAY -> when {
            current.contains("INT. CONTINUOUS") -> "\n\nINT. QUIET ROOM — NIGHT\n\nThe room is still, but the decision is not.\n\nPROTAGONIST\nThen this is where it changes."
            else -> "\n\nINT. CONTINUOUS — LATER\n\nThe first choice creates a consequence. The protagonist has to decide whether to run from it or face it."
        }
        OutputFormat.POETRY -> if (current.count { it == '\n' } > 8) {
            "\n\nAnd this time,\nthe silence answers."
        } else {
            "\n\nThe thought keeps unfolding,\nlike a door left open\nfor one more honest breath."
        }
        OutputFormat.LYRICS -> when {
            current.contains("[VERSE 2]") -> "\n\n[BRIDGE]\nMaybe the road was never the point,\nmaybe we were learning how to move."
            else -> "\n\n[VERSE 2]\nThe road keeps moving, but the feeling stays,\nturning one small memory into a reason to change."
        }
        OutputFormat.YOUTUBE_SCRIPT -> when {
            current.contains("[CLOSING]") -> "\n\n[OUTRO]\nIf this idea made you think, keep it. Write the next sentence before the feeling disappears."
            current.contains("[MID-ROLL]") -> "\n\n[CLOSING]\nNow bring the idea back to the person watching. End with one clear takeaway and one reason to remember it."
            else -> "\n\n[MID-ROLL]\nHere is the turning point: the original thought becomes more interesting when there is a clear problem, a consequence, and a choice."
        }
        OutputFormat.ARTICLE -> "\n\n## What this means in practice\nThe strongest version keeps the original idea visible while adding enough context for the reader to understand why it matters."
        OutputFormat.ESSAY -> "\n\n## The deeper question\nThe idea becomes stronger when the argument acknowledges its tension instead of avoiding it. That gives the conclusion something meaningful to resolve."
        OutputFormat.DIALOGUE -> "\n\nA: So what happens now?\nB: Now we stop explaining it and make the choice.\nA: And if it goes wrong?\nB: Then we finally have a story."
        OutputFormat.PROFESSIONAL -> "\n\nNext step:\nLet's confirm the priority, agree on the timeline, and move forward with the clearest available option."
        else -> "\n\nThe next beat grows from what is already on the page. It adds a new turn without losing the original voice or idea."
    }

    private fun compose(idea: String, format: OutputFormat, p: WritingPreferences): String = when (format) {
        OutputFormat.NOVEL -> "Chapter One\n\n$idea\n\nThe idea becomes an opening scene. The ${p.tone.lowercase()} tone gives the characters room to move, and the first decision creates a reason to turn the page."
        OutputFormat.MOVIE_SCREENPLAY -> "FADE IN:\n\nEXT. LOCATION — DAY\n\n$idea\n\nThe protagonist notices the first impossible detail. They hesitate, then make the choice that starts the story.\n\nFADE OUT."
        OutputFormat.SHORT_STORY -> "The Beginning\n\n$idea\n\nThe thought becomes a situation, the situation becomes a choice, and the choice carries a cost. By the end, the protagonist understands something they could not see at the beginning."
        OutputFormat.YOUTUBE_SCRIPT -> "[HOOK]\n$idea\n\n[INTRO]\nImagine this idea becoming real. What would change first — and what would it cost?\n\n[MAIN POINT]\nStart with the unusual part, give it stakes, then take the audience through one clear turn.\n\n[ENDING]\nBring the original idea back and leave the audience with one question worth thinking about."
        OutputFormat.ARTICLE -> "# $idea\n\n## The idea\n$idea\n\n## Why it matters\nThe central idea becomes clearer when we separate the claim from the assumptions around it.\n\n## Conclusion\nKeep the original meaning, then give the reader a clean path from the first thought to the final point."
        OutputFormat.ESSAY -> "# $idea\n\n$idea\n\nThe central question deserves a clear position. The argument develops from the original thought, considers its implications, and closes by returning to what matters most."
        OutputFormat.POETRY -> "$idea\n\nA thought arrives quietly,\nwaiting between one breath and the next,\nasking to become something true."
        OutputFormat.LYRICS -> "[VERSE 1]\n$idea\nI can hear the beginning in the silence.\n\n[CHORUS]\nWe keep the words that matter,\nand turn the feeling into sound."
        OutputFormat.DIALOGUE -> "A: $idea\nB: Then say what you really mean.\nA: I'm trying.\nB: Good. Start there."
        OutputFormat.PROFESSIONAL -> "Subject: $idea\n\nHello,\n\n$idea\n\nI would like to move this forward with a clear next step. Please let me know what works best.\n\nRegards,"
        OutputFormat.POLISHED_WRITING -> "$idea\n\nThe thought stays intact while the wording becomes clearer, smoother, and easier for a reader to follow."
    }

    private fun suggestTitle(idea: String, format: OutputFormat): String =
        idea.replace(Regex("[\\r\\n]+"), " ").trim().take(60).ifBlank { format.displayName }
}
