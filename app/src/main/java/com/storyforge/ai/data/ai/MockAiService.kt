package com.storyforge.ai.data.ai

import com.storyforge.ai.domain.model.OutputFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Offline writer used when no API key is configured. */
class MockAiService : AiService {
    override val providerId = "mock"
    override val displayName = "StoryForge Demo Writer"

    override fun generate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        if (request.idea.isBlank()) { emit(GenerationEvent.Failed("Add an idea before generating.", false)); return@flow }
        emit(GenerationEvent.Progress(18, "Understanding the idea")); delay(120)
        emit(GenerationEvent.Progress(48, "Building the structure")); delay(120)
        emit(GenerationEvent.Progress(78, "Writing the draft")); delay(120)
        val text = compose(request.idea.trim(), request.format, request.preferences)
        emit(GenerationEvent.Chunk(text))
        emit(GenerationEvent.Progress(100, "Ready"))
        emit(GenerationEvent.Completed(text, suggestTitle(request.idea, request.format)))
    }

    override fun continueWriting(request: ContinueRequest): Flow<GenerationEvent> = flow {
        if (request.currentText.isBlank()) {
            emitAllCompat(generate(GenerationRequest(request.idea, request.format, request.preferences)))
            return@flow
        }
        emit(GenerationEvent.Progress(35, "Reading the last scene")); delay(120)
        emit(GenerationEvent.Progress(72, "Finding the next turn")); delay(120)
        val addition = continuation(request.currentText, request.format)
        val text = request.currentText.trimEnd() + addition
        emit(GenerationEvent.Chunk(text)); emit(GenerationEvent.Progress(100, "Ready"))
        emit(GenerationEvent.Completed(text, suggestTitle(request.idea, request.format)))
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<GenerationEvent>.emitAllCompat(flow: Flow<GenerationEvent>) {
        flow.collect { emit(it) }
    }

    private fun continuation(current: String, format: OutputFormat): String = when (format) {
        OutputFormat.MOVIE_SCREENPLAY -> "\n\nINT. UNKNOWN ROOM — NIGHT\n\nThe consequence finally arrives. The protagonist looks at the impossible evidence and realizes the first choice was only the beginning.\n\nPROTAGONIST\nIf I can change this, I can change what comes next."
        OutputFormat.YOUTUBE_SCRIPT -> "\n\n[MID-ROLL]\nBut here's the part most people miss: the original idea only becomes a story when the character has something to lose. That's the pressure that forces the next decision.\n\n[CLOSING]\nSo take the original thought, add one impossible problem, and follow the consequence. That's where the interesting story begins."
        OutputFormat.LYRICS -> "\n\n[VERSE 2]\nThe night keeps a secret I cannot outrun,\nand every small choice changes what I've become.\n\n[BRIDGE]\nMaybe the answer was never outside,\nmaybe it was waiting on the other side."
        OutputFormat.POETRY -> "\n\nAnd then the thought changes shape,\nnot because it found an answer,\nbut because it finally found a question."
        OutputFormat.DIALOGUE -> "\n\nA: You're saying this was the plan?\nB: No. The plan was to survive.\nA: And now?\nB: Now we decide what surviving is supposed to mean."
        else -> "\n\nThe next moment changes the meaning of what came before. The protagonist has one clear choice, one consequence, and no easy way back."
    }

    private fun compose(idea: String, format: OutputFormat, p: WritingPreferences): String = when (format) {
        OutputFormat.NOVEL -> """Chapter One

$idea

The first sign that something was wrong arrived quietly. There was no warning, no convenient explanation—only a detail that refused to make sense.

The protagonist noticed it and tried to ignore it. That would have been easier if the world had stayed ordinary. Instead, the strange detail became a choice: follow it, or walk away and accept that some questions are safer unanswered.

They followed it.

By the time the truth appeared, the original problem had become personal. Someone else now had something to lose, and the protagonist understood that solving the mystery would cost more than curiosity.

The door opened.

Whatever waited on the other side had been waiting for them.

*Tone: ${p.tone}. This opening is designed to grow into a chaptered story.*"""

        OutputFormat.MOVIE_SCREENPLAY -> """FADE IN:

EXT. CITY STREET — NIGHT

$idea

The street is almost empty. The protagonist stops beneath a broken light and studies the impossible thing in their hands.

PROTAGONIST
This shouldn't be possible.

A distant sound answers.

They look up. Someone is watching from across the street.

INT. SMALL APARTMENT — LATER

The evidence covers the table. Every explanation leads to the same conclusion: the rules have changed.

PROTAGONIST
If this is real, someone else already knows.

A knock.

The protagonist freezes.

CUT TO BLACK.

TITLE: THE FIRST DOOR"""

        OutputFormat.SHORT_STORY -> """$idea

At first, nobody believed it.

The strange part was not that the impossible thing happened. It was how ordinary everything looked immediately afterward. Cars continued down the road. People checked their phones. Somewhere, a dog barked at nothing.

But the protagonist knew what they had seen.

There was only one way to prove it: try again.

The second attempt worked.

That was when the wonder disappeared and the danger began. If the ability could change one small thing, it could change something much bigger. And if the protagonist could do it, there was no guarantee they were the only one who could.

They had one night to decide whether the gift was worth its price.

By sunrise, the answer would change everything."""

        OutputFormat.YOUTUBE_SCRIPT -> """[HOOK]
$idea

What if this wasn't just an idea—but a real ability? Imagine discovering that one impossible rule suddenly works in your everyday life.

[SETUP]
At first, the ability looks harmless. The character tests it with something small. It works. Then they try something bigger, and that's where the story changes.

[THE TURN]
The real problem isn't whether the ability works. It's who notices. Every powerful advantage creates attention, and attention creates consequences.

[PAYOFF]
Now the character has a choice: use the ability to fix their own life, or risk everything by using it for someone else.

[CLOSING]
That's the part that makes the premise powerful. The ability is only the hook. The real story is what the character is willing to sacrifice for it."""

        OutputFormat.ARTICLE -> """# ${idea.replaceFirstChar { it.uppercase() }}

## The core idea
$idea

## Why it is interesting
The strongest version of this idea is not simply the unusual premise. It is the consequence. Once the central rule is accepted, the reader naturally asks what changes, who benefits, and what could go wrong.

## The practical story logic
A useful way to develop the idea is to move from possibility to pressure. First establish the unusual ability or situation. Then introduce a problem that makes using it unavoidable. Finally, force a choice where every option has a cost.

## Conclusion
Keep the original idea at the center, but build a chain of consequences around it. That turns a premise into something a reader can follow and remember."""

        OutputFormat.ESSAY -> """# ${idea.replaceFirstChar { it.uppercase() }}

$idea

The central question is not whether the premise sounds extraordinary. It is what the premise reveals about the person experiencing it. A strange ability becomes meaningful when it creates a conflict between what someone wants and what they are willing to sacrifice.

From that perspective, the idea can be developed through three steps: establish the possibility, introduce a consequence, and force a decision. Each step should make the next one harder to avoid.

The strongest conclusion returns to the original thought with a changed understanding. The idea remains the same, but the meaning around it has deepened."""

        OutputFormat.POETRY -> """$idea

A thought walks in
without knocking,
carrying a world
inside its hands.

It asks for a name.
It asks for a door.
It asks what happens
when impossible things
become ordinary.

And somewhere between
fear and wonder,
the first story begins."""

        OutputFormat.LYRICS -> """[VERSE 1]
$idea
Something impossible is calling my name,
I thought I was ready, but nothing's the same.

[CHORUS]
If I could change the world tonight,
would I know what's wrong from right?
Every answer opens another door,
and I don't know what I'm looking for.

[VERSE 2]
The secret gets heavier every time I hide,
there's nowhere left for yesterday to stay inside.

[BRIDGE]
Maybe the gift was never meant to save me,
maybe the choice is what makes me free.

[CHORUS]
If I could change the world tonight,
would I know what's wrong from right?"""

        OutputFormat.DIALOGUE -> """A: $idea
B: And you actually believe it?
A: I didn't. Not until I saw it happen.
B: Then what are you going to do?
A: Find out what the ability costs.
B: And if the price is too high?
A: Then I need to know before someone else pays it for me."""

        OutputFormat.PROFESSIONAL -> """Subject: ${idea.take(70)}

Hello,

I am writing to turn this idea into a clear next step. The main objective is to preserve the original intent while making the message easier to understand and act on.

Proposed approach:
1. Clarify the core objective.
2. Identify the most important audience or outcome.
3. Define the next concrete action.

Please let me know if you would like to proceed with this direction.

Regards,"""

        OutputFormat.POLISHED_WRITING -> """$idea

The thought is strong; it simply needs a clearer path for the reader. The revised version keeps the original meaning, removes unnecessary repetition, strengthens the central image, and gives the idea a more natural rhythm.

The result should still sound like the author—just more precise, confident, and readable."""
    }

    private fun suggestTitle(idea: String, format: OutputFormat): String =
        idea.replace(Regex("[\\r\\n]+"), " ").trim().take(60).ifBlank { format.displayName }
}
