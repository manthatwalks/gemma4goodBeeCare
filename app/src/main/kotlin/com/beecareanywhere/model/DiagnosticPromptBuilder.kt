package com.beecareanywhere.model

object DiagnosticPromptBuilder {
    const val IMAGE_CLARIFICATION_PROMPT: String =
        "I can use the photo, but to avoid misdiagnosis please describe what you see.\n\n" +
            "Are these your bees, pests, dead bees, webbing, ants, beetles, or something else?\n" +
            "What is the weather like?\n" +
            "Are they flying heavily, leaving the hive, or mostly clustered in one place?\n" +
            "Is this on the entrance, outside wall, frame, comb, or hive stand?"

    const val IMAGE_CLARIFICATION_PROMPT_SW: String =
        "Naweza kutumia picha, lakini ili kuepuka utambuzi usio sahihi tafadhali eleza unachoona.\n\n" +
            "Je, hawa ni nyuki wako, wadudu, nyuki waliokufa, utando, siafu, mende, au kitu kingine?\n" +
            "Hali ya hewa ikoje?\n" +
            "Je, wanaruka sana, wanaondoka kwenye mzinga, au wamejikusanya sehemu moja?\n" +
            "Hii iko mlangoni, ukuta wa nje, fremu, sega, au standi ya mzinga?"

    fun build(
        userText: String,
        hasImage: Boolean,
        hasAudio: Boolean,
        responseLanguage: com.beecareanywhere.data.Settings.Language,
        knowledge: List<String>,
    ): String = buildString {
        val swahili = responseLanguage == com.beecareanywhere.data.Settings.Language.Swahili
        appendLine("BeeCare diagnostic workflow:")
        appendLine("- Answer in ${if (swahili) "Swahili" else "English"}.")
        appendLine("- Treat any image/audio as weak evidence unless the beekeeper describes what is visible or audible.")
        appendLine("- If media evidence and user description conflict, ask a follow-up instead of guessing.")
        appendLine("- Do not overdiagnose pests, swarming, disease, absconding, or equipment problems from one photo.")
        appendLine("- If uncertain, say uncertain and ask for the missing observation.")
        appendLine()
        appendLine("Retrieved local beekeeping knowledge:")
        knowledge.forEach { appendLine("- $it") }
        appendLine()
        appendLine("Available inputs:")
        appendLine("- Image attached: ${if (hasImage) "yes" else "no"}")
        appendLine("- Audio attached: ${if (hasAudio) "yes" else "no"}")
        appendLine()
        appendLine("User description and question:")
        appendLine(userText.trim())
        appendLine()
        if (swahili) {
            appendLine("Muundo wa jibu:")
            appendLine("1. Utambuzi unaowezekana, pamoja na kiwango cha uhakika.")
            appendLine("2. Kwa nini unaendana au hauendani na maelezo.")
            appendLine("3. Hatua za kufanya sasa.")
            appendLine("4. Cha kukagua baadaye.")
            appendLine("Jibu kwa vitendo na epuka kutisha isipokuwa kuna ushahidi wa dharura.")
        } else {
            appendLine("Answer format:")
            appendLine("1. Most likely diagnosis, with uncertainty.")
            appendLine("2. Why this fits or does not fit the observations.")
            appendLine("3. What to do now.")
            appendLine("4. What to check next.")
            appendLine("Keep the answer practical and avoid alarmist advice unless there is clear urgent evidence.")
        }
    }

    fun clarificationPrompt(language: com.beecareanywhere.data.Settings.Language): String =
        if (language == com.beecareanywhere.data.Settings.Language.Swahili) {
            IMAGE_CLARIFICATION_PROMPT_SW
        } else {
            IMAGE_CLARIFICATION_PROMPT
        }
}
