#!/usr/bin/env python3
"""Generate the best public-resource bilingual dataset we can without field collection.

The rows are grounded in public apiculture guidance and the public seed images in this
repository. They are useful for fine-tuning behavior and safety, but they are not a
replacement for expert-reviewed Kenyan field observations.
"""

from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path


SOURCES = {
    "ilri": "https://www.ilri.org/knowledge/publications/apiculture-training-manual",
    "beeaware_wax_moth": "https://beeaware.org.au/archive-pest/wax-moth-18/",
    "beeaware_shb": "https://beeaware.org.au/archive-pest/small-hive-beetle/",
    "beeaware_pesticide": "https://beeaware.org.au/pollination/pollination-and-pesticides/managing-the-risk-of-honey-bee-poisoning/",
    "govuk_bee_health": "https://www.gov.uk/guidance/bee-health",
    "sasa_adult_diseases": "https://www.sasa.gov.uk/wildlife-environment/bee-health/adult-bee-diseases",
    "plos_losses": "https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0322489",
}


IMAGE_CASES = [
    {
        "key": "healthy_frame",
        "image": "training_data/kenya_bee_health/images/healthy_langstroth_honey_frame.jpg",
        "condition": "healthy_resource_frame",
        "severity": 0,
        "source": SOURCES["ilri"],
        "license": "CC BY-SA 4.0 or GFDL image; generated Q/A text",
        "attribution": "Benlisquare / Wikimedia Commons; BeeCare generated annotations",
        "safety": ["single_image_limit"],
        "prompts_en": [
            "This frame looks full and orderly. What should I tell the beekeeper?",
            "Can the beekeeper harvest after seeing this frame?",
            "What normal features should the model learn from this frame?",
            "The beekeeper says the colony looks calm. Is any treatment needed?",
            "What follow-up checks should be done even if this frame looks healthy?",
        ],
        "answers_en": [
            "This looks like a generally healthy resource frame. There is no obvious webbing, slimeout, or deformed workers in the image. Tell the beekeeper to keep monitoring brood, stores, water, and entrance activity, especially before a dry period.",
            "Do not decide harvest from one frame alone. If the colony has surplus capped honey and enough stores for itself, a small harvest may be possible. During drought or dearth, leave more stores and avoid weakening the colony.",
            "Learn this as a normal reference: orderly comb, capped stores, no greasy fermented honey, no heavy webbing, and no obvious pest tunnels. Still remind the user that one frame cannot prove the whole hive is healthy.",
            "No treatment is visible from this image. The safest answer is monitoring: check brood pattern, eggs or larvae, stores, and water access. Do not apply pesticide or medicine without a clear diagnosis.",
            "Ask whether the queen is laying, whether brood is present, whether the bees cover the comb, whether stores are enough, and whether there are ants, wax moth, small hive beetle, or deformed workers elsewhere.",
        ],
        "prompts_sw": [
            "Fremu hii inaonekana imejaa na imepangika. Nimwambie nini mfugaji?",
            "Je, mfugaji anaweza kuvuna baada ya kuona fremu hii?",
            "Ni dalili gani za kawaida modeli ijifunze kutoka kwenye picha hii?",
            "Mfugaji anasema kundi limetulia. Je, tiba inahitajika?",
            "Ni ukaguzi gani wa kufuatilia ufanywe hata fremu ikionekana nzuri?",
        ],
        "answers_sw": [
            "Hii inaonekana kama fremu yenye akiba nzuri na bila tatizo kubwa linaloonekana. Hakuna utando wa wax moth, asali iliyooza ya small hive beetle, au nyuki wenye mabawa yaliyoharibika kwenye picha. Mshauri mfugaji aendelee kukagua majana, akiba ya chakula, maji, na shughuli mlangoni hasa kabla ya kiangazi.",
            "Asiamue kuvuna kwa kutazama fremu moja pekee. Kama kundi lina asali ya ziada iliyofunikwa na bado lina akiba ya kutosha, anaweza kuvuna kidogo. Wakati wa ukame au uhaba wa maua, aache akiba zaidi ili asidhoofishe kundi.",
            "Modeli ijifunze hii kama mfano wa kawaida: sega limepangika, kuna akiba iliyofunikwa, hakuna asali yenye ute, hakuna utando mzito, na hakuna mashimo makubwa ya wadudu. Lakini fremu moja haiwezi kuthibitisha mzinga mzima uko salama.",
            "Hakuna tiba inayoonekana kuhitajika kutoka kwenye picha hii. Jibu salama ni kufuatilia: angalia mpangilio wa majana, mayai au viluwiluwi, akiba ya chakula, na upatikanaji wa maji. Usitumie dawa bila utambuzi wa wazi.",
            "Uliza kama malkia anataga, kama kuna majana, kama nyuki wanafunika sega, kama chakula kinatosha, na kama kuna siafu, wax moth, small hive beetle, au nyuki wenye mabawa yaliyoharibika sehemu nyingine.",
        ],
    },
    {
        "key": "bearding",
        "image": "training_data/kenya_bee_health/images/bees_bearding.jpg",
        "condition": "heat_bearding",
        "severity": 1,
        "source": SOURCES["ilri"],
        "license": "Public domain image; generated Q/A text",
        "attribution": "Shoefly / Wikimedia Commons; BeeCare generated annotations",
        "safety": ["avoid_false_swarm_alarm", "collect_more_evidence"],
        "prompts_en": [
            "Many bees are clustered outside the hive. Is this swarming?",
            "It is a hot afternoon in the Rift Valley and bees are hanging outside. What should the beekeeper do?",
            "How do I distinguish bearding from absconding?",
            "What should the app avoid saying when it sees this entrance image?",
            "What follow-up question matters most for this photo?",
        ],
        "answers_en": [
            "This looks more like heat bearding than confirmed swarming. Bees often cluster outside to cool the hive. Ask whether bees return inside after evening cooling, whether there are queen cells, and whether a mass of bees is leaving.",
            "Advise clean water near the apiary, shade that does not block airflow, and improved ventilation. Avoid opening the hive during peak heat unless there is an urgent problem. Recheck in the evening.",
            "Bearding usually happens while the colony remains active inside and the cluster shrinks when temperatures fall. Absconding means the colony is leaving or has left. Check whether brood and stores are still covered and whether normal guarding continues.",
            "Do not say this is definitely swarming from the photo alone. Do not tell the beekeeper to destroy queen cells or split the hive unless inspection evidence supports swarming.",
            "Ask: did the cluster reduce after sunset or cooler weather? If yes, heat bearding is more likely than swarming or absconding.",
        ],
        "prompts_sw": [
            "Nyuki wengi wamejikusanya nje ya mzinga. Je, huu ni mtawanyiko wa swarm?",
            "Ni mchana wa joto Rift Valley na nyuki wananing'inia nje. Mfugaji afanye nini?",
            "Nitatofautishaje bearding na kuhama kwa kundi?",
            "Programu iepuke kusema nini ikiona picha hii ya mlango?",
            "Ni swali gani la kufuatilia ni muhimu zaidi kwa picha hii?",
        ],
        "answers_sw": [
            "Hii inaonekana zaidi kama bearding ya joto, si swarming iliyothibitishwa. Nyuki hujikusanya nje ili kupunguza joto ndani ya mzinga. Uliza kama wanarudi ndani jioni, kama kuna seli za malkia, na kama kundi kubwa linaondoka.",
            "Mshauri kuweka maji safi karibu na manzuki, kutoa kivuli kisichozuia hewa, na kuongeza uingizaji hewa. Asifungue mzinga wakati wa joto kali isipokuwa kuna dharura. Akague tena jioni.",
            "Bearding hutokea wakati kundi bado linafanya kazi ndani na mkusanyiko hupungua joto likishuka. Kuhama kwa kundi humaanisha nyuki wanaondoka au tayari wameondoka. Angalia kama majana na akiba bado zinafunikwa na kama ulinzi mlangoni unaendelea.",
            "Programu isiseme huu ni swarming kwa uhakika kutoka picha pekee. Isimwambie mfugaji avunje seli za malkia au agawanye mzinga bila ushahidi wa ukaguzi.",
            "Uliza: je, mkusanyiko ulipungua baada ya jua kuzama au hali kupoa? Kama ndiyo, bearding ya joto inawezekana zaidi kuliko swarming au kuhama.",
        ],
    },
    {
        "key": "wax_moth_webbing",
        "image": "training_data/kenya_bee_health/images/wax_moth_webbing.jpg",
        "condition": "wax_moth_webbing",
        "severity": 2,
        "source": SOURCES["beeaware_wax_moth"],
        "license": "See Wikimedia Commons image page; generated Q/A text",
        "attribution": "Wikimedia Commons contributor; BeeCare generated annotations",
        "safety": ["urgent_inspection", "no_unapproved_insecticide_inside_hive", "collect_more_evidence"],
        "prompts_en": [
            "What pest sign is visible in this comb?",
            "The beekeeper says webbing is spreading but bees are still present. What now?",
            "Can pesticide fix this wax moth problem?",
            "Why is wax moth a weak-colony warning?",
            "What should be done with badly webbed comb?",
        ],
        "answers_en": [
            "The image suggests wax moth webbing. Remove heavily webbed comb, reduce excess hive space, and check why the colony is weak. Strong colonies can usually defend comb better than weak or abandoned colonies.",
            "Act before the colony loses more comb. Remove comb the bees cannot cover, reduce empty space, clean debris, and check for low stores, queen problems, varroa/DWV, ants, or small hive beetle.",
            "Do not spray ordinary pesticide inside the hive. Wax moth management is mostly comb removal, freezing or destroying badly damaged comb, storage hygiene, and strengthening the colony.",
            "Wax moth often becomes serious when bees cannot patrol all comb. That weakness may come from drought, queen failure, disease, varroa, ants, or overlarge hive space.",
            "Freeze salvageable comb if possible, or destroy badly damaged comb. Do not leave infested comb in warm dark storage where moths can multiply.",
        ],
        "prompts_sw": [
            "Ni dalili gani ya mdudu inaonekana kwenye sega hili?",
            "Mfugaji anasema utando unasambaa lakini nyuki bado wapo. Afanye nini?",
            "Je, dawa ya kuua wadudu inaweza kutatua tatizo la wax moth?",
            "Kwa nini wax moth ni onyo la kundi dhaifu?",
            "Sega lenye utando mwingi lifanyiwe nini?",
        ],
        "answers_sw": [
            "Picha inaonyesha dalili za utando wa wax moth. Ondoa masega yaliyoathirika sana, punguza nafasi tupu ndani ya mzinga, na chunguza kwa nini kundi ni dhaifu. Makundi yenye nguvu hulinda sega vizuri kuliko makundi dhaifu au yaliyotelekezwa.",
            "Chukua hatua kabla sega zaidi halijaharibika. Ondoa sega ambalo nyuki hawawezi kufunika, punguza nafasi tupu, safisha uchafu, na angalia kama kuna upungufu wa chakula, tatizo la malkia, varroa/DWV, siafu, au small hive beetle.",
            "Usipulizie dawa ya kawaida ndani ya mzinga. Udhibiti wa wax moth ni kuondoa sega, kugandisha au kuharibu sega lililoharibika sana, kuhifadhi masega vizuri, na kuimarisha kundi.",
            "Wax moth huwa mbaya wakati nyuki hawawezi kulinda sega lote. Udhaifu huo unaweza kutokana na ukame, malkia kushindwa, ugonjwa, varroa, siafu, au nafasi kubwa mno ndani ya mzinga.",
            "Gandisha sega linaloweza kuokolewa kama inawezekana, au haribu sega lililoharibika sana. Usiache sega lenye wadudu mahali penye joto na giza ambako nondo wanaweza kuongezeka.",
        ],
    },
    {
        "key": "wax_moth_damage",
        "image": "training_data/kenya_bee_health/images/wax_moth_damage.jpg",
        "condition": "wax_moth_damage",
        "severity": 2,
        "source": SOURCES["beeaware_wax_moth"],
        "license": "See Wikimedia Commons image page; generated Q/A text",
        "attribution": "Wikimedia Commons contributor; BeeCare generated annotations",
        "safety": ["urgent_inspection", "no_unapproved_insecticide_inside_hive"],
        "prompts_en": [
            "This comb has tunnels and ragged damage. Is it safe to ignore?",
            "How should a KTBH beekeeper manage comb that looks like this?",
            "What caused this damage?",
            "What should the beekeeper check after finding wax moth damage?",
            "How can stored comb be protected from this problem?",
        ],
        "answers_en": [
            "Do not ignore it. Tunnels and ragged wax damage suggest wax moth activity or abandoned comb. Remove badly damaged comb and reduce space so bees can defend what remains.",
            "For a Kenya Top Bar Hive, remove the affected top bars carefully, keep only comb the colony can cover, and clean debris. If the colony is very weak, seek local advice about combining or re-hiving.",
            "The likely cause is wax moth larvae feeding through comb, especially when the colony is weak or comb is stored unprotected.",
            "Check colony strength, queen status, food stores, varroa/DWV signs, ants, and small hive beetle. Wax moth damage is often a symptom of a deeper problem.",
            "Store comb in clean, dry, well-ventilated conditions, protect it from moth entry, and avoid leaving old dark comb unattended in warm dark places.",
        ],
        "prompts_sw": [
            "Sega hili lina mashimo na uharibifu. Je, ni salama kupuuza?",
            "Mfugaji wa KTBH ashughulikieje sega linaloonekana hivi?",
            "Ni nini kimesababisha uharibifu huu?",
            "Mfugaji aangalie nini baada ya kupata uharibifu wa wax moth?",
            "Masega yaliyohifadhiwa yatalindwaje dhidi ya tatizo hili?",
        ],
        "answers_sw": [
            "Usipuuze. Mashimo na uharibifu wa sega vinaashiria wax moth au sega lililoachwa bila kulindwa. Ondoa sega lililoharibika sana na punguza nafasi ili nyuki walinde kilichobaki.",
            "Kwa Kenya Top Bar Hive, ondoa top bars zilizoathirika kwa uangalifu, acha sega ambalo kundi linaweza kufunika, na safisha uchafu. Kama kundi ni dhaifu sana, tafuta ushauri wa kuunganisha au kuhamisha kundi.",
            "Sababu inayowezekana ni viluwiluwi wa wax moth kula ndani ya sega, hasa kundi likiwa dhaifu au sega likihifadhiwa bila kinga.",
            "Angalia nguvu ya kundi, hali ya malkia, akiba ya chakula, dalili za varroa/DWV, siafu, na small hive beetle. Uharibifu wa wax moth mara nyingi ni dalili ya tatizo la ndani zaidi.",
            "Hifadhi masega mahali safi, pakavu, penye hewa, yalindwe nondo wasiingie, na usiache sega la zamani lenye giza mahali pa joto na giza.",
        ],
    },
    {
        "key": "small_hive_beetle",
        "image": "training_data/kenya_bee_health/images/small_hive_beetle_larvae.jpg",
        "condition": "small_hive_beetle_larvae",
        "severity": 2,
        "source": SOURCES["beeaware_shb"],
        "license": "CC BY 3.0 US image; generated Q/A text",
        "attribution": "James D. Ellis, University of Florida / Bugwood.org; BeeCare generated annotations",
        "safety": ["food_safety_warning", "urgent_inspection", "no_unapproved_insecticide_inside_hive"],
        "prompts_en": [
            "Is this wax moth or small hive beetle?",
            "The honey looks greasy and is leaking from cells. What should the app say?",
            "Can the beekeeper harvest honey from comb like this?",
            "What management steps reduce small hive beetle damage?",
            "Why does small hive beetle point to colony weakness?",
        ],
        "answers_en": [
            "The image is consistent with small hive beetle larvae, though larvae can be confused with wax moth. SHB larvae can slime comb and ferment honey; wax moth usually leaves more dry silken webbing.",
            "Warn about possible slimeout. Remove badly slimed comb, do not harvest affected honey for food, reduce hive space, clean debris, and strengthen the colony so bees can patrol comb.",
            "No. Honey from slimed or fermented comb should not be harvested for food. Remove affected comb and protect clean comb from contamination.",
            "Keep colonies strong, reduce excess space, remove burr comb and debris, place hives in suitable dry sunny locations where practical, and handle infested comb quickly.",
            "Serious SHB damage often develops when bees cannot control larvae and beetles. Check for queen problems, low stores, drought stress, wax moth, ants, and varroa/DWV.",
        ],
        "prompts_sw": [
            "Hii ni wax moth au small hive beetle?",
            "Asali inaonekana yenye mafuta na inavuja kwenye seli. Programu iseme nini?",
            "Je, mfugaji anaweza kuvuna asali kutoka sega kama hili?",
            "Ni hatua gani hupunguza madhara ya small hive beetle?",
            "Kwa nini small hive beetle inaonyesha uwezekano wa kundi dhaifu?",
        ],
        "answers_sw": [
            "Picha inaendana na viluwiluwi wa small hive beetle, ingawa wanaweza kuchanganywa na wax moth. SHB wanaweza kufanya sega liwe na ute na asali ichache; wax moth kawaida huacha utando mkavu zaidi.",
            "Onya kuhusu slimeout. Ondoa sega lenye ute mwingi, usivune asali iliyoathirika kwa chakula, punguza nafasi ndani ya mzinga, safisha uchafu, na imarisha kundi ili nyuki walinde sega.",
            "Hapana. Asali kutoka sega lenye ute au iliyochacha haifai kuvunwa kwa chakula. Ondoa sega lililoathirika na linda sega safi lisichafuliwe.",
            "Weka makundi yenye nguvu, punguza nafasi tupu, ondoa burr comb na uchafu, weka mizinga sehemu kavu yenye mwanga inapowezekana, na shughulikia sega lililoathirika haraka.",
            "Madhara makubwa ya SHB hutokea nyuki wakishindwa kudhibiti viluwiluwi na mende. Angalia tatizo la malkia, chakula kidogo, ukame, wax moth, siafu, na varroa/DWV.",
        ],
    },
    {
        "key": "safari_ants",
        "image": "training_data/kenya_bee_health/images/safari_ants_kenya.jpg",
        "condition": "safari_ants_siafu",
        "severity": 3,
        "source": SOURCES["ilri"],
        "license": "CC BY 3.0 image; generated Q/A text",
        "attribution": "Bartolucci / Wikimedia Commons; BeeCare generated annotations",
        "safety": ["urgent_inspection", "avoid_honey_contamination"],
        "prompts_en": [
            "The image shows safari ants. Why does this matter for a Kenyan hive?",
            "Ants are climbing the hive stand. What immediate action is safest?",
            "Should engine oil be put inside the hive to stop ants?",
            "Can ants explain an empty hive after drought?",
            "What should be checked after ants are blocked?",
        ],
        "answers_en": [
            "Safari ants, or siafu, can overwhelm weak colonies and may push bees to abandon a hive. Treat this as urgent, especially if ants are reaching the hive body.",
            "Remove vegetation bridges, isolate stand legs, use safe external barriers or ash where appropriate, and move a threatened weak colony if needed. Keep barriers outside the hive and away from honey and comb.",
            "No. Do not put engine oil or toxic chemicals inside the hive. Any barrier must stay outside the hive and must not contaminate honey, wax, bees, or soil.",
            "Ants can contribute, but drought, starvation, heat stress, queen failure, wax moth, SHB, and disturbance can also cause absconding. Ask for frame photos and recent hive history.",
            "Check whether the colony is queenright, strong enough to cover comb, short of food, or already invaded by wax moth or small hive beetle.",
        ],
        "prompts_sw": [
            "Picha inaonyesha siafu. Kwa nini ni muhimu kwa mzinga wa Kenya?",
            "Siafu wanapanda standi ya mzinga. Hatua gani ya haraka ni salama?",
            "Je, oil ya injini iwekwe ndani ya mzinga kuzuia siafu?",
            "Je, siafu wanaweza kueleza mzinga kuwa mtupu baada ya ukame?",
            "Baada ya kuzuia siafu, nini kiangaliwe?",
        ],
        "answers_sw": [
            "Siafu wanaweza kuzidi nguvu kundi dhaifu na kusababisha nyuki kuhama. Chukulia hili kama jambo la dharura, hasa kama siafu wamefika kwenye mzinga wenyewe.",
            "Ondoa nyasi au matawi yanayogusa mzinga, tenga miguu ya standi, tumia vizuizi salama vya nje au majivu inapofaa, na hamisha kundi dhaifu lililo hatarini kama lazima. Vizuizi vibaki nje ya mzinga na mbali na asali na sega.",
            "Hapana. Usiweke oil ya injini au kemikali zenye sumu ndani ya mzinga. Kizuizi chochote kikae nje na kisichafue asali, nta, nyuki, au udongo.",
            "Siafu wanaweza kuchangia, lakini ukame, njaa, joto, tatizo la malkia, wax moth, SHB, na usumbufu pia vinaweza kusababisha kundi kuhama. Omba picha za fremu na historia ya hivi karibuni.",
            "Angalia kama kundi lina malkia anayetaga, lina nguvu ya kufunika sega, lina chakula kidogo, au tayari limevamiwa na wax moth au small hive beetle.",
        ],
    },
    {
        "key": "varroa_dwv",
        "image": "training_data/kenya_bee_health/images/dwv_varroa_worker.jpg",
        "condition": "varroa_visible",
        "severity": 2,
        "source": SOURCES["govuk_bee_health"],
        "license": "CC0 1.0 image; generated Q/A text",
        "attribution": "Stefan de Konink / Wikimedia Commons; BeeCare generated annotations",
        "safety": ["no_carbaryl", "registered_treatment_only", "follow_label_and_extension_advice"],
        "prompts_en": [
            "What disease or pest signal is visible on this worker?",
            "The farmer wants to use Sevin Dudu Dust after seeing bees like this. What should the answer be?",
            "What follow-up inspection is needed after seeing deformed wings and mites?",
            "Why does this make wax moth or SHB more likely later?",
            "How should treatment advice be worded safely?",
        ],
        "answers_en": [
            "The photo shows deformed wings and a visible Varroa mite. This is an unhealthy sign and suggests varroa-associated virus pressure such as Deformed Wing Virus.",
            "Do not use Sevin Dudu Dust or Carbaryl inside the hive. It can poison bees and contaminate hive products. Recommend mite monitoring and locally approved bee-safe treatment according to label and extension guidance.",
            "Inspect more adult bees, brood pattern, mite levels, crawling workers, colony strength, stores, and whether pests have begun invading weak comb.",
            "Varroa and DWV weaken adult bees and brood. A weakened colony may no longer defend comb well, giving wax moths, SHB, and ants a chance to take over.",
            "Say: confirm mite pressure first, then use only registered bee-safe products at label dose and temperature. If uncertain, contact a local extension officer or experienced beekeeper.",
        ],
        "prompts_sw": [
            "Ni dalili gani ya ugonjwa au mdudu inaonekana kwa nyuki huyu?",
            "Mkulima anataka kutumia Sevin Dudu Dust baada ya kuona nyuki kama hawa. Jibu liwe nini?",
            "Ni ukaguzi gani wa kufuatilia unahitajika baada ya kuona mabawa yaliyoharibika na mite?",
            "Kwa nini hali hii inaweza kuongeza wax moth au SHB baadaye?",
            "Ushauri wa tiba uandikweje kwa usalama?",
        ],
        "answers_sw": [
            "Picha inaonyesha mabawa yaliyoharibika na Varroa mite anayeonekana. Hii ni dalili isiyo nzuri na inaashiria shinikizo la varroa na virusi kama Deformed Wing Virus.",
            "Usitumie Sevin Dudu Dust au Carbaryl ndani ya mzinga. Inaweza kuua nyuki na kuchafua mazao ya mzinga. Pendekeza kufuatilia kiwango cha mite na kutumia tiba salama iliyosajiliwa kwa nyuki kulingana na lebo na ushauri wa ugani.",
            "Kagua nyuki wengine wazima, mpangilio wa majana, kiwango cha mite, nyuki wanaotambaa, nguvu ya kundi, akiba ya chakula, na kama wadudu wameanza kuvamia sega dhaifu.",
            "Varroa na DWV hudhoofisha nyuki wazima na majana. Kundi dhaifu haliwezi kulinda sega vizuri, hivyo wax moth, SHB, na siafu wanaweza kuchukua nafasi.",
            "Sema: thibitisha kwanza shinikizo la mite, kisha tumia bidhaa salama kwa nyuki zilizosajiliwa kwa kipimo na joto lililo kwenye lebo. Ukiwa na shaka, wasiliana na afisa ugani au mfugaji mwenye uzoefu.",
        ],
    },
    {
        "key": "dwv",
        "image": "training_data/kenya_bee_health/images/dwv_worker.jpg",
        "condition": "deformed_wing_virus",
        "severity": 2,
        "source": SOURCES["sasa_adult_diseases"],
        "license": "CC BY-SA 3.0 image; generated Q/A text",
        "attribution": "Xolani90 / Wikimedia Commons; BeeCare generated annotations",
        "safety": ["registered_treatment_only", "collect_more_evidence"],
        "prompts_en": [
            "The bee has stumpy, deformed wings. What should the assistant say?",
            "Are a few deformed bees enough to diagnose the whole colony?",
            "What practical advice should follow this image?",
            "What should the model avoid overclaiming?",
            "How does this connect to colony collapse risk?",
        ],
        "answers_en": [
            "This is suspicious for Deformed Wing Virus, often linked with varroa pressure. Repeated sightings mean the colony needs closer inspection.",
            "No. One image is not enough to diagnose the whole colony. Ask whether many workers are crawling or deformed and whether mites are being monitored.",
            "Check adult bees, brood, mite levels, stores, and colony strength. Use only locally approved treatments and follow product labels.",
            "Do not claim laboratory confirmation from a photo. Say it is suspected DWV and needs inspection or expert confirmation if severe.",
            "DWV and varroa can weaken a colony until it cannot defend against wax moth, SHB, ants, or drought stress.",
        ],
        "prompts_sw": [
            "Nyuki ana mabawa mafupi yaliyoharibika. Msaidizi aseme nini?",
            "Je, nyuki wachache wenye mabawa mabovu wanatosha kutambua kundi lote?",
            "Ni ushauri gani wa vitendo ufuate picha hii?",
            "Modeli iepuke kudai nini kupita kiasi?",
            "Hii inahusianaje na hatari ya kundi kuanguka?",
        ],
        "answers_sw": [
            "Hii inatia shaka kwa Deformed Wing Virus, mara nyingi ikihusishwa na varroa. Ukiona mara kwa mara, kundi linahitaji ukaguzi wa karibu zaidi.",
            "Hapana. Picha moja haitoshi kutambua kundi lote. Uliza kama nyuki wengi wanatambaa au wameharibika mabawa na kama mite wanafuatiliwa.",
            "Kagua nyuki wazima, majana, kiwango cha mite, akiba ya chakula, na nguvu ya kundi. Tumia tu tiba zilizoidhinishwa eneo lako na fuata lebo.",
            "Usidai uthibitisho wa maabara kutoka picha. Sema ni tuhuma ya DWV na inahitaji ukaguzi au uthibitisho wa mtaalamu kama ni kali.",
            "DWV na varroa zinaweza kudhoofisha kundi mpaka lishindwe kujilinda dhidi ya wax moth, SHB, siafu, au msongo wa ukame.",
        ],
    },
]


