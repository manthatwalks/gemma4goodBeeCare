# Minimum Dataset Targets

Production quality means the model has seen enough local examples to avoid brittle, unsafe advice. These are minimum targets before a serious fine-tune.

## Image Targets

| Category | Minimum examples | Notes |
| --- | ---: | --- |
| Healthy brood/stores frames | 1,000 | Langstroth and KTBH, multiple counties, phone qualities. |
| Normal entrances | 500 | Morning/noon/evening, wet/dry seasons. |
| Heat bearding | 500 | Record temperature/time; include non-swarming labels. |
| True swarming or swarm-prep evidence | 250 | Include queen cells and inspection notes where possible. |
| Drought/dearth/starvation risk | 500 | Pair with hive weight/stores/season notes. |
| Wax moth webbing/damage | 500 | Different stages, weak/abandoned colonies. |
| Small hive beetle adults/larvae/slimeout | 500 | Include slimeout food-safety examples. |
| Safari ants and other ant pressure | 300 | Hives, stands, vegetation bridges, barriers. |
| Varroa visible / DWV | 500 | Worker closeups and brood context. |
| Nosema-like fecal staining | 300 | Mark as "suspected" unless lab-confirmed. |
| Weak colony, unknown cause | 500 | Train the model to ask follow-up questions. |

## Audio Targets

| Category | Minimum duration | Notes |
| --- | ---: | --- |
| Queenright normal colony | 20 hours | Multiple hive types and counties. |
| Queenless or queen problem | 10 hours | Must have inspection confirmation. |
| Drought/dearth/starvation risk | 20 hours | Pair with stores/feeding observations. |
| Heat stress / bearding | 10 hours | Pair with temperature and entrance images. |
| Disturbance, robbing, wind/rain noise | 20 hours | Needed to reduce false alarms. |

## Split Rule

Split by `source_media_id`, apiary, and collection date. Do not put near-duplicate photos from the same inspection in both train and validation/test.

Recommended split:

- Train: 80%
- Validation: 10%
- Test: 10%
- Holdout: separate county/apiary group if possible

## Acceptance Threshold

For a public demo model, at least 2,000 reviewed multimodal rows is acceptable. For real field use, target 6,000-10,000 reviewed rows plus 80+ hours of labeled audio.
