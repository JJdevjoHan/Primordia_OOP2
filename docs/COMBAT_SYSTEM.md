# Primordia — Combat System Reference
> Last updated: May 2026 · Branch: `feature/finalTweaks`

---

## 1. Character Archetypes & Stats

Characters are **not** identical — each has a unique archetype role.

| Character | HP | Archetype | Playstyle |
|---|---|---|---|
| Caedric Thunderbound | 230 | Thunder | High burst, chain pressure |
| Gwyneth Verdantide | 230 | Nature | Balanced, sustain |
| Myrrik Flameheart | 230 | Fire | Burn DoT, aggression |
| Faecyrion Vhaloris | 230 | Light | Shield/defense |
| Earl Froggington | 265 | Nature | Attrition/poison specialist |
| Ondine | 230 | Water | Heal/sustain |
| Ali Morningstart | 230 | Steel | Balanced warrior |
| Yuri Moonshade | 230 | Dark | Dark magic burst |

---

## 2. Elemental Type Chart

| Type | Strong vs | Weak vs |
|---|---|---|
| Fire | Nature, Steel | Water, Wind |
| Water | Fire, Nature | Steel, Thunder |
| Nature | Water, Wind | Fire, Steel |
| Steel | Nature, Wind | Fire, Water |
| Thunder | Water, Steel | Nature, Wind |
| Wind | Fire, Thunder | Nature, Steel |
| Light | Dark | Thunder, Dark |
| Dark | Light | Fire, Light |

**Advantage multiplier:** ×1.24 damage when attacking a weak enemy.

---

## 3. Damage Formula (`CombatBalance.java`)

```
base       = 20.0 + power × 0.62 + skillSlot × 2.5
execMult   = 0.92 + timingRatio × 0.18      (timingRatio ∈ [0, 1])
setupMult  = 1.08  (if setup skill was used this turn, else 1.0)
elemMult   = 1.24  (if defender is weak to attacker's type, else 1.0)

damage = round(base × execMult × setupMult × elemMult)
         clamped to [20, 70]
```

**Preview values** shown in character selection use `timingRatio = 0.5` (median).

---

## 4. Poison System

```
poisonDamage = clamp(round(5.0 + rawPoison × 0.85), 7, 18)  per tick
poisonDuration = clamp(durationTurns, 0, 3)  turns

Stacking: additive (damage sums, duration takes max)
Stack cap: 28 damage/tick
```

- Poison ticks at the **start of the victim's turn**
- Visual tint color: green (Earl), red (Myrrik burn), blue (Caedric shock)
- `poisonTickedThisTurn` flag prevents double-ticking from projectile animations

---

## 5. Mana Economy

| Parameter | Value |
|---|---|
| Max MP | 240 |
| MP regen per turn | 25 |
| Skill 1 cost | ~45–55 MP |
| Skill 2 cost | ~70–85 MP |
| Skill 3 cost | ~90–105 MP |

High costs force 3–4 turn cycles between powerful skills, rewarding resource management.

---

## 6. Skill Preview (Character Selection)

Each skill card shows:
- **`MP {cost}`** badge in the name header
- **`DMG {neutral}`** — base damage at median timing, no type advantage
- **`ADV {value}`** — damage × 1.24 when enemy is weak to your type
- **`PSN {dmg}x{dur}`** — poison ticks × duration
- **`HEAL +{value}`** — healing amount
- **`SHLD +{value}`** — shield amount
- **`REGEN +{value}`** — self-heal amount

---

## 7. Archive

Outdated balance documents from earlier iterations are in `docs/archive/`.
They describe a previous "perfectly balanced identical stats" design that was replaced.
