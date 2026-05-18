# Triage Labels

The skills speak in terms of five canonical triage roles. This repo uses Linear as the tracker, so the mapping below records the closest Linear status or label vocabulary to use.

| Label in mattpocock/skills | Linear equivalent | Meaning |
| -------------------------- | ----------------- | ------- |
| `needs-triage` | Status `Backlog` | Maintainer needs to evaluate this issue |
| `needs-info` | Label `needs-info` if created, otherwise leave a clear issue comment | Waiting on reporter for more information |
| `ready-for-agent` | Status `Todo` in the active Linear project | Fully specified, ready for an AFK agent |
| `ready-for-human` | Status `Todo` with an explicit human-needed note | Requires human implementation |
| `wontfix` | Status `Canceled` | Will not be actioned |

## Existing Linear Statuses

- `Backlog`
- `Todo`
- `In Progress`
- `In Review`
- `Done`
- `Duplicate`
- `Canceled`

## Existing Linear Labels

- `setup`: Project setup, local development, build system, dependency pinning, and documentation
- `tts`: Text-to-speech latency, backend selection, language handling, playback queue, and audio quality
- `architecture`: Java architecture, state management, action system, API clients, and parsing contracts
- `ui`: Swing UI, settings panels, overlays, styling duplication, and interaction fixes
- `docs`: README, setup guides, service matrix, architecture notes, and developer handoff docs
- `Bug`
- `Feature`
- `Improvement`

When a skill mentions a role, use the corresponding Linear equivalent from the table. Do not create new Linear labels unless the user asks for label creation or the current task explicitly requires it.
