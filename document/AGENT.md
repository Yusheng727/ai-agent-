# AGENTS.md

## Scope

This file applies to all files under `document/`.

Write documentation for this directory as project deliverables, not as loose notes.

---

## Mission

Produce technical documentation that is:

- accurate to the repository,
- easy for developers to read,
- easy for AI systems / RAG pipelines to parse,
- consistent with the existing style in this project,
- useful for learning, debugging, review, and interview preparation.

This project may include topics such as:
- Spring AI application development,
- advisor chains,
- chat memory,
- RAG,
- tool-calling,
- testing,
- debugging,
- architecture decisions.

---

## Required Workflow

For non-trivial documentation tasks:

1. Read relevant code, tests, and nearby docs first.
2. Reuse existing terminology and section style.
3. Write from verified implementation, not guesses.
4. Prefer explaining design, call flow, and trade-offs over feature listing.
5. Update `document/README.md` when adding, renaming, or reorganizing docs.
6. State uncertainty explicitly when something is inferred.

---

## Writing Rules

Keep documents:

- structured,
- concise,
- technically precise,
- Markdown-first,
- text-first,
- easy to scan.

Prefer:
- clear headings,
- stable terminology,
- explicit class names,
- explicit file paths,
- explicit config keys,
- short paragraphs,
- small useful lists.

Avoid:
- vague wording,
- decorative prose,
- image-heavy explanations,
- large raw log dumps unless necessary,
- deeply nested lists,
- generic AI-generated text detached from this repository.

---

## Preferred Structure

Use a stable structure when applicable:

1. Background and Goal
2. Main Changes
3. Core Design / Implementation
4. Key Classes / Config
5. Call Flow
6. Testing and Validation
7. Problems / Debugging Notes
8. Trade-offs
9. Future Improvements
10. Summary

For troubleshooting documents, prefer:

1. Symptom
2. Root Cause
3. Investigation
4. Fix
5. Verification
6. Prevention

---

## Accuracy Rules

Before describing:
- class responsibilities,
- advisor behavior,
- memory persistence,
- RAG flow,
- tool-calling flow,
- config behavior,
- test results,
- runtime issues,

verify them from code, tests, or existing grounded docs.

Do not:
- invent implementation details,
- claim behavior you did not verify,
- describe a call chain without checking it.

If something is inferred, mark it clearly.

---

## Style Rules

Match the current documentation style in this directory.

Keep language consistent inside each file.
If the document is mainly Chinese, keep it in Chinese; introduce important English technical terms on first mention when useful.

Use Mermaid diagrams only when they clarify a real flow such as:
- advisor chain,
- memory flow,
- RAG retrieval flow,
- tool-calling flow.

Do not add diagrams just for decoration.

---

## Naming and Navigation

Follow the current naming pattern unless there is a better local convention.

Prefer:
- `partN-主题.md` for phase documents,
- clear topic-based names for deep dives or troubleshooting docs.

Avoid vague names like:
- `notes.md`
- `temp.md`
- `misc.md`

When a new document is added or renamed, update `document/README.md` so the reading order and navigation stay accurate.

---

## Create vs Update

Create a new document when:
- a new project stage begins,
- the topic is large enough to stand alone,
- the topic is a deep dive,
- the troubleshooting content would clutter an existing doc.

Update an existing document when:
- the content belongs to the same phase,
- the change is a clarification,
- file paths, config, or behavior changed within the same topic,
- README navigation needs refresh.

If unsure:
- phase evolution -> update existing phase doc
- deep dive / troubleshooting -> create a new doc

---

## Guardrails

Do not:
- write docs detached from actual code,
- duplicate near-identical documents,
- replace precise names with vague references,
- leave `document/README.md` outdated,
- use images where Markdown text is enough.

If uncertain:
- inspect existing docs first,
- preserve local style,
- make assumptions explicit,
- choose the simpler and more maintainable document structure.