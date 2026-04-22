# Repository Guidelines

## Project Structure & Module Organization
This repository is a Vue 3 + Vite frontend (`ant-live-web`).

- `src/main.js`: app bootstrap.
- `src/router/`: route definitions.
- `src/views/`: page-level screens (for example `system/`, `center/`, `room/`).
- `src/components/`: reusable UI components.
- `src/layout/`: layout shells and navigation pieces.
- `src/api/`: API request modules grouped by domain.
- `src/stores/`: shared state modules.
- `src/styles/` and `src/assets/`: global styles and static assets.
- `public/`: static files served as-is.
- `dist/`: production build output.

Prefer keeping feature-related view logic, API calls, and local components close to each other in consistent folder names.

## Build, Test, and Development Commands
- `npm install`: install dependencies.
- `npm run dev`: start local dev server with `dev` mode.
- `npm run prod`: run Vite in `prod` mode for environment checks.
- `npm run build`: create production bundle in `dist/`.
- `npm run preview`: preview the built output locally.

Example flow: `npm run build && npm run preview`.

## Coding Style & Naming Conventions
- Use Prettier as the source of truth (`.prettier.json`).
- Formatting highlights: 2-space indentation, semicolons, single quotes, trailing commas.
- Vue SFCs should keep template/script/style readable and focused.
- Component files: `PascalCase.vue` (for example `PhoneBindModal.vue`).
- View directories and route modules: lowercase or domain-based naming (`views/system/user/`).
- API modules: descriptive camelCase filenames (`systemUser.js`, `systemMessage.js`).

## Testing Guidelines
There is currently no configured automated test runner (no Vitest/Jest/Cypress setup in this repo).

- For new features, include manual verification steps in the PR (route, role, and expected UI/API behavior).
- If you introduce tests, prefer Vue-friendly tooling (Vitest + Vue Test Utils) and colocate tests as `*.spec.js` next to source files.

## Commit & Pull Request Guidelines
Recent history follows Conventional Commit prefixes, mainly:
- `feat: ...`
- `chore: ...`

Keep commit messages imperative and scoped to one change. PRs should include:
- clear summary and affected modules (for example `src/views/system/room/`),
- linked issue/task ID if available,
- screenshots or short screen recordings for UI changes,
- notes on environment variables or API contract changes.
