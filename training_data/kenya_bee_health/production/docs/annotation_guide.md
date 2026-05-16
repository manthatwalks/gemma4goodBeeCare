# Annotation Guide

## Reviewer Roles

Use reviewer roles rather than personal names in the public manifest:

- `beekeeper`
- `extension_officer`
- `apiculture_researcher`
- `veterinary_or_agriculture_officer`
- `dataset_curator`

## Labeling Steps

1. Confirm the media belongs to the stated hive/inspection.
2. Choose one `primary_condition`.
3. Add `secondary_conditions` only when visible or strongly supported by context.
4. Set severity:
   - `0`: normal or no action.
   - `1`: monitor or preventive action.
   - `2`: action needed within days.
   - `3`: urgent same-day action.
5. Set confidence:
   - `high`: visible sign is clear or inspection/lab confirms it.
   - `medium`: likely diagnosis, but follow-up would help.
   - `low`: uncertain; answer should ask for more evidence.
6. Add safety flags, especially when the answer must avoid pesticide contamination or false swarm alarms.
7. Write the target answer in farmer-facing language: concise, practical, and safe.

## Diagnostic Standards

- Bearding is not swarming unless there is supporting evidence such as queen cells, mass flight, or swarm departure.
- Nosema-like fecal staining is suspected from images; lab confirmation is needed for a confident disease label.
- Small hive beetle slimeout is also a food-safety issue.
- Wax moth and SHB should trigger weak-colony reasoning, not pest-only advice.
- Varroa/DWV advice must avoid Carbaryl/Sevin dust inside hives.
- Drought/dearth audio needs local validation; do not overclaim from generic acoustic features.