TEXT_CASES = [
    (
        "drought_feeding",
        "drought_dearth_starvation_risk",
        2,
        SOURCES["plos_losses"],
        ["collect_more_evidence", "follow_label_and_extension_advice"],
        [
            ("en", "A beekeeper in an ASAL area says the hive is quieter and stores are low. What should the app advise?",
             "Treat this as a dearth or starvation-risk warning. Ask the beekeeper to check stores, brood coverage, and entrance traffic. Provide clean water and consider 1:1 sugar syrup for emergency support if stores are low. Recheck soon because drought stress can lead to absconding."),
            ("sw", "Mfugaji katika eneo la ASAL anasema mzinga umetulia sana na chakula kimepungua. Programu ishauri nini?",
             "Chukulia hii kama onyo la uhaba wa malisho au hatari ya njaa. Mwambie akague akiba ya chakula, majana yanayofunikwa, na shughuli mlangoni. Aweke maji safi na afikirie syrup ya sukari 1:1 kwa msaada wa dharura kama akiba ni ndogo. Akague tena mapema kwa sababu ukame unaweza kusababisha kundi kuhama."),
        ],
    ),
    (
        "absconding_empty_hive",
        "post_absconding_empty_hive",
        2,
        SOURCES["plos_losses"],
        ["collect_more_evidence", "urgent_inspection"],
        [
            ("en", "The beekeeper opens the hive after drought and finds almost no bees. What diagnosis should be considered?",
             "Possible absconding, but do not assume one cause. Check for remaining stores, dead brood, wax moth, small hive beetle slimeout, ants, queen problems, recent harvest, pesticide exposure, and disturbance. Clean the hive before re-baiting and fix the likely cause."),
            ("sw", "Mfugaji anafungua mzinga baada ya ukame na anakuta karibu hakuna nyuki. Ni utambuzi gani uzingatiwe?",
             "Inaweza kuwa kundi limehama, lakini usifikirie sababu moja pekee. Angalia akiba iliyobaki, majana yaliyokufa, wax moth, ute wa small hive beetle, siafu, tatizo la malkia, mavuno ya karibuni, sumu ya dawa, na usumbufu. Safisha mzinga kabla ya kuuvutia tena na shughulikia chanzo kinachowezekana."),
        ],
    ),
    (
        "nosema_fecal_staining",
        "nosema_like_fecal_staining",
        1,
        SOURCES["sasa_adult_diseases"],
        ["collect_more_evidence", "single_image_limit"],
        [
            ("en", "There are brown streaks on the hive front. Is it definitely Nosema?",
             "Brown fecal streaking can be consistent with dysentery or Nosema-like signs, but a photo alone is not laboratory confirmation. Ask about weak crawling bees, colony strength, moisture, and recent stress. Recommend cleaning contaminated equipment and seeking expert testing if losses continue."),
            ("sw", "Kuna mistari ya kinyesi kahawia mbele ya mzinga. Je, ni Nosema kwa uhakika?",
             "Mistari ya kinyesi inaweza kuendana na dalili kama Nosema, lakini picha pekee si uthibitisho wa maabara. Uliza kuhusu nyuki dhaifu wanaotambaa, nguvu ya kundi, unyevu, na msongo wa karibuni. Pendekeza kusafisha vifaa vilivyochafuliwa na kutafuta upimaji wa mtaalamu kama upotevu unaendelea."),
        ],
    ),
    (
        "queenless_audio",
        "queen_problem_suspected",
        2,
        SOURCES["sasa_adult_diseases"],
        ["collect_more_evidence"],
        [
            ("en", "The beekeeper says the hive sound is restless and there are no eggs. What should be asked next?",
             "Ask how long eggs have been absent, whether young larvae are present, whether the colony has queen cells, and whether the queen was seen. A restless sound plus no eggs may suggest queen trouble, but confirm by brood inspection before giving management advice."),
            ("sw", "Mfugaji anasema sauti ya mzinga si tulivu na hakuna mayai. Ni nini kiulizwe?",
             "Uliza mayai yamekosekana kwa muda gani, kama kuna viluwiluwi wachanga, kama kuna seli za malkia, na kama malkia ameonekana. Sauti isiyotulia pamoja na kukosekana kwa mayai inaweza kuashiria tatizo la malkia, lakini thibitisha kwa ukaguzi wa majana kabla ya ushauri wa usimamizi."),
        ],
    ),
    (
        "pesticide_misuse",
        "pesticide_misuse_risk",
        3,
        SOURCES["beeaware_pesticide"],
        ["no_carbaryl", "no_unapproved_insecticide_inside_hive", "avoid_honey_contamination"],
        [
            ("en", "A farmer asks if Carbaryl dust can be sprinkled inside the hive to kill pests. What is the safe answer?",
             "No. Do not put Carbaryl, Sevin dust, or ordinary insecticide inside the hive. It can poison bees and contaminate honey and wax. Identify the pest first, use mechanical or IPM steps where possible, and use only registered bee-safe products according to label and local extension guidance."),
            ("sw", "Mkulima anauliza kama Carbaryl au Sevin dust inaweza kunyunyiziwa ndani ya mzinga kuua wadudu. Jibu salama ni lipi?",
             "Hapana. Usiweke Carbaryl, Sevin dust, au dawa ya kawaida ya wadudu ndani ya mzinga. Inaweza kuua nyuki na kuchafua asali na nta. Tambua mdudu kwanza, tumia njia za usimamizi na IPM inapowezekana, na tumia tu bidhaa salama kwa nyuki zilizosajiliwa kulingana na lebo na ushauri wa ugani."),
        ],
    ),
    (
        "robbing_vs_normal",
        "weak_colony_unknown_cause",
        2,
        SOURCES["ilri"],
        ["collect_more_evidence", "urgent_inspection"],
        [
            ("en", "There is fighting at the entrance and many bees are rushing in and out. Is this normal traffic?",
             "It may be robbing, not normal traffic. Ask whether bees are fighting, whether wax cappings are scattered, whether the colony is weak, and whether feeding or spilled honey triggered robbing. Reduce the entrance, stop open feeding, and protect weak colonies."),
            ("sw", "Kuna mapigano mlangoni na nyuki wengi wanaingia na kutoka haraka. Je, hii ni shughuli ya kawaida?",
             "Inaweza kuwa uporaji wa asali, si shughuli ya kawaida. Uliza kama nyuki wanapigana, kama kuna vipande vya nta vilivyotapakaa, kama kundi ni dhaifu, na kama kulisha au asali iliyomwagika kumechochea uporaji. Punguza mlango, acha kulisha wazi, na linda makundi dhaifu."),
        ],
    ),
    (
        "movable_frame_photos",
        "unknown_or_needs_more_information",
        0,
        SOURCES["ilri"],
        ["collect_more_evidence"],
        [
            ("en", "What photos should a Langstroth or KTBH beekeeper send for a better diagnosis?",
             "Ask for four photos: hive entrance, full frame or top bar, closeup of the suspicious sign, and a normal comparison comb if available. Add context: county, season, hive type, whether stores are low, and what changed recently."),
            ("sw", "Mfugaji wa Langstroth au KTBH atume picha gani ili utambuzi uwe bora?",
             "Omba picha nne: mlango wa mzinga, fremu au top bar nzima, picha ya karibu ya dalili inayotia shaka, na sega la kawaida la kulinganisha kama lipo. Aongeze muktadha: county, msimu, aina ya mzinga, kama chakula kimepungua, na kilichobadilika karibuni."),
        ],
    ),
    (
        "stored_comb",
        "wax_moth_damage",
        1,
        SOURCES["beeaware_wax_moth"],
        ["no_unapproved_insecticide_inside_hive"],
        [
            ("en", "How should unused comb be stored to reduce wax moth damage?",
             "Store comb clean, dry, and protected from moth entry. Avoid warm, dark, poorly ventilated storage. Freeze suspect comb where possible and do not keep badly damaged comb near active hives."),
            ("sw", "Masega yasiyotumika yahifadhiweje kupunguza uharibifu wa wax moth?",
             "Hifadhi masega yakiwa safi, makavu, na yamelindwa nondo wasiingie. Epuka sehemu za joto, giza, na zisizo na hewa. Gandisha sega linalotiliwa shaka inapowezekana na usiweke sega lililoharibika karibu na mizinga yenye nyuki."),
        ],
    ),
    (
        "treatment_legal",
        "varroa_visible",
        2,
        SOURCES["govuk_bee_health"],
        ["registered_treatment_only", "follow_label_and_extension_advice", "no_carbaryl"],
        [
            ("en", "The app suspects varroa. Should it give a chemical recipe?",
             "No. It should recommend confirming mite pressure and using only locally registered bee-safe treatments according to the product label, temperature limits, and extension guidance. It should not invent doses or recommend agricultural insecticides inside the hive."),
            ("sw", "Programu inashuku varroa. Je, itoe mchanganyiko wa kemikali?",
             "Hapana. Ipendekeze kuthibitisha kiwango cha mite na kutumia tu tiba salama kwa nyuki zilizosajiliwa eneo hilo kulingana na lebo, mipaka ya joto, na ushauri wa ugani. Isibuni vipimo wala kupendekeza dawa za kawaida za kilimo ndani ya mzinga."),
        ],
    ),
    (
        "shb_slimeout",
        "small_hive_beetle_slimeout",
        3,
        SOURCES["beeaware_shb"],
        ["food_safety_warning", "urgent_inspection", "no_unapproved_insecticide_inside_hive"],
        [
            ("en", "A comb smells fermented and honey is wet and slimy. What is the urgent advice?",
             "This is suspicious for small hive beetle slimeout. Do not harvest affected honey for food. Remove badly slimed comb, protect clean comb, reduce hive space, clean debris, and inspect colony strength immediately."),
            ("sw", "Sega linanuka kuchacha na asali ni yenye maji na ute. Ushauri wa dharura ni upi?",
             "Hii inatia shaka kwa slimeout ya small hive beetle. Usivune asali iliyoathirika kwa chakula. Ondoa sega lenye ute mwingi, linda sega safi, punguza nafasi ndani ya mzinga, safisha uchafu, na kagua nguvu ya kundi mara moja."),
        ],
    ),
]


