# Sherko Engine — Development Status

> Commit-contained handoff checkpoint. This file describes the repository commit that contains it; GitHub Issues/Project is authoritative for workflow activity after that commit.

## Checkpoint identity

| Field | Value |
| --- | --- |
| Verified pre-checkpoint `master` | `ec8cad564467684762126925dba4bbb183c64b1a` — post-P2-T09 handoff maintenance PR #124 merged |
| Milestone / completed phase | M1 — Engine Foundation remains in progress through P1-P4; P1 is complete |
| Completed roadmap implementation | P1-T01 through P1-T10A, P2-T01 through P2-T09 |
| Current executable work | P2-T10 / Issue #80 — explicit native-resource registry |
| Current branch | `p2-t10-native-resource-registry` |
| Next planned roadmap implementation | P2-T11 / Issue #81 — allocation measurement; planning-only until P2-T10 completes |
| Independent feasibility follow-ups | P0-T09A / #42, P0-T13 / #43, P0-T14 / #44 |

The containing commit is the exact checkpoint. A Markdown file cannot embed the hash of the commit that creates itself; a fresh agent must inspect live branch/HEAD, remote `master`, Issues, PRs, and workflow state before continuing.

## P2-T09 completion evidence

P2-T09 / Issue #79 is complete:

- PR #122 merged to `master` as `b2f8d8971c81234568d7cbce180b8cc7b5e9d0fe`.
- Final exact-head PR workflow #186 / run `34596141502` passed all five required jobs on head `0142bb0a0d5af497878b6dc082dadbed646b304f`.
- PR `engine-subsystem-tests` artifact: ID `10262673006`, digest `sha256:dc1ba55b99e1bf9c0723230e9d00eab8c1426aa327b039df7ae83c80f82139e4`.
- PR `jacoco-reports` artifact: ID `10262088284`, digest `sha256:b5f33b7840bdf30b7446dcab29d724d781917a976df910e1f3d1bba476c7ccc1`.
- Merged-master workflow #187 / run `34596379138` passed all five required jobs on exact merge commit `b2f8d8971c81234568d7cbce180b8cc7b5e9d0fe`.
- Merged-master `engine-subsystem-tests` artifact: ID `10262423630`, digest `sha256:ed391cface3cbd9666a633e86fdb381fe03be7cc205206dd9fe0a6379f282ea9`.
- Merged-master `jacoco-reports` artifact: ID `10262088696`, digest `sha256:faa427e126647da4aa6510e8fee0602f08d8a834f8402259391d3e84b33400a0`.
- Independent review was recorded as `not performed` because no separate reviewer/agent identity was available; CI and self-review were not represented as independent review.

P2-T09 established deterministic startup configuration layering through `EngineConfigLoader`: fixed precedence `EngineConfigSchema` defaults < game file < user file < command-line overrides, bounded UTF-8 `key=value` parsing with source attribution, validate-after-merge behavior, and no lifecycle side effects.

Documentation-only Issue #123 / PR #124 then reconciled the post-P2-T09 handoff and merged as `ec8cad564467684762126925dba4bbb183c64b1a`. Its complete diff contained only `README.md` and this status document, so the Markdown-only CI exemption applied and no build/test workflow was required.

## P2-T10 executable contract

Issue #80 is activated and is the sole executable roadmap task for this branch.

`NativeResourceRegistry` is being added to `engine-core` as a diagnostic ownership registry for native handles. Each successful registration stores a normalized resource type, opaque nonzero `long` handle, automatically captured allocation site, and caller-supplied closer. The returned `Registration` is the supported close capability.

Successful close invokes the closer once and unregisters the resource. A closer failure is terminal, propagates the original unchecked throwable, is not retried, and remains tracked as `CLOSE_FAILED` so failed cleanup cannot be mistaken for release. Duplicate live `(resourceType, handle)` identities are rejected; the same numeric handle may coexist under different types and a successfully released identity may later be reused.

`assertNoOpenResources()` is a non-cleaning debug-shutdown check. It returns only when the registry is empty; otherwise it throws with deterministic registration-order diagnostics containing the tracked count, type, handle, state, and captured allocation site. It does not invoke closers or mutate the registry.

Calls remain externally serialized and closers run synchronously on the caller thread. P2-T10 adds no native binding, force-close-all behavior, thread-affinity dispatcher, actual OpenGL/Jolt/OpenAL/Steam wrapper, logging/fatal-shutdown integration, allocation metrics, or composition-root integration.

