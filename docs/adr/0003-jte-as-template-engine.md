# jte as the template engine

The homegrown template engine (52 templates under `game/src/main/templates`, compiled to Java at
`generate-sources` by the 717-line `TemplateCompiler` in the `templates` module) is being retired. We
target **jte**, not Thymeleaf — despite 14 working Thymeleaf pages already existing under
`WEB-INF/templates`.

Two reasons. jte compiles templates to Java at build time with type-safe parameters, which is the same
shape as the existing `TemplateCompiler` step — so this is a swap within the current build pipeline
rather than a new paradigm bolted alongside it. And with 28 test files covering 689 sources, the
compiler is the safety net that actually exists: type-safe template parameters make the large
presentation refactor ahead (1022 inline-HTML call sites across 104 files, including domain classes
such as `Fabrik` and `SchiffFlugService`) checkable at build time. Thymeleaf is runtime-interpreted,
so the same refactors would only be verifiable by loading the page.

The 14 Thymeleaf pages are frozen — no new ones — and will be ported to jte last, after the 52
homegrown templates, since porting working pages yields nothing a player can see. End state is a
single engine.

## Consequences

Three engines coexist for most of the migration. This is accepted, with a stated end state, because
the alternative orderings either delay deleting `TemplateCompiler` or spend the first effort on churn.

jte templates are to be decomposed into **fragment** templates from the start, even where nothing
renders them individually yet. Fragment-level rendering is the one prerequisite for adopting htmx
later (to retire AngularJS 1.0.1, EOL since 2021, ~19k lines serving 2 templates). Porting the 52
templates as monolithic whole-page templates would mean splitting them again afterwards — porting
twice is the outcome worth avoiding, and decomposing up front costs nothing.
