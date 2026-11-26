package com.transportguide.app.net

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.transportguide.app.logic.BritishEnglish
import com.transportguide.app.logic.Disruption
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * On-device translation of Italian / German headlines into English (Google ML Kit).
 * The language pack (~30 MB each) downloads once on first use, then works offline.
 * Output is passed through [BritishEnglish] for UK spelling.
 */
object Translator {
    private val cache = ConcurrentHashMap<String, String>()
    private val clients = mapOf(
        "it" to Translation.getClient(options(TranslateLanguage.ITALIAN)),
        "de" to Translation.getClient(options(TranslateLanguage.GERMAN)),
    )

    private fun options(src: String) = TranslatorOptions.Builder()
        .setSourceLanguage(src).setTargetLanguage(TranslateLanguage.ENGLISH).build()

    /** Ready language packs, so the first slow download happens only once. */
    private val ready = ConcurrentHashMap<String, Boolean>()

    private suspend fun toEnglish(text: String, lang: String): String? {
        val client = clients[lang] ?: return BritishEnglish.convert(text)
        cache[text]?.let { return it }
        return withTimeoutOrNull(90_000) {
            if (ready[lang] != true) {
                client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                ready[lang] = true
            }
            BritishEnglish.convert(client.translate(text).await()).also { cache[text] = it }
        }
    }

    /** Translates headlines; any that fail keep the original wording. */
    suspend fun translate(items: List<Disruption>): List<Disruption> = coroutineScope {
        items.map { d ->
            async {
                val en = runCatching { toEnglish(d.title, d.lang) }.getOrNull()
                when {
                    en == null -> d
                    d.lang == "en" -> d.copy(title = en)
                    else -> d.copy(title = en, originalTitle = d.title)
                }
            }
        }.awaitAll()
    }
}
