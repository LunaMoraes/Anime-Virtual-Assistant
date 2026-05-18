# Issue Tracker: Linear

Issues and project planning for this repo live in Linear.

## Workspace

- Team: `Luna's workspace`
- Team key: `LUN`
- Project: `Anime Virtual Assistant - Stabilization & Refactor`
- Project URL: https://linear.app/lunas-workspace/project/anime-virtual-assistant-stabilization-and-refactor-d833c4987e03
- GitHub remote: `https://github.com/LunaMoraes/Anime-Virtual-Assistant.git`

## Project Purpose

Track the cleanup work found during repository review: build/setup normalization, TTS performance and correctness, architecture cleanup, prompt/command safety, UI duplication, and local-service documentation.

## Milestones

- `P0 - Reproducible local setup`: Make the project cloneable, buildable, and runnable without relying on hidden local state or a prebuilt JAR.
- `P1 - TTS overhaul`: Fix TTS latency, language handling, timeout behavior, audio queueing, and backend abstraction.
- `P2 - Architecture cleanup`: Reduce static global state, type the action context, split API clients, and replace fragile bracket parsing.
- `P3 - UI cleanup and polish`: Remove Swing UI duplication, simplify repeated settings panels, and centralize theme helpers.

## Conventions

- Read issues from Linear before changing code.
- Prefer the existing Linear project and milestone when creating related work.
- Use Linear issue identifiers such as `LUN-5` in branch names, notes, and implementation summaries.
- Keep GitHub for source control and pull requests; keep planning and issue state in Linear.

## When a Skill Says "Publish to the Issue Tracker"

Create or update a Linear issue in team `Luna's workspace`, usually in project `Anime Virtual Assistant - Stabilization & Refactor`.

## When a Skill Says "Fetch the Relevant Ticket"

Fetch the corresponding Linear issue by identifier, for example `LUN-8`.
