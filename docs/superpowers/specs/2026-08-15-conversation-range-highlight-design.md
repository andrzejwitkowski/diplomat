# Conversation range highlight

Date: 2026-08-15

## Problem

The user can mark conversation **start** and **end**, but messages between them do not look like part of the range. Start/end show a colored border; interior bubbles either never get `RangeRole.Interior` or the fill is indistinguishable from a normal bubble. There is no background behind the span.

Today `rangeRole` decides interior membership by comparing timestamps on a flattened list of all channels. Start/end still match by message id, so those roles appear while interior often does not.

## Goal

When a range is complete, it is obvious which messages are the conversation: a full-width row strip from start through end, and a distinct bubble fill on interior messages. Start and end keep their current border treatment.

## Membership

Compute role **inside one channel group** (`ChannelMessageGroup.messages`, already sorted by `timestamp` then `id`). Do not use the flattened cross-channel list.

Given `range` and `channelMessages`:

1. If `range` is null or `!range.isComplete`, every message is `None` except a message whose id is `startMessageId` (`Start`) or `endMessageId` (`End`). Incomplete range: no strip, no `Interior`.
2. If `message.sourceApp != range.sourceApp`, role is `None` (unless it is start/end, which cannot happen across channels).
3. Let `startIndex` / `endIndex` be indices of `range.startMessageId` / `range.endMessageId` in `channelMessages`. If either id is missing from this list, no `Interior` (start/end still match by id if those rows exist).
4. `lo = min(startIndex, endIndex)`, `hi = max(startIndex, endIndex)`.
5. For index `i`:
   - `message.id == startMessageId` → `Start`
   - `message.id == endMessageId` → `End`
   - `lo < i < hi` → `Interior`
   - otherwise → `None`

Index order matches what the user sees. Swapped timestamps (usecase already stores earlier id as start) still form a contiguous span between the two rows.

## Visual

Apply only when the range is complete, on rows whose role is `Start`, `Interior`, or `End`:

| Role | Row background (full width) | Bubble fill | Border / label |
|------|-----------------------------|-------------|----------------|
| Start | Same strip color | Unchanged (incoming `surfaceVariant` / outgoing `primaryContainer`) | 2.dp `primary`, START label |
| End | Same strip color | Unchanged | 2.dp `error`, END label |
| Interior | Same strip color | `tertiaryContainer` / `onTertiaryContainer` for both directions | None |
| None | None | Unchanged | None |

Row strip uses `secondaryContainer.copy(alpha = 0.45f)` on the full-width row `Box` (behind the bubble). Same color for Start, Interior, and End so consecutive rows read as one band. No strip when the range is incomplete (start-only still has the start border, no band). Channel headers stay unstyled.

Clicks, mark mode, and start/end overflow menus stay as they are.

## Files

No domain or usecase changes. Range storage remains a pair of message ids.

- `presentation/.../ConversationRangeRole.kt` — pure `rangeRole` (no Compose); used by the screen
- `presentation/.../ConversationMessageBubble.kt` — row strip; interior bubble colors
- `presentation/.../ConversationDetailScreen.kt` — pass `group.messages` into `rangeRole`, not `flatMessages`
- `presentation/build.gradle.kts` — `testImplementation` JUnit so `rangeRole` can be tested without Robolectric
- `presentation/src/test/.../ConversationRangeRoleTest.kt` — cases below

## Tests

`ConversationRangeRoleTest` (JUnit on the presentation module):

- Complete range, three same-channel messages: first `Start`, middle `Interior`, last `End`
- Same-channel messages outside `[lo, hi]`: `None`
- Message with a different `sourceApp` than `range.sourceApp`: `None` (never `Interior`)
- Incomplete range (`endMessageId` null): marked message is `Start`, neighbors `None`
- Start index > end index in the list: `Interior` is still the open interval between them

UI uses `range.isComplete` to paint the strip: Start/Interior/End rows get the strip only when both ends exist. Start-only keeps the start border and no band.

## Out of scope

- Persisting a set of message ids
- Drawing one overlay rect behind `LazyColumn`
- Changing mark-mode UX, channel grouping, or range apply/swap rules
- New user-facing strings beyond existing START/END labels

## Success

With start and end set on the same channel, every message visually between them has the strip; interior bubbles use tertiary fill; start/end keep primary/error borders; other channel and messages outside the span look unchanged.