Synthetic registry tests do not establish native leak freedom or repeated native restartability. P0-T13 and P0-T14 remain independent evidence gates.

## Current repository state

- Java 25 Gradle multi-project foundation remains intact.
- The repository declares the locked 16 production-target modules plus experimental `feasibility-spikes`.
- Root remains a build/quality/task aggregator with no Java production source tree.
- `engine-core` contains lifecycle/order/startup rollback, monotonic elapsed sampling, exact 60 Hz accumulation, bounded catch-up, interpolation alpha, typed/layered startup configuration, and on this branch native-resource ownership diagnostics.
- P2-T10 adds no dependency, lockfile, module edge, native binding, protocol, persisted format, or product-scope change.
- CI retains five Windows x64 self-hosted jobs and the Markdown-only path exemption; P2-T10 is non-exempt because Java/YAML files change.

## Exact next action

On `p2-t10-native-resource-registry`:

1. finish only the eight paths authorized by Issue #80;
2. run/audit `NativeResourceRegistryTest` through exact-head CI together with the existing engine-core regression set;
3. audit the complete changed-file list against Issue #80;
4. self-review ownership identity, close success/failure, allocation-site capture, deterministic diagnostics, and non-cleaning verification;
5. record independent-review provenance honestly;
6. open one linked PR;
7. require passing exact-head PR CI on the current head SHA;
8. merge only after the current-head run passes;
9. require separate passing push CI on the exact resulting `master` merge commit;
10. record final artifacts/evidence and confirm Issue #80 closes consistently.

Do not activate or implement P2-T11 until P2-T10 is merged and verified.

## Phase 2 status

Completed and merged:

- P2-T01 / #64 — `EngineSubsystem` lifecycle.
- P2-T02 / #72 — `SubsystemGraph` dependency ordering.
- P2-T03 / #73 — coordinated partial-startup rollback.
- P2-T04 / #74 — monotonic `EngineClock` elapsed-nanosecond sampling.
- P2-T05 / #75 — exact fixed-step accumulator at 60 Hz.
- P2-T06 / #76 — bounded frame-gap and catch-up policy.
- P2-T07 / #77 — renderer-facing interpolation alpha separated from whole simulation ticks.
- P2-T08 / #78 — typed configuration keys/defaults/bounds/source-aware validation.
- P2-T09 / #79 — fixed-precedence layered configuration loading.

Active:

- P2-T10 / #80 — explicit native-resource registry and shutdown leak diagnostics.

Next after P2-T10:

- P2-T11 / #81 — allocation measurement; planning-only until explicitly activated.
- P2-T12 / #82 — structured runtime logging; planning-only.
- P2-T13 / #83 — orderly fatal termination; planning-only and depends on P2-T10/P2-T12.

P2 phase completion is not claimed. The exit gate remains a deterministic headless loop running fixed ticks for ten minutes with bounded catch-up and verified cleanup. Isolated P2 task tests do not satisfy that integrated gate.

## Open gates and blockers

| Gate | Blocks | Current evidence gap |
| --- | --- | --- |
| P0-T09A / #42 | Production Steam transport work in P10/P13 | Two-process connect/accept/callback/send/receive/message-release/close lifecycle through `ISteamNetworkingSockets`. |
| P0-T13 / #43 | Claims of sustained native stability | At least 15 minutes of combined execution with memory/handle/traffic metrics and JFR. |
| P0-T14 / #44 | Claims of repeatable native lifecycle safety | 100 supported initialize/use/shutdown cycles or explicit process-global limitations. |

These gates do not block P2-T10 implementation, but synthetic registry tests and ordinary CI must not strengthen their claims.

## Live-state reconciliation

Before implementation or handoff, a fresh agent must:

1. read `AGENTS.md` fully and follow its required order;
2. inspect local status/HEAD when a local checkout exists;
3. compare remote `master`, open PRs, active Issues, and workflow state with this checkpoint;
4. verify #80 remains the sole active executable roadmap task;
5. audit all changed paths against #80;
6. require exact-head PR CI and exact merged-master push CI because this task is non-Markdown;
7. keep P2-T11 and later tasks planning-only until activated one at a time;
8. preserve P2's still-unproven ten-minute integrated exit gate and independent native feasibility gates;
9. stop if code, docs, live GitHub state, or the active Issue conflict instead of guessing.