FOLLOWUP_PROMPTS = {
    "en": [
        "What should the beekeeper check next?",
        "Give a short farmer-facing answer.",
        "What is the safest immediate action?",
        "What mistake should the model avoid?",
    ],
    "sw": [
        "Mfugaji aangalie nini baadaye?",
        "Toa jibu fupi kwa mfugaji.",
        "Hatua salama ya haraka ni ipi?",
        "Modeli iepuke kosa gani?",
    ],
}


def sha256(path: Path) -> str | None:
    if not path.exists():
        return None
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def split_for(counter: int, is_image: bool) -> str:
    if is_image:
        return "train"
    if counter % 10 == 0:
        return "test"
    if counter % 10 == 5:
        return "validation"
    return "train"


def location_for(condition: str) -> dict:
    if condition in {"drought_dearth_starvation_risk", "heat_bearding", "possible_absconding", "post_absconding_empty_hive"}:
        return {"country": "Kenya", "county": "Baringo", "region_type": "ASAL", "season_context": "hot_dry_or_drought", "collection_date": None}
    if condition == "safari_ants_siafu":
        return {"country": "Kenya", "county": "Kakamega", "region_type": "western_highlands_or_forest_edge", "season_context": "rainy_or_mixed", "collection_date": None}
    return {"country": "Kenya", "county": None, "region_type": "mixed_smallholder_apiary", "season_context": "unknown_or_prompt_context", "collection_date": None}


