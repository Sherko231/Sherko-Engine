from pathlib import Path

path = Path("docs/DEVELOPMENT_STATUS.md")
text = path.read_text(encoding="utf-8")
old = "Authoring verification: temporary branch preflight run #2 / `37013532281` passed module-local lock generation guard, Spotless, `engine-world` tests, and the architecture boundary test on the formatter-applied candidate lineage. Final full branch verification and the five-job exact-head PR matrix remain pending."
new = "Authoring verification: temporary branch preflight run #5 / `37015889902` passed the guarded documentation reconciliation, Spotless, `engine-world` tests, the architecture boundary test, root `check`, and read-only dependency-lock resolution on the candidate lineage. Temporary authoring tooling remains branch-only and must be removed before the final PR; the five-job exact-head PR matrix remains pending."
if text.count(old) != 1:
    raise RuntimeError("Expected exactly one P7-T07 authoring-verification marker")
path.write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
