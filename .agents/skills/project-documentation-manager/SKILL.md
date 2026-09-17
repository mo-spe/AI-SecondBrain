---
name: project-documentation-manager
description: "Project documentation management. Use when tasks produce long-term deliverables — design docs, architecture, API specs, dev plans, test plans, reports — that should be saved as standalone Markdown files in the project's docs/ directory. Handles directory organization, deduplication, versioning, naming conventions, and deprecation cleanup."
---

# Project Documentation Manager

## When to Apply

Use this skill whenever a task produces a **final deliverable** that has long-term reference value:

- Requirements analysis, PRDs, functional design, UI/UX design
- System/technical/deployment architecture
- API / interface design, database design
- Development plans, task breakdowns
- Test plans, test reports
- Research reports, technical evaluations, project summaries

**Skip** for: simple Q&A, code explanations, bug investigations, single-function implementations, or one-off discussions.

---

## Core Workflow

```
Analyze task → Is it a deliverable? → Check existing docs/ structure
    → Check for duplicate/similar docs → Check for stale/temp files
    → Determine target directory → Determine filename
    → Create or update Markdown file → Verify quality
    → Report path + any cleanup suggestions to user
```

---

## Rules

### 1. Output to a file, not just chat

Final deliverables must be saved as standalone `.md` files. The chat response should only summarize: what was done, where it's saved, whether an existing doc was updated, and any stale files found.

### 2. Reuse existing directories

Check the project's actual `docs/` structure first. Prefer existing directories over creating new ones. Common patterns:

```
docs/
├── design/         # Product/feature/UI design
├── architecture/   # System/tech/deployment architecture
├── api/            # API specs, OpenAPI
├── database/       # Schema, ER models
├── tasks/          # Dev plans, task breakdowns
├── testing/        # Test plans, reports
├── reports/        # Research, summaries
├── requirements/   # Requirements analysis, PRDs
└── standards/      # Coding/tech standards
```

**Never** create duplicate directories like `designs/` when `design/` already exists.

### 3. Update existing docs first

Before creating a new document, check for same-topic docs (by filename, title, content theme, or module). Priority:

```
Update existing doc  >  Create new doc
```

Only create a new doc when:
- Old doc is truly obsolete → mark deprecated or delete, then create new
- New doc serves a different purpose (e.g., `system-architecture.md` vs `deployment-architecture.md`)
- Major architectural change requiring parallel version → use `xxx-v2.md` or `xxx-2026.md`

**Never** create: `final.md`, `final2.md`, `new.md`, `new-new.md`, `test.md`, `copy.md`, `xxx_old.md`, `xxx_bak.md`.

### 4. Clean up stale files

Before creating a new doc, check for: `draft.md`, `temp.md`, `tmp.md`, `test.md`, `*-old.md`, `*-bak.md`.

If found: assess whether they can be merged into a formal doc or deleted. If unsure, **ask the user** before deleting.

### 5. File naming

- English, lowercase, `-` separated
- Must convey actual content: `knowledge-graph-design.md`, `api-design.md`
- No meaningless names: `new.md`, `abc.md`, `111.md`

### 6. Document quality

Every output document must:
- Have clear heading hierarchy
- Be complete, coherent, and self-contained
- Use consistent terminology and formatting
- Be readable **without** the chat context

**Never** dump raw chat transcripts as a document. Always restructure into formal prose.

### 7. Document templates by type

| Type | Must include |
|------|-------------|
| **Requirements** | Background, problem, goals, user roles, functional/non-functional requirements, acceptance criteria |
| **Design** | Background, goals, architecture, modules, core flows, data flow, tech choices, risks |
| **API** | Overview, auth, request/response format, examples, error codes |
| **Dev plan** | Goals, task breakdown, priorities, dependencies, timeline, milestones, risks |
| **Test plan** | Objectives, scope, environment, strategy, test cases, acceptance criteria, risks |

### 8. Language

Default to Chinese. Code comments in Chinese. Technical terms (API names, class names, framework names) stay in their original English.

---

## Important Constraints

1. Don't create a new file just because output exists — check first.
2. Prefer updating existing docs over creating duplicates.
3. Don't create duplicate/synonymous directories.
4. Don't use meaningless filenames.
5. Don't mass-produce `final`, `new`, `copy` version files.
6. Don't delete old files without user confirmation.
7. Every document must stand alone without chat context.
8. Treat `docs/` as a long-term engineering asset, not a temporary AI output dump.
