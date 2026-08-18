package com.storyforge.ai.data.ai

import com.storyforge.ai.domain.model.OutputFormat
import com.storyforge.ai.domain.model.WritingPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Deterministic on-device writer so the app runs without an API key.
 * Responses are structured per [OutputFormat] and incorporate the user's idea.
 */
class MockAiService(
    private val failNext: () -> Boolean = { false }
) : AiService {

    override val providerId: String = "mock"
    override val displayName: String = "StoryForge Mock Writer"

    override fun generate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        runPipeline(request.idea, request.format, request.preferences, request.existingText, continueFrom = false)
    }

    override fun continueWriting(request: ContinueRequest): Flow<GenerationEvent> = flow {
        runPipeline(request.idea, request.format, request.preferences, request.currentText, continueFrom = true)
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<GenerationEvent>.runPipeline(
        idea: String,
        format: OutputFormat,
        preferences: WritingPreferences,
        existing: String?,
        continueFrom: Boolean
    ) {
        val trimmed = idea.trim()
        if (trimmed.isEmpty()) {
            emit(GenerationEvent.Failed("Add an idea before generating.", retryable = false))
            return
        }
        if (failNext()) {
            emit(GenerationEvent.Progress(8, "Connecting"))
            delay(240)
            emit(GenerationEvent.Failed("The writer could not be reached. Try again.", retryable = true))
            return
        }

        val stages = listOf(
            12 to "Reading your idea",
            28 to "Choosing structure",
            46 to "Drafting ${format.displayName.lowercase()}",
            68 to "Shaping voice (${preferences.tone})",
            86 to "Polishing lines",
            100 to "Ready"
        )

        try {
            for ((percent, stage) in stages) {
                emit(GenerationEvent.Progress(percent, stage))
                delay(if (continueFrom) 180L else 260L)
            }

            val text = if (continueFrom) {
                val base = existing.orEmpty().ifBlank { compose(trimmed, format, preferences) }
                base.trimEnd() + "\n\n" + continuation(trimmed, format, preferences)
            } else {
                compose(trimmed, format, preferences)
            }

            emit(GenerationEvent.Chunk(text))
            emit(GenerationEvent.Completed(text, suggestTitle(trimmed, format)))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            emit(GenerationEvent.Failed(error.message ?: "Generation failed.", retryable = true))
        }
    }

    companion object {
        fun suggestTitle(idea: String, format: OutputFormat): String {
            val seed = idea.trim()
                .replace(Regex("[\\r\\n]+"), " ")
                .take(48)
                .ifBlank { "Untitled" }
            val short = if (seed.length < idea.trim().length && seed.contains(' ')) {
                seed.substringBeforeLast(' ')
            } else seed
            return when (format) {
                OutputFormat.YOUTUBE_SCRIPT -> "$short — Script"
                OutputFormat.MOVIE_SCREENPLAY -> short.uppercase().take(40)
                else -> short.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        fun compose(idea: String, format: OutputFormat, prefs: WritingPreferences): String {
            val hook = idea.trim().replace(Regex("\\s+"), " ")
            val tone = prefs.tone.lowercase()
            val pov = prefs.pov
            val lengthNote = when (prefs.length.lowercase()) {
                "short" -> "tight"
                "long" -> "expansive"
                else -> "measured"
            }
            return when (format) {
                OutputFormat.NOVEL -> novel(hook, tone, pov, lengthNote)
                OutputFormat.MOVIE_SCREENPLAY -> screenplay(hook, tone)
                OutputFormat.SHORT_STORY -> shortStory(hook, tone, pov)
                OutputFormat.YOUTUBE_SCRIPT -> youtube(hook, tone)
                OutputFormat.POLISHED_WRITING -> polished(hook, tone)
            }
        }

        private fun continuation(idea: String, format: OutputFormat, prefs: WritingPreferences): String {
            val hook = idea.trim()
            return when (format) {
                OutputFormat.NOVEL ->
                    "Chapter Two\n\nThe promise inside “$hook” does not stay on the first page. " +
                        "By morning the choice has a cost, and the ${prefs.tone.lowercase()} air of the opening " +
                        "gives way to consequence. She keeps moving because stopping would mean admitting " +
                        "the idea was only a wish."
                OutputFormat.MOVIE_SCREENPLAY ->
                    "INT. CONTINUOUS — LATER\n\nThe camera holds. A door that should stay closed does not.\n\n" +
                        "PROTAGONIST\n(quiet)\nWe don't get to pretend we didn't start this.\n\n" +
                        "They step through. The next scene begins."
                OutputFormat.SHORT_STORY ->
                    "Later, when the noise thinned, the meaning of “$hook” sat in the room like a second person. " +
                        "Nothing supernatural — only the ordinary bravery of staying. " +
                        "She put the kettle on and let the ending arrive without being forced."
                OutputFormat.YOUTUBE_SCRIPT ->
                    "[MID-ROLL BEAT]\nIf this is useful, stay for the last part — it's the piece I wish someone " +
                        "had told me about “$hook.”\n\n[B-ROLL: notebook, coffee, a window]\n" +
                        "Here's the practical close: pick one next action and do it before you overthink it."
                OutputFormat.POLISHED_WRITING ->
                    "What follows is the second pass: fewer hedges, a cleaner verb, and a last line that " +
                        "earns the idea instead of decorating it. The thought is still yours. It simply stands up straighter."
            }
        }

        private fun novel(hook: String, tone: String, pov: String, lengthNote: String): String = """
            Chapter One
            The Weight of a Beginning

            $pov. A $tone register. The book opens on a $lengthNote breath.

            Nobody writes “$hook” on a scrap of paper unless the sentence has already been living rent-free in the body. It arrived the way weather does — not asked for, impossible to argue with. By the time the kettle clicked off, the idea had a hallway, a weather system, and a person who would have to walk through both.

            She did not yet know the antagonist's name. She knew the temperature of the room when the thought appeared, which is how most true stories start: not with a plot, but with a pulse.

            Outside, the city kept its ordinary appointments. Inside, a first chapter did the only job a first chapter has — it made going back feel like a smaller life.

            The door, when she finally opened it, did not creak for drama. It simply let the next page in.
        """.trimIndent()

        private fun screenplay(hook: String, tone: String): String = """
            FADE IN:

            TITLE CARD: from a raw note — "$hook"

            EXT. CITY EDGE — DUSK

            A $tone sky. Wind worries a loose flyer against a chain-link fence. Our PROTAGONIST (30s) stands as if the thought just found them.

            PROTAGONIST
            (under breath)
            Say it once, out loud, and it becomes a job.

            They fold a scrap of paper. The words are already smudged.

            INT. SMALL KITCHEN — CONTINUOUS

            Cheap light. A phone face-down. They write anyway — not a plan, a dare.

            PROTAGONIST
            We start here. We don't wait for permission.

            A kettle screams. They don't move. The story has begun.

            FADE OUT.
        """.trimIndent()

        private fun shortStory(hook: String, tone: String, pov: String): String = """
            The Note

            It began as almost nothing: $hook.

            $pov, $tone without trying to be. The sentence sat on the table beside a chipped mug and refused to stay small. People think ideas arrive dressed as plots. This one arrived dressed as weather — a change in pressure, a reason to look up.

            She almost deleted it. Deleting would have been tidy. Instead she left the words where they were and let the afternoon arrange itself around them. A neighbor laughed two floors down. A bus sighed at the curb. Ordinary life, briefly willing to be a stage.

            By evening the idea had a person in it, and the person had a choice, and the choice had a cost she could feel in her hands. That was enough. Not a novel. Not a thesis. A complete small true thing:

            She kept the note. She walked out the door as if the rest of the story already knew her name.
        """.trimIndent()

        private fun youtube(hook: String, tone: String): String = """
            [HOOK — 0:00]
            What if the whole video is just this: $hook
            Stay. I'll turn that raw thought into something you can actually say out loud.

            [INTRO — 0:08]
            I'm drafting this in a $tone voice on purpose — not corporate, not chaotic. If you've ever stared at a notes app and felt the idea go cold, this is the fix.

            [BEAT 1 — The raw thought]
            Read the idea once. Don't decorate it. The power is already in the first wording.

            [BEAT 2 — The shape]
            Hook. Stakes. One turn. A last line people remember. That's the whole architecture.

            [BEAT 3 — The craft]
            Speak it. Cut anything you wouldn't say to a friend. Keep the line that makes you slightly nervous.

            [CTA]
            If this helped, save the script, record a messy take today, and tell me what you made. Subscribe if you want the next forge session.

            [END SCREEN]
            Next: polish pass in under ten minutes.
        """.trimIndent()

        private fun polished(hook: String, tone: String): String {
            val cleaned = hook
                .replace(Regex("\\s+"), " ")
                .trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            return """
                $cleaned

                That is the thought, stood upright. The hedges are gone. The rhythm is $tone, but it still sounds like you.

                What you meant is allowed to be simple. Simple is not small. It is the version a reader can carry out of the room.

                Keep this draft. Change only what is untrue.
            """.trimIndent()
        }
    }
}