def hive_for(condition: str) -> dict:
    frame_removed = condition not in {"heat_bearding", "safari_ants_siafu", "possible_absconding"}
    return {
        "hive_type": "langstroth_or_kenya_top_bar_hive",
        "bee_subspecies": "Apis mellifera scutellata target; public source images may differ",
        "frame_removed": frame_removed,
        "inspection_context": "Public-resource generated training scenario for Kenyan smallholder beekeeping.",
    }


def make_record(
    record_id: str,
    split: str,
    language: str,
    modality: str,
    condition: str,
    severity: int,
    task: str,
    prompt: str,
    answer: str,
    safety: list[str],
    source_url: str,
    media: dict,
    license_text: str,
    attribution: str,
) -> dict:
    return {
        "record_id": record_id,
        "language": language,
        "dataset_status": "public_resource_generated",
        "split": split,
        "modality": modality,
        "media": media,
        "location_context": location_for(condition),
        "hive_context": hive_for(condition),
        "labels": {
            "primary_condition": condition,
            "secondary_conditions": [],
            "task": task,
            "severity": severity,
            "confidence": "medium",
            "lab_confirmed": None,
        },
        "prompt": prompt,
        "target_answer": answer,
        "review": {
            "reviewer_role": "dataset_curator",
            "review_status": "single_reviewed",
            "safety_flags": sorted(set(safety)),
            "notes": "Generated from public apiculture guidance and public seed media. Needs Kenyan expert review before field deployment.",
        },
        "provenance": {
            "source_type": "public_guidance_plus_generated_annotation",
            "source_url": source_url,
            "license": license_text,
            "attribution": attribution,
            "consent_status": "public_resource_not_field_personal_data",
        },
    }


