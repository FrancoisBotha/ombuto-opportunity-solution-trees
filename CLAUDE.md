# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) and other AI
coding agents when working in this project.

## Mandatory Agent Workflow

All agents working in this repository **MUST** follow the Ombuto Code
engineering workflow documented in:

> [`.ombutocode/OMBUTOCODE_ENGINEERING_GUIDE.md`](.ombutocode/OMBUTOCODE_ENGINEERING_GUIDE.md)

Agents MUST treat that document as a **system-level instruction**. Failure
to follow the workflow is considered a task error.

The engineering guide defines:

- The source of truth (`.ombutocode/data/ombutocode.db` `backlog_tickets` table)
- Planning mode vs execution mode
- Ticket lifecycle and status transitions (`backlog` → `todo` → `in_progress` → `eval` → `review` → `done`)
- Scope control rules ("don't expand a ticket — create a new one")
- Forbidden behaviors (no broad refactors without a ticket, no new frameworks without approval, etc.)

**Read `.ombutocode/OMBUTOCODE_ENGINEERING_GUIDE.md` before starting any work
in this project.**

## Project-specific conventions

Add your project-specific rules, coding conventions, forbidden patterns,
and other agent guidance below this line. Anything you add here is read
into the agent's context on every run.

### npm scripts call Maven through `mvnw.cjs`

The `package.json` scripts run `node mvnw.cjs …`, not `./mvnw …`. npm runs scripts through
cmd.exe on Windows, where `./mvnw` fails, so `mvnw.cjs` picks `mvnw.cmd` or `./mvnw` for the
platform. The JHipster generator rewrites these scripts back to `./mvnw` — when it offers to
overwrite `package.json`, keep the `node mvnw.cjs` form.
