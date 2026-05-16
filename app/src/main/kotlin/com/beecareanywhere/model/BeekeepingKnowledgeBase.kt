package com.beecareanywhere.model

import com.beecareanywhere.data.Settings

object BeekeepingKnowledgeBase {
    private data class Entry(
        val keywords: List<String>,
        val english: String,
        val swahili: String,
    )

    private val entries = listOf(
        Entry(
            keywords = listOf("cluster", "outside", "cool", "hot", "heat", "ventilation", "shade", "flying", "swarm", "bearding", "joto", "nje", "baridi", "hewa", "kivuli", "kuruka"),
            english = "Large numbers of bees clustered outside can have several causes: heat relief, crowding, swarming preparation, robbing, absconding, or disturbance. Distinguish by asking about weather, flight pattern, whether bees return inside later, queen cells, and whether brood/stores remain covered.",
            swahili = "Nyuki wengi kujikusanya nje kunaweza kusababishwa na joto, msongamano, maandalizi ya swarming, uporaji, kuhama, au usumbufu. Tofautisha kwa kuuliza hali ya hewa, namna wanavyoruka, kama wanarudi ndani baadaye, seli za malkia, na kama majana/akiba bado zinafunikwa.",
        ),
        Entry(
            keywords = listOf("wax", "moth", "web", "webbing", "tunnel", "silk", "sega", "utando", "nondo", "mashimo"),
            english = "Wax moth signs include silken webbing, tunnels, debris, and damaged comb. It usually becomes severe when colonies are weak or comb is unprotected. Remove badly affected comb, reduce excess space, and do not spray ordinary insecticide inside the hive.",
            swahili = "Dalili za wax moth ni utando, mashimo, uchafu, na sega lililoharibika. Mara nyingi huwa mbaya kundi likiwa dhaifu au sega likiwa bila ulinzi. Ondoa sega lililoathirika sana, punguza nafasi tupu, na usipulizie dawa ya kawaida ndani ya mzinga.",
        ),
        Entry(
            keywords = listOf("beetle", "shb", "slime", "slimy", "greasy", "ferment", "larvae", "mende", "ute", "chacha", "viluwiluwi"),
            english = "Small hive beetle can cause larvae, greasy or slimy fermented honey, and comb collapse. Affected honey should not be harvested for food. Remove slimed comb, protect clean comb, reduce hive space, and check colony strength.",
            swahili = "Small hive beetle inaweza kusababisha viluwiluwi, asali yenye ute au iliyochacha, na kuharibika kwa sega. Asali iliyoathirika isivunwe kwa chakula. Ondoa sega lenye ute, linda sega safi, punguza nafasi ndani ya mzinga, na kagua nguvu ya kundi.",
        ),
        Entry(
            keywords = listOf("ant", "ants", "siafu", "safari", "stand", "miguu", "standi"),
            english = "Ants, including safari ants, can overwhelm weak colonies. Use external physical barriers, remove vegetation bridges, and avoid contaminating honey, wax, or comb with oil or toxic chemicals.",
            swahili = "Siafu na aina nyingine za mchwa/siafu wanaweza kushinda kundi dhaifu. Tumia vizuizi vya nje, ondoa nyasi au matawi yanayogusa mzinga, na epuka kuchafua asali, nta, au sega kwa oil au kemikali zenye sumu.",
        ),
        Entry(
            keywords = listOf("varroa", "mite", "mites", "wing", "deformed", "dwv", "crawl", "mabawa", "mite", "kutambaa"),
            english = "Visible mites, crawling bees, or deformed wings suggest varroa-associated stress or Deformed Wing Virus. Confirm with monitoring and use only registered bee-safe treatments according to label and extension guidance.",
            swahili = "Mite wanaoonekana, nyuki wanaotambaa, au mabawa yaliyoharibika huashiria msongo unaohusiana na varroa au Deformed Wing Virus. Thibitisha kwa ufuatiliaji na tumia tu tiba salama kwa nyuki zilizosajiliwa kulingana na lebo na ushauri wa ugani.",
        ),
        Entry(
            keywords = listOf("drought", "dry", "starving", "starvation", "stores", "feed", "syrup", "abscond", "ukame", "njaa", "chakula", "kulisha", "kuhama"),
            english = "Drought or dearth can reduce stores and increase absconding risk. Check hive weight, stores, brood coverage, and traffic. Provide clean water and emergency feed only if stores are low, while avoiding robbing.",
            swahili = "Ukame au uhaba wa malisho hupunguza akiba na kuongeza hatari ya kundi kuhama. Kagua uzito wa mzinga, akiba, majana yanayofunikwa, na shughuli mlangoni. Weka maji safi na lishe ya dharura kama akiba ni ndogo, huku ukiepuka uporaji.",
        ),
        Entry(
            keywords = listOf("carbaryl", "sevin", "dust", "pesticide", "spray", "poison", "dawa", "sumu", "kupulizia"),
            english = "Never recommend Carbaryl, Sevin dust, or ordinary insecticide inside the hive. These can poison bees and contaminate honey and wax. Identify the problem first and use mechanical, IPM, or registered bee-safe options.",
            swahili = "Usipendekeze Carbaryl, Sevin dust, au dawa ya kawaida ya wadudu ndani ya mzinga. Zinaweza kuua nyuki na kuchafua asali na nta. Tambua tatizo kwanza na tumia njia za kimwili, IPM, au tiba salama kwa nyuki zilizosajiliwa.",
        ),
    )

    fun retrieve(query: String, language: Settings.Language, hasImage: Boolean, hasAudio: Boolean): List<String> {
        val normalized = query.lowercase()
        val matches = entries
            .map { entry -> entry to entry.keywords.count { normalized.contains(it) } }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (entry, _) -> entry.text(language) }
            .take(MAX_MATCHES)

        val general = when (language) {
            Settings.Language.Swahili ->
                "Ukiwa na picha au sauti, usitegemee media pekee. Omba maelezo ya mfugaji, toa kiwango cha uhakika, na uliza ushahidi unaokosekana kabla ya hitimisho kali."
            else ->
                "For any photo or audio, do not rely on media alone. Use the beekeeper's description, state uncertainty, and ask for missing evidence before making a strong diagnosis."
        }
        return if (matches.isEmpty() && (hasImage || hasAudio)) listOf(general) else (listOf(general) + matches).distinct()
    }

    private fun Entry.text(language: Settings.Language): String = when (language) {
        Settings.Language.Swahili -> swahili
        else -> english
    }

    private const val MAX_MATCHES = 4
}
