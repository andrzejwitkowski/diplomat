# Agent guidance

Authoritative skills live in `.cursor/skills/`. Do not duplicate their full text here.

| When | Skill |
|------|--------|
| Writing / designing / fixing code | [ponytail](.cursor/skills/ponytail/SKILL.md) |
| Before done / commit / PR | [deslop](.cursor/skills/deslop/SKILL.md) (diff vs `main`) |
| Same finish gate | [thermo-nuclear-code-quality-review](.cursor/skills/thermo-nuclear-code-quality-review/SKILL.md) |

**Precedence:** ponytail decides whether to add a thing; thermo-nuclear restructures what is shipping; deslop strips AI noise.

Also enforced by [`.cursor/rules/agent-skills.mdc`](.cursor/rules/agent-skills.mdc) (`alwaysApply`).
