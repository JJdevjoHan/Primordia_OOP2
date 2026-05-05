# Character Configuration & Animation Comparison
## GamePanel vs SurvivalGamePanel vs ArcadeGamePanel

---

## 1. WIND WIZARD SKILL CONFIGURATION DIFFERENCES

### 1.1 Wind Wizard Skill 2 Offset (Enemy P2 Left Nudge)

**Purpose:** Adjusts horizontal positioning of Wind Wizard's Skill 2 animation for the enemy (P2) character.

| Panel | Constant Name | Value | File Reference |
|-------|---------------|-------|-----------------|
| **GamePanel** | `WIND_SKILL2_P2_LEFT_NUDGE_X` | 400 | [line 38](GamePanel.java#L38) |
| **SurvivalGamePanel** | `WIND_SKILL2_P2_LEFT_NUDGE_X` | 60 | [line 39](SurvivalGamePanel.java#L39) |
| **ArcadeGamePanel** | NOT DEFINED | — | — |

**Impact:** 
- GamePanel moves Skill 2 animation 400 pixels to the left (aggressive offset)
- SurvivalGamePanel moves only 60 pixels to the left (subtle offset)
- ArcadeGamePanel doesn't use this offset at all (skill animation renders at default position)

**Where Used:**
- GamePanel: [line 646](GamePanel.java#L646) - `enemySkillDrawX -= WIND_SKILL2_P2_LEFT_NUDGE_X;`
- SurvivalGamePanel: [line 680](SurvivalGamePanel.java#L680) - Same usage
- ArcadeGamePanel: Not used

---

### 1.2 Wind Wizard Skill 3 Scale Factor

**Purpose:** Scales the overlay animation for Wind Wizard's Skill 3 (area effect attack).

| Panel | Constant Name | Value | File Reference |
|-------|---------------|-------|-----------------|
| **GamePanel** | `WIND_SKILL3_SCALE` | 2.2 | [line 41](GamePanel.java#L41) |
| **SurvivalGamePanel** | `WIND_SKILL3_SCALE` | 1.18 | [line 41](SurvivalGamePanel.java#L41) |
| **ArcadeGamePanel** | NOT DEFINED | — | — |

**Impact:**
- GamePanel: Skill 3 overlay is 2.2x the character's draw size (large, dramatic effect)
- SurvivalGamePanel: Skill 3 overlay is 1.18x the character's draw size (smaller, contained effect)
- ArcadeGamePanel: Uses different rendering method entirely (see Section 2)

**Where Used:**
- GamePanel: [line 1580](GamePanel.java#L1580) - Calculation: `scale = ... * WIND_SKILL3_SCALE`
- SurvivalGamePanel: [line 1710](SurvivalGamePanel.java#L1710) - Same calculation

---

### 1.3 Wind Wizard Skill 3 Feet Offset (Directional Adjustment)

**Purpose:** Applies directional horizontal offset to Skill 3 overlay positioning based on character facing direction.

| Panel | Constant Name | Value | File Reference |
|-------|---------------|-------|-----------------|
| **GamePanel** | `WIND_SKILL3_FEET_OFFSET_X` | 80 | [line 39](GamePanel.java#L39) |
| **SurvivalGamePanel** | NOT DEFINED | — | — |
| **ArcadeGamePanel** | NOT DEFINED | — | — |

**Impact:**
- GamePanel: Skill 3 overlay is offset 80 pixels horizontally based on mirror direction
  - For mirrored (P2): offset +80 pixels
  - For non-mirrored (P1): offset -80 pixels
- SurvivalGamePanel: No directional offset applied (centered on feet)
- ArcadeGamePanel: Uses completely different positioning method (see Section 2)

**Where Used:**
- GamePanel: [line 1583](GamePanel.java#L1583) - `int directionalOffsetX = mirror ? WIND_SKILL3_FEET_OFFSET_X : -WIND_SKILL3_FEET_OFFSET_X;`
- SurvivalGamePanel: Not used (line 1714 doesn't include this offset)
- ArcadeGamePanel: Not used

---

## 2. SKILL 3 OVERLAY RENDERING METHOD DIFFERENCES

### 2.1 GamePanel & SurvivalGamePanel: Feet-Anchored Rendering

**Method Name:** `drawAnchoredSkillFrame()` 

**Location:**
- GamePanel: [lines 1565-1596](GamePanel.java#L1565-L1596)
- SurvivalGamePanel: [lines 1695-1724](SurvivalGamePanel.java#L1695-L1724)

**Rendering Approach:**
- Anchors overlay to **target's feet position** (foot anchor coordinates from map)
- Centers horizontally on target's feet
- Applies WIND_SKILL3_SCALE multiplier for sizing

**Key Difference Between GamePanel and SurvivalGamePanel:**

| Aspect | GamePanel | SurvivalGamePanel |
|--------|-----------|-------------------|
| Directional Offset | YES - Uses `directionalOffsetX` | NO - Direct centering |
| Formula | `x = targetFeetX - (drawWidth/2) + directionalOffsetX` | `x = targetFeetX - (drawWidth/2)` |
| Line Reference | [line 1583](GamePanel.java#L1583) | [line 1714](SurvivalGamePanel.java#L1714) |

**Why This Matters:**
- GamePanel's offset makes the overlay slightly asymmetrical based on character direction
- SurvivalGamePanel's direct centering creates perfectly centered overlays

---

### 2.2 ArcadeGamePanel: Sprite-Center Rendering

**Method Name:** `drawCenteredSkillFrame()`

**Location:** [lines 1511-1527](ArcadeGamePanel.java#L1511-L1527)

**Rendering Approach:**
- Anchors overlay to **sprite position** (character's top-left corner), not feet
- Centers within sprite bounding box using character's draw dimensions
- Does NOT use feet anchor coordinates
- Applies different positioning math: `x = spriteX + (spriteWidth - drawWidth) / 2`

**Key Features:**
```java
int x = spriteX + (spriteWidth - drawWidth) / 2;
int y = spriteY + (spriteHeight - drawHeight) / 2;
```
vs GamePanel/SurvivalGamePanel:
```java
int x = targetFeetX - (drawWidth / 2) + directionalOffsetX;  // Uses feet anchor + scale
int y = targetFeetY - drawHeight + WIND_SKILL3_FEET_OFFSET_Y; // From feet, not sprite top
```

**Why This Matters:**
- ArcadeGamePanel's positioning is **relative to character sprite bounds**, not feet
- GamePanel/SurvivalGamePanel's positioning is **absolute on map** (via feet anchor)
- Different visual positioning for the same skill animation across game modes

---

## 3. CHARACTER INITIALIZATION DIFFERENCES IN setCharacters()

### 3.1 Skill Button Refresh Method

**Location:** Inside `setCharacters()` method

| Panel | Method Called | Line Reference | Wrapper? |
|-------|---------------|---|----------|
| **GamePanel** | `refreshSkillButtonLabels()` | [line 726](GamePanel.java#L726) | YES - calls `refreshSkillButtons()` |
| **SurvivalGamePanel** | `refreshSkillButtons()` | [line 760](SurvivalGamePanel.java#L760) | NO - direct call |
| **ArcadeGamePanel** | `refreshSkillButtonLabels()` | [line 675](ArcadeGamePanel.java#L675) | YES - calls `refreshSkillButtons()` |

**Impact:**
- Minor difference in code organization, no functional difference
- Both `refreshSkillButtonLabels()` and `refreshSkillButtons()` do the same thing
- SurvivalGamePanel uses the direct method name

---

## 4. CHARACTER-SPECIFIC GAME MODE FEATURES

### 4.1 Enemy Swapping (Survival Mode Only)

**SurvivalGamePanel Exclusive Methods:**
- `setEnemyCharacter(int enemyIdx)` - [line 798](SurvivalGamePanel.java#L798)
- `pickNewSurvivalEnemy()` - [line 830](SurvivalGamePanel.java#L830)

**Purpose:** Allows mid-game enemy swapping in survival mode after defeating an enemy.

**Not Present In:** GamePanel, ArcadeGamePanel

**Impact:** Survival mode can dynamically replace the enemy; Versus and Arcade modes have fixed character matchups.

---

### 4.2 Game-Specific Constants

| Constant | SurvivalGamePanel | ArcadeGamePanel | GamePanel | Impact |
|----------|-------------------|-----------------|-----------|--------|
| `BOT_TURN_DELAY_MS` | 900 | 900 | NOT DEFINED | Bot AI turn timing in multiplayer modes |
| `SURVIVAL_ROUND_HEAL_AMOUNT` | 30 | NOT DEFINED | NOT DEFINED | Healing per round (Survival mode only) |
| `SURVIVAL_ROUND_HEAL_AMOUNT` location | [line 44](SurvivalGamePanel.java#L44) | — | — | — |

---

## 5. SKILL ANIMATION CONFIGURATION

### 5.1 Skill Frame Size Adjustments for Wind Wizard

**Location:** `getSkillFrameWidth()` and `getSkillFrameHeight()` methods

All three panels have identical logic:

| Character | Skill | Frame Width | Frame Height | File Reference |
|-----------|-------|-------------|--------------|---|
| Wind Wizard | 2 | 200 px | 128 px | GamePanel [1186-1195](GamePanel.java#L1186-L1195) |
| Wind Wizard | 3 | 288 px | 128 px | GamePanel [1186-1195](GamePanel.java#L1186-L1195) |
| Other | All | 128 px (DEFAULT_FRAME_SIZE) | 128 px | — |

**Impact:** Wind Wizard's Skill 2 and 3 use wider frame widths (200/288 px) compared to other skills (128 px) to accommodate the expanded sprite sheets.

**Same In All Three Panels:** ✓ No differences

---

## 6. CHARACTER DRAW WIDTH/HEIGHT USAGE

### 6.1 Dynamic Draw Dimension Methods

All three panels have identical implementations:

**Methods:** `getPlayerDrawWidth()`, `getPlayerDrawHeight()`, `getEnemyDrawWidth()`, `getEnemyDrawHeight()`

**Location:**
- GamePanel: [lines 1041-1044](GamePanel.java#L1041-L1044)
- SurvivalGamePanel: [lines 1171-1174](SurvivalGamePanel.java#L1171-L1174)
- ArcadeGamePanel: [lines 996-999](ArcadeGamePanel.java#L996-L999)

**Implementation:**
```java
private int getPlayerDrawWidth()  { return currentPlayerDef != null ? currentPlayerDef.drawWidth  : DEFAULT_DRAW_WIDTH;  }
private int getPlayerDrawHeight() { return currentPlayerDef != null ? currentPlayerDef.drawHeight : DEFAULT_DRAW_HEIGHT; }
private int getEnemyDrawWidth()   { return currentEnemyDef  != null ? currentEnemyDef.drawWidth   : DEFAULT_DRAW_WIDTH;  }
private int getEnemyDrawHeight()  { return currentEnemyDef  != null ? currentEnemyDef.drawHeight  : DEFAULT_DRAW_HEIGHT; }
```

**Impact:** Each character's draw dimensions come from CharacterDef JSON config, defaulting to 480x480 if not specified.

**Same In All Three Panels:** ✓ No differences

---

## 7. DEFAULT DIMENSIONS AND ANIMATION TIMING

All three panels use identical defaults:

| Constant | Value | Purpose |
|----------|-------|---------|
| `DEFAULT_DRAW_WIDTH` | 480 | Default character render width |
| `DEFAULT_DRAW_HEIGHT` | 480 | Default character render height |
| `DEFAULT_FRAME_SIZE` | 128 | Default sprite frame size (usually for idle/hurt/dead) |
| `DEFAULT_IDLE_DELAY_MS` | 120 | Idle animation frame delay |
| `DEFAULT_SKILL_DELAY_MS` | 90 | Skill animation frame delay |
| `DEFAULT_HURT_DELAY_MS` | 90 | Hurt animation frame delay |
| `DEFAULT_DEAD_DELAY_MS` | 150 | Dead animation frame delay |
| `POST_ATTACK_HURT_MS` | 600 | Delay before opponent's hurt animation starts |

**Same In All Three Panels:** ✓ No differences

---

## 8. SUMMARY TABLE: KEY DIFFERENCES

| Feature | GamePanel | SurvivalGamePanel | ArcadeGamePanel |
|---------|-----------|-------------------|-----------------|
| **Wind Skill 2 P2 Offset** | 400 px | 60 px | None |
| **Wind Skill 3 Scale** | 2.2x | 1.18x | N/A (different method) |
| **Wind Skill 3 Feet Offset** | ±80 px directional | None (centered) | N/A (different method) |
| **Skill 3 Rendering Method** | Feet-anchored + offset | Feet-anchored (no offset) | Sprite-centered |
| **Skill 3 Render Function** | `drawAnchoredSkillFrame()` | `drawAnchoredSkillFrame()` | `drawCenteredSkillFrame()` |
| **Enemy Swapping** | ✗ | ✓ | ✗ |
| **Survival Score** | ✗ | ✓ | ✗ |
| **Bot Turn Delay** | ✗ | 900 ms | 900 ms |
| **Button Refresh Method** | Wrapper | Direct | Wrapper |

---

## 9. WHY THESE DIFFERENCES MATTER FOR RENDERING & ANIMATION

### 9.1 Visual Impact

1. **Wind Skill 2 Positioning Difference (400 vs 60 px offset):**
   - Creates different visual "impact zones" for the same attack
   - GamePanel: More aggressive, hits further to the left
   - SurvivalGamePanel: More centered, less extreme positioning

2. **Wind Skill 3 Scale Difference (2.2x vs 1.18x):**
   - Affects perceived skill power and visual intensity
   - GamePanel: 2.2x scale = skill dominates screen (35% larger effect)
   - SurvivalGamePanel: 1.18x scale = more subtle effect

3. **Wind Skill 3 Offset Presence/Absence:**
   - GamePanel adds asymmetry based on character direction (±80 px)
   - SurvivalGamePanel keeps perfect symmetry
   - Creates different visual balance perception

4. **Skill 3 Rendering Method Difference:**
   - ArcadeGamePanel's sprite-centered approach may position overlay differently than feet-anchored
   - Could appear higher/lower or more/less centered depending on character sprite height

### 9.2 Gameplay Balance Implications

- **SurvivalGamePanel's smaller Wind Skill 2 offset (60 px):** Less punishment for aggressive positioning
- **SurvivalGamePanel's smaller Wind Skill 3 scale (1.18x):** Less dominant visual effect, may feel less powerful
- **ArcadeGamePanel's different overlay rendering:** Could affect hit registration perception or attack feeling

### 9.3 Animation Configuration Consistency

- **Skill frame sizes:** Same across all panels (Wind Wizard gets special 200/288 px widths)
- **Animation timings:** Same across all panels (90-150 ms delays)
- **Character draw dimensions:** Same system across all panels (using CharacterDef values)

---

## 10. RECOMMENDATIONS FOR CONSISTENCY

If standardizing across all three game modes:

1. **Decide on Wind Skill 2 offset:** Choose 400, 60, or 0
2. **Decide on Wind Skill 3 scale:** Choose 2.2, 1.18, or a middle value
3. **Decide on Skill 3 rendering method:**
   - Option A: Use feet-anchored with offset (GamePanel style)
   - Option B: Use feet-anchored centered (SurvivalGamePanel style)
   - Option C: Use sprite-centered (ArcadeGamePanel style)
4. **Consider if differences are intentional:** Each mode may have different design goals
   - Versus: Competitive fairness?
   - Survival: Different difficulty scaling?
   - Arcade: Unique visual identity?
