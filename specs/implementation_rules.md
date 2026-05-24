# Implementation Rules

Approved source of truth:
- docs/PRD.md
- docs/functional_spec.md
- docs/architecture.md
- docs/technical_decisions.md
- docs/data_contracts.md
- docs/screen_specs.md
- docs/implementation_plan.md
- docs/risk_register.md

Rules:
- Treat approved docs as contractual source of truth
- Do not invent requirements
- Do not weaken constraints
- Validate SDK/API correctness before coding
- No fake APIs
- Production-grade code only
- No pseudocode
- No TODO placeholders
- No stubs
- Include tests
- Keep architecture consistent
- Stop on ambiguity
- Show implementation plan before coding
- Self-review after coding:
  - compile correctness
  - architecture compliance
  - dependency correctness
  - spec compliance
  - test coverage

Implementation strategy:
- exactly one module at a time
- analyze architecture impact
- show plan
- list files
- implement
- test
- self-review
- stop