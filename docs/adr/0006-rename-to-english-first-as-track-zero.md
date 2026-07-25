# Rename German identifiers to English first, as Track Zero

The German-to-English rename described in `CONTEXT.md` happens **first**, before the persistence,
templating, presentation-extraction and frontend work — not incrementally as each file is touched.

Renames produce very large diffs. Mixed into a behavioural change they hide the real edit, make review
and `git bisect` unreliable, and turn every subsequent merge into a conflict. Done first and alone,
they are mechanical, verifiable by the compiler, and every later diff is legible. The alternative —
renaming opportunistically as files are touched — spreads the churn across every other track and
guarantees that the hardest files (`Fabrik`, `Werft`, `SchiffFlugService`) get a rename tangled into
their riskiest change.

## Consequences

Renames are performed with IDE rename refactoring, one concept per commit, with no behavioural change
in the same commit. Commit subjects should name the concept, not the file count.

**Scope is Java identifiers only.** Template variables are a separate, string-keyed namespace: 896
`setVar("...")` call sites feeding string keys such as `offizier.train.ability` into the templates.
`TemplateCompiler` performs no reflection, so Java renames cannot break templates — but nor do they
translate those keys. That vocabulary is deliberately left in German for now and translated during the
jte migration (ADR-0003), where the keys become typed template parameters and the compiler can check
the change. Attempting it in Track Zero would mean editing 896 call sites and 52 templates in lockstep
with no compiler assistance.

**Value is highest for code that survives.** Buildings, services, controllers and framework code are
long-lived and worth renaming. Entity classes are scheduled for deletion as they migrate to jOOQ
(ADR-0004), so renaming them buys less — prefer the survivors when sequencing the work within this
track.

The three convention violators named in `CONTEXT.md` — `Fabrik`, `Werft`, `Kommandozentrale` — become
`FactoryBuilding`, `ShipyardBuilding` and `CommandCenterBuilding` as part of this track.