def main() -> int:
    repo_root = Path(__file__).resolve().parents[4]
    data_dir = repo_root / "training_data/kenya_bee_health"
    output_path = data_dir / "production/manifests/public_resource_bilingual_manifest.jsonl"
    output_path.parent.mkdir(parents=True, exist_ok=True)

    rows: list[dict] = []
    counter = 1

    for case in IMAGE_CASES:
        image_path = repo_root / case["image"]
        for language in ("en", "sw"):
            prompts = case[f"prompts_{language}"]
            answers = case[f"answers_{language}"]
            for idx, (prompt, answer) in enumerate(zip(prompts, answers), start=1):
                media = {
                    "image_path": case["image"],
                    "audio_path": None,
                    "source_media_id": f"public_image:{Path(case['image']).stem}",
                    "sha256": sha256(image_path),
                }
                rows.append(
                    make_record(
                        f"prb_img_{counter:04d}",
                        "train",
                        language,
                        "image",
                        case["condition"],
                        case["severity"],
                        "visual_triage" if idx <= 2 else "differential_diagnosis",
                        prompt,
                        answer,
                        case["safety"],
                        case["source"],
                        media,
                        case["license"],
                        case["attribution"],
                    )
                )
                counter += 1

    text_counter = 1
    for key, condition, severity, source_url, safety, bilingual_rows in TEXT_CASES:
        for language, base_prompt, base_answer in bilingual_rows:
            for variant_idx, followup in enumerate(FOLLOWUP_PROMPTS[language], start=1):
                if variant_idx == 1:
                    prompt = base_prompt
                    answer = base_answer
                    task = "advice"
                elif variant_idx == 2:
                    prompt = f"{base_prompt} {followup}"
                    answer = base_answer
                    task = "advice"
                elif variant_idx == 3:
                    prompt = followup + " " + base_prompt
                    answer = base_answer
                    task = "follow_up_questions" if "check" in followup or "aangalie" in followup else "advice"
                else:
                    prompt = followup + " " + base_prompt
                    avoid_en = "The model should avoid overclaiming certainty, recommending unsafe pesticides inside the hive, or ignoring missing evidence. "
                    avoid_sw = "Modeli iepuke kudai uhakika bila ushahidi, kupendekeza dawa hatari ndani ya mzinga, au kupuuza taarifa zinazokosekana. "
                    answer = (avoid_sw if language == "sw" else avoid_en) + base_answer
                    task = "counterfactual_safety"

                media = {
                    "image_path": None,
                    "audio_path": None,
                    "source_media_id": f"public_text:{key}:{language}:{variant_idx}",
                    "sha256": None,
                }
                rows.append(
                    make_record(
                        f"prb_txt_{text_counter:04d}",
                        split_for(text_counter, is_image=False),
                        language,
                        "text_only",
                        condition,
                        severity,
                        task,
                        prompt,
                        answer,
                        safety,
                        source_url,
                        media,
                        "Generated Q/A text based on cited public guidance; review before deployment",
                        "BeeCare generated annotations from public guidance",
                    )
                )
                text_counter += 1

    with output_path.open("w", encoding="utf-8") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")

    print(f"Wrote {len(rows)} bilingual public-resource records to {output_path}")
    print("Image rows: 80; text rows: 80")
    return 0


if __name__ == "__main__":
    sys.exit(main())
