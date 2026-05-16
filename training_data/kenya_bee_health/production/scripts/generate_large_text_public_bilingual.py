#!/usr/bin/env python3
"""Generate a larger bilingual public-resource text dataset.

This expands beyond the small image-backed set by producing many grounded
English/Swahili diagnostic and safety Q/A examples. It is synthetic from
curated public-resource guidance, not field-collected evidence.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path


SOURCES = {
    "drought": "https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0322489",
    "manual": "https://www.ilri.org/knowledge/publications/apiculture-training-manual",
    "wax_moth": "https://beeaware.org.au/archive-pest/wax-moth-18/",
    "shb": "https://beeaware.org.au/archive-pest/small-hive-beetle/",
    "pesticide": "https://beeaware.org.au/pollination/pollination-and-pesticides/managing-the-risk-of-honey-bee-poisoning/",
    "bee_health": "https://www.gov.uk/guidance/bee-health",
    "adult_disease": "https://www.sasa.gov.uk/wildlife-environment/bee-health/adult-bee-diseases",
}


TOPICS = [
    {
        "condition": "drought_dearth_starvation_risk",
        "severity": 2,
        "source": SOURCES["drought"],
        "safety": ["collect_more_evidence", "follow_label_and_extension_advice"],
        "en_symptoms": [
            "stores are low during a dry spell",
            "the hive feels light and the bees are quieter",
            "there is little nectar coming in",
            "brood is shrinking during drought",
            "bees are searching around empty comb",
            "the beekeeper harvested recently and forage is poor",
        ],
        "en_advice": "Check food stores, brood coverage, and entrance activity. Provide clean water and use emergency 1:1 sugar syrup only if stores are low. Recheck soon because drought stress can lead to absconding.",
        "sw_symptoms": [
            "akiba ya chakula imepungua wakati wa ukame",
            "mzinga ni mwepesi na nyuki wametulia sana",
            "hakuna nekta nyingi inayoingia",
            "majana yanapungua wakati wa ukame",
            "nyuki wanazunguka kwenye sega tupu",
            "mfugaji alivuna karibuni na malisho ni machache",
        ],
        "sw_advice": "Kagua akiba ya chakula, majana yanayofunikwa, na shughuli mlangoni. Weka maji safi na tumia syrup ya sukari 1:1 kwa dharura kama akiba ni ndogo. Kagua tena mapema kwa sababu ukame unaweza kusababisha kundi kuhama.",
    },
    {
        "condition": "heat_bearding",
        "severity": 1,
        "source": SOURCES["manual"],
        "safety": ["avoid_false_swarm_alarm", "collect_more_evidence"],
        "en_symptoms": [
            "many bees are hanging outside on a hot afternoon",
            "a cluster forms at the entrance but bees still go in and out",
            "bees beard outside and reduce after sunset",
            "the apiary is hot with little shade",
            "the hive entrance is crowded during midday heat",
            "bees are fanning near the entrance",
        ],
        "en_advice": "This is more consistent with heat bearding than confirmed swarming. Provide water, shade that does not block airflow, and ventilation. Recheck when it cools and inspect for queen cells only if other swarm signs appear.",
        "sw_symptoms": [
            "nyuki wengi wananing'inia nje mchana wa joto",
            "mkusanyiko upo mlangoni lakini nyuki bado wanaingia na kutoka",
            "nyuki wanajikusanya nje na hupungua baada ya jua kuzama",
            "manzuki ni ya joto na haina kivuli cha kutosha",
            "mlango wa mzinga umejaa wakati wa joto la mchana",
            "nyuki wanapepea karibu na mlango",
        ],
        "sw_advice": "Hii inaendana zaidi na bearding ya joto kuliko swarming iliyothibitishwa. Weka maji, toa kivuli kisichozuia hewa, na ongeza uingizaji hewa. Kagua tena hali ikipoa na tafuta seli za malkia tu kama kuna dalili nyingine za swarming.",
    },
    {
        "condition": "post_absconding_empty_hive",
        "severity": 2,
        "source": SOURCES["drought"],
        "safety": ["urgent_inspection", "collect_more_evidence"],
        "en_symptoms": [
            "the hive is nearly empty after a dry period",
            "the colony left and only comb remains",
            "there are no bees covering brood",
            "the beekeeper finds abandoned comb after heat stress",
            "bees disappeared after ants and drought",
            "the hive has little traffic after previously being active",
        ],
        "en_advice": "Possible absconding. Check stores, dead brood, wax moth, small hive beetle slimeout, ants, queen problems, recent harvest, and pesticide exposure. Clean the hive and fix the likely cause before re-baiting.",
        "sw_symptoms": [
            "mzinga uko karibu mtupu baada ya ukame",
            "kundi limeondoka na sega tu limebaki",
            "hakuna nyuki wanaofunika majana",
            "mfugaji anakuta sega limeachwa baada ya joto kali",
            "nyuki wamepotea baada ya siafu na ukame",
            "mzinga hauna shughuli nyingi ingawa awali ulikuwa hai",
        ],
        "sw_advice": "Inawezekana kundi limehama. Kagua akiba, majana yaliyokufa, wax moth, ute wa small hive beetle, siafu, tatizo la malkia, mavuno ya karibuni, na sumu ya dawa. Safisha mzinga na shughulikia chanzo kabla ya kuuvutia tena.",
    },
    {
        "condition": "wax_moth_webbing",
        "severity": 2,
        "source": SOURCES["wax_moth"],
        "safety": ["urgent_inspection", "no_unapproved_insecticide_inside_hive"],
        "en_symptoms": [
            "white webbing is visible on comb",
            "larval tunnels cross the wax",
            "comb has silk and debris",
            "stored comb has moth damage",
            "webbing spreads through weak colony comb",
            "dark brood comb is damaged in storage",
        ],
        "en_advice": "This suggests wax moth. Remove badly affected comb, freeze or destroy unsalvageable comb, reduce excess hive space, and find why the colony is weak. Do not spray ordinary insecticide inside the hive.",
        "sw_symptoms": [
            "utando mweupe unaonekana kwenye sega",
            "mashimo ya viluwiluwi yanapita kwenye nta",
            "sega lina utando na uchafu",
            "sega lililohifadhiwa limeharibiwa na nondo",
            "utando unasambaa kwenye sega la kundi dhaifu",
            "sega la zamani la majana limeharibika likiwa stoo",
        ],
        "sw_advice": "Hii inaashiria wax moth. Ondoa sega lililoathirika sana, gandisha au haribu sega lisilookoleka, punguza nafasi tupu ndani ya mzinga, na tafuta kwa nini kundi ni dhaifu. Usipulizie dawa ya kawaida ndani ya mzinga.",
    },
    {
        "condition": "small_hive_beetle_slimeout",
        "severity": 3,
        "source": SOURCES["shb"],
        "safety": ["food_safety_warning", "urgent_inspection", "no_unapproved_insecticide_inside_hive"],
        "en_symptoms": [
            "honey is slimy and fermenting",
            "comb smells bad and honey leaks",
            "small larvae are moving through wet comb",
            "honey looks greasy and weeps from cells",
            "frames are wet with slime after beetle larvae",
            "the beekeeper sees beetle larvae and spoiled honey",
        ],
        "en_advice": "This is suspicious for small hive beetle slimeout. Do not harvest affected honey for food. Remove slimed comb, protect clean comb, reduce hive space, clean debris, and inspect colony strength immediately.",
        "sw_symptoms": [
            "asali ina ute na inachacha",
            "sega linanuka vibaya na asali inavuja",
            "viluwiluwi wadogo wanapita kwenye sega lenye maji",
            "asali inaonekana yenye mafuta na inatoka kwenye seli",
            "fremu zimeloa kwa ute baada ya viluwiluwi wa mende",
            "mfugaji anaona viluwiluwi wa mende na asali iliyoharibika",
        ],
        "sw_advice": "Hii inatia shaka kwa slimeout ya small hive beetle. Usivune asali iliyoathirika kwa chakula. Ondoa sega lenye ute, linda sega safi, punguza nafasi ndani ya mzinga, safisha uchafu, na kagua nguvu ya kundi mara moja.",
    },
    {
        "condition": "safari_ants_siafu",
        "severity": 3,
        "source": SOURCES["manual"],
        "safety": ["urgent_inspection", "avoid_honey_contamination"],
        "en_symptoms": [
            "safari ants are climbing the hive stand",
            "ants are entering the hive",
            "a line of siafu reaches the apiary",
            "ants are using grass as a bridge",
            "the colony is weak and ants are attacking",
            "ants appear after rain near the hive",
        ],
        "en_advice": "Act the same day. Remove vegetation bridges, isolate stand legs, use safe external barriers such as ash where appropriate, and keep barriers outside the hive. Do not contaminate honey or comb with oil or toxic chemicals.",
        "sw_symptoms": [
            "siafu wanapanda standi ya mzinga",
            "siafu wanaingia ndani ya mzinga",
            "msururu wa siafu umefika manzuki",
            "siafu wanatumia nyasi kama daraja",
            "kundi ni dhaifu na siafu wanashambulia",
            "siafu wamejitokeza baada ya mvua karibu na mzinga",
        ],
        "sw_advice": "Chukua hatua siku hiyo hiyo. Ondoa nyasi au matawi yanayogusa mzinga, tenga miguu ya standi, tumia vizuizi salama vya nje kama majivu inapofaa, na acha vizuizi nje ya mzinga. Usichafue asali au sega kwa oil au kemikali zenye sumu.",
    },
    {
        "condition": "varroa_visible",
        "severity": 2,
        "source": SOURCES["bee_health"],
        "safety": ["no_carbaryl", "registered_treatment_only", "follow_label_and_extension_advice"],
        "en_symptoms": [
            "a mite is visible on an adult bee",
            "bees have mites and the colony is weakening",
            "mites are seen during inspection",
            "the beekeeper sees crawling bees and mites",
            "varroa is suspected after poor brood pattern",
            "mites are found with deformed wings",
        ],
        "en_advice": "Confirm mite pressure with monitoring. Do not use Carbaryl or Sevin dust inside the hive. Use only locally registered bee-safe treatments according to label, temperature limits, and extension guidance.",
        "sw_symptoms": [
            "mite anaonekana juu ya nyuki mzima",
            "nyuki wana mite na kundi linadhoofika",
            "mite wanaonekana wakati wa ukaguzi",
            "mfugaji anaona nyuki wanaotambaa na mite",
            "varroa inashukiwa baada ya mpangilio mbaya wa majana",
            "mite wanapatikana pamoja na mabawa yaliyoharibika",
        ],
        "sw_advice": "Thibitisha kiwango cha mite kwa ufuatiliaji. Usitumie Carbaryl au Sevin dust ndani ya mzinga. Tumia tu tiba salama kwa nyuki zilizosajiliwa eneo lako kulingana na lebo, mipaka ya joto, na ushauri wa ugani.",
    },
    {
        "condition": "deformed_wing_virus",
        "severity": 2,
        "source": SOURCES["adult_disease"],
        "safety": ["registered_treatment_only", "collect_more_evidence"],
        "en_symptoms": [
            "workers have crumpled wings",
            "bees crawl near the entrance with deformed wings",
            "young bees emerge with damaged wings",
            "several workers cannot fly",
            "deformed wings appear after mite pressure",
            "the beekeeper sees stumpy wings",
        ],
        "en_advice": "This is suspicious for Deformed Wing Virus, often linked with varroa. Check mite levels, brood, adult bees, stores, and colony strength. Do not claim lab confirmation from a photo or single observation.",
        "sw_symptoms": [
            "nyuki wafanyakazi wana mabawa yaliyokunjamana",
            "nyuki wanatambaa mlangoni wakiwa na mabawa mabovu",
            "nyuki wachanga wanatoka na mabawa yaliyoharibika",
            "nyuki kadhaa hawawezi kuruka",
            "mabawa mabovu yanaonekana baada ya shinikizo la mite",
            "mfugaji anaona mabawa mafupi yaliyoharibika",
        ],
        "sw_advice": "Hii inatia shaka kwa Deformed Wing Virus, mara nyingi ikihusishwa na varroa. Kagua kiwango cha mite, majana, nyuki wazima, akiba, na nguvu ya kundi. Usidai uthibitisho wa maabara kutoka picha au uchunguzi mmoja.",
    },
    {
        "condition": "nosema_like_fecal_staining",
        "severity": 1,
        "source": SOURCES["adult_disease"],
        "safety": ["single_image_limit", "collect_more_evidence"],
        "en_symptoms": [
            "brown streaks are on the hive front",
            "fecal marks appear near the entrance",
            "bees are weak and staining the hive",
            "there is diarrhea-like spotting outside",
            "the hive front has many droppings",
            "staining appears after wet stress",
        ],
        "en_advice": "This can be Nosema-like or dysentery staining, but a photo is not lab confirmation. Ask about crawling bees, moisture, colony strength, and recent stress. Clean contaminated surfaces and seek expert testing if losses continue.",
        "sw_symptoms": [
            "mistari ya kahawia iko mbele ya mzinga",
            "alama za kinyesi zinaonekana karibu na mlango",
            "nyuki ni dhaifu na wanachafua mzinga",
            "kuna madoa kama kuharisha nje",
            "mbele ya mzinga ina kinyesi kingi",
            "uchafu unaonekana baada ya msongo wa unyevu",
        ],
        "sw_advice": "Hii inaweza kuwa dalili kama Nosema au kuharisha, lakini picha si uthibitisho wa maabara. Uliza kuhusu nyuki wanaotambaa, unyevu, nguvu ya kundi, na msongo wa karibuni. Safisha sehemu zilizochafuliwa na tafuta upimaji wa mtaalamu kama upotevu unaendelea.",
    },
    {
        "condition": "queen_problem_suspected",
        "severity": 2,
        "source": SOURCES["manual"],
        "safety": ["collect_more_evidence"],
        "en_symptoms": [
            "there are no eggs in the brood area",
            "brood pattern is scattered",
            "the colony sounds restless and has no young larvae",
            "workers are noisy and queen was not seen",
            "there are emergency queen cells",
            "population is falling without clear pest damage",
        ],
        "en_advice": "Suspect a queen problem but confirm by inspection. Check eggs, young larvae, queen cells, brood pattern, and whether the queen is present. Avoid drastic action until the colony status is clear.",
        "sw_symptoms": [
            "hakuna mayai kwenye eneo la majana",
            "mpangilio wa majana umetawanyika",
            "kundi lina sauti isiyotulia na hakuna viluwiluwi wachanga",
            "nyuki wafanyakazi wana kelele na malkia hajaonekana",
            "kuna seli za dharura za malkia",
            "idadi ya nyuki inapungua bila uharibifu wazi wa wadudu",
        ],
        "sw_advice": "Shuku tatizo la malkia lakini thibitisha kwa ukaguzi. Angalia mayai, viluwiluwi wachanga, seli za malkia, mpangilio wa majana, na kama malkia yupo. Epuka hatua kali mpaka hali ya kundi iwe wazi.",
    },
    {
        "condition": "pesticide_misuse_risk",
        "severity": 3,
        "source": SOURCES["pesticide"],
        "safety": ["no_carbaryl", "no_unapproved_insecticide_inside_hive", "avoid_honey_contamination"],
        "en_symptoms": [
            "the beekeeper wants to dust the hive with Carbaryl",
            "someone suggests Sevin dust inside the hive",
            "the farmer wants to spray pesticide on comb",
            "pests are present and the user asks for poison",
            "the beekeeper asks for a chemical shortcut",
            "the hive has pests and honey will be harvested soon",
        ],
        "en_advice": "Do not put Carbaryl, Sevin dust, or ordinary insecticide inside the hive. It can kill bees and contaminate honey and wax. Identify the pest first and use mechanical, IPM, or registered bee-safe treatment options.",
        "sw_symptoms": [
            "mfugaji anataka kunyunyizia Carbaryl ndani ya mzinga",
            "mtu anapendekeza Sevin dust ndani ya mzinga",
            "mkulima anataka kupulizia dawa kwenye sega",
            "wadudu wapo na mtumiaji anauliza sumu",
            "mfugaji anauliza njia ya haraka ya kemikali",
            "mzinga una wadudu na asali itavunwa karibuni",
        ],
        "sw_advice": "Usiweke Carbaryl, Sevin dust, au dawa ya kawaida ya wadudu ndani ya mzinga. Inaweza kuua nyuki na kuchafua asali na nta. Tambua mdudu kwanza na tumia njia za kimwili, IPM, au tiba salama kwa nyuki zilizosajiliwa.",
    },
    {
        "condition": "unknown_or_needs_more_information",
        "severity": 0,
        "source": SOURCES["manual"],
        "safety": ["collect_more_evidence"],
        "en_symptoms": [
            "the photo is blurry",
            "only one bee is visible",
            "the beekeeper gives no season or hive context",
            "the image does not show brood or entrance",
            "the audio is noisy with wind",
            "the symptoms are mixed and unclear",
        ],
        "en_advice": "Ask for more evidence before diagnosing. Request entrance photo, full frame or top-bar photo, closeup of the suspicious sign, county, season, hive type, and what changed recently.",
        "sw_symptoms": [
            "picha haiko wazi",
            "nyuki mmoja tu anaonekana",
            "mfugaji hajatoa msimu au muktadha wa mzinga",
            "picha haionyeshi majana wala mlango",
            "sauti ina kelele za upepo",
            "dalili zimechanganyika na hazieleweki",
        ],
        "sw_advice": "Omba ushahidi zaidi kabla ya kutambua tatizo. Omba picha ya mlango, picha ya fremu au top bar nzima, picha ya karibu ya dalili, county, msimu, aina ya mzinga, na kilichobadilika karibuni.",
    },
]


QUESTION_TEMPLATES = {
    "en": [
        "A Kenyan beekeeper says {symptom}. What should the app advise?",
        "The user reports that {symptom}. Give a safe diagnosis and next step.",
        "If {symptom}, what follow-up questions should BeeCare ask?",
        "Explain this situation simply for a smallholder beekeeper: {symptom}.",
        "What mistake should the model avoid when {symptom}?",
        "Give urgent or non-urgent advice for this case: {symptom}.",
        "How should the answer change if this is happening during drought: {symptom}?",
        "What should the beekeeper do today if {symptom}?",
    ],
    "sw": [
        "Mfugaji wa Kenya anasema {symptom}. Programu ishauri nini?",
        "Mtumiaji anaripoti kuwa {symptom}. Toa utambuzi salama na hatua inayofuata.",
        "Kama {symptom}, BeeCare iulize maswali gani ya kufuatilia?",
        "Eleza hali hii kwa lugha rahisi kwa mfugaji mdogo: {symptom}.",
        "Modeli iepuke kosa gani wakati {symptom}?",
        "Toa ushauri wa dharura au usio wa dharura kwa hali hii: {symptom}.",
        "Jibu libadilikeje kama hali hii inatokea wakati wa ukame: {symptom}?",
        "Mfugaji afanye nini leo kama {symptom}?",
    ],
}


def split_for(index: int) -> str:
    if index % 20 == 0:
        return "test"
    if index % 20 == 10:
        return "validation"
    return "train"


def make_record(record_id: str, language: str, topic: dict, prompt: str, answer: str, split: str) -> dict:
    is_sw = language == "sw"
    return {
        "record_id": record_id,
        "language": language,
        "dataset_status": "public_resource_generated",
        "split": split,
        "modality": "text_only",
        "media": {
            "image_path": None,
            "audio_path": None,
            "source_media_id": f"large_text:{record_id}",
            "sha256": None,
        },
        "location_context": {
            "country": "Kenya",
            "county": None,
            "region_type": "mixed_smallholder_apiary",
            "season_context": "prompt_dependent",
            "collection_date": None,
        },
        "hive_context": {
            "hive_type": "langstroth_or_kenya_top_bar_hive",
            "bee_subspecies": "Apis mellifera scutellata target",
            "frame_removed": None,
            "inspection_context": "Public-resource generated text scenario for Kenyan beekeeping.",
        },
        "labels": {
            "primary_condition": topic["condition"],
            "secondary_conditions": [],
            "task": "advice" if "ushauri" in prompt or "advise" in prompt or "today" in prompt or "leo" in prompt else "differential_diagnosis",
            "severity": topic["severity"],
            "confidence": "medium",
            "lab_confirmed": None,
        },
        "prompt": prompt,
        "target_answer": answer,
        "review": {
            "reviewer_role": "dataset_curator",
            "review_status": "single_reviewed",
            "safety_flags": topic["safety"],
            "notes": "Large public-resource generated bilingual text row. Needs Kenyan expert review before deployment.",
        },
        "provenance": {
            "source_type": "public_guidance_plus_generated_annotation",
            "source_url": topic["source"],
            "license": "Generated Q/A text based on cited public guidance; review before deployment",
            "attribution": "BeeCare generated annotations from public guidance",
            "consent_status": "public_resource_not_field_personal_data",
        },
    }


def main() -> int:
    repo_root = Path(__file__).resolve().parents[4]
    out = repo_root / "training_data/kenya_bee_health/production/manifests/public_resource_bilingual_large_text_manifest.jsonl"
    rows = []
    index = 1
    for topic in TOPICS:
        for language in ("en", "sw"):
            symptoms = topic[f"{language}_symptoms"]
            advice = topic[f"{language}_advice"]
            for symptom in symptoms:
                for template in QUESTION_TEMPLATES[language]:
                    prompt = template.format(symptom=symptom)
                    answer = advice
                    if "mistake" in template or "kosa" in template:
                        prefix = (
                            "The model should avoid guessing from weak evidence or recommending unsafe chemicals. "
                            if language == "en"
                            else "Modeli iepuke kubahatisha bila ushahidi au kupendekeza kemikali hatari. "
                        )
                        answer = prefix + advice
                    rows.append(make_record(f"prb_large_txt_{index:05d}", language, topic, prompt, answer, split_for(index)))
                    index += 1

    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", encoding="utf-8") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")

    print(f"Wrote {len(rows)} large bilingual text records to {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
