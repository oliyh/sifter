# The Sifter — Lesson Plan

A step-by-step plan for building a Minecraft mod together, and learning to
program along the way. Written for a first-time programmer, working in
**Java**, using the **Fabric** mod loader.

## How this works

- We build the mod in **phases**. Each phase ends with something you can
  actually see or do in the game — that's how we know it worked.
- Before we use a new programming idea (like "if statements" or "loops"),
  we stop and learn it on its own first, with a tiny example that has
  nothing to do with Minecraft. Then we immediately use it in the mod, so
  it's obvious why it's useful.
- Every phase has parts marked **You type this** — those are yours. Parts
  marked **We read this together** are where I'll explain what existing
  code does before we change it.
- Each phase is broken into short sessions (20-40 minutes). Don't feel you
  need to finish a whole phase in one sitting.
- **Testing is part of the cycle, not a separate topic.** From the
  Interlude onwards, most phases ask "is there a bit of this we can pull
  out and check automatically, instead of only checking by playing?" Not
  everything can be (see Phase 1 and Phase 3) — and that's an honest,
  useful thing to notice, not a gap to feel bad about.
- There's a running **glossary** at the bottom of this file. Add to it as
  you meet new words — it's your reference.

---

## Phase 0 — Hello, Sifter (done — see `src/main/java/com/sifter/Sifter.java`)

**Goal:** load the mod in real Minecraft and see proof it ran.

**Concepts introduced:**
- A **program** is just a list of instructions the computer follows, in
  order, top to bottom.
- A **class** is a container for code, roughly "a thing, and what it can
  do." `Sifter.java` is a class.
- A **method** (a.k.a. **function**) is a named chunk of instructions you
  can run. `onInitialize()` is a method — Minecraft calls it once, the
  moment our mod loads.
- A **comment** (`//` in Java) is a note for humans that the computer
  ignores. Good for explaining *why*, not *what*.
- **Logging** — `LOGGER.info(...)` — printing a line of text somewhere you
  can read it later. It's the simplest way to ask a program "did you run?"

**What we built:** `Sifter implements ModInitializer` with one method,
`onInitialize()`, that logs `"Sifter mod loaded!"`.

**Checkpoint:** build the mod (`SETUP.md`), put it in Minecraft's mods
folder, launch the game, and check:
1. The **Mods** button on the main menu lists "Sifter".
2. `.minecraft/logs/latest.log` contains the line `Sifter mod loaded!`.

**Stretch idea (optional, do together):** change the message and rebuild,
to prove *you* control what it says.

---

## Phase 0.5 — Say hello in chat

**Goal:** when you join your world, the Sifter mod says hello *in the game
chat* — no log-file digging required.

**Concepts introduced:**
- A **parameter** — information passed into a method. (`onInitialize`
  takes none; the method we add here takes a "who just joined" parameter.)
- An **event / callback** — "run this code automatically whenever X
  happens," instead of us calling it ourselves. Like setting an alarm: you
  don't personally check the clock every second, you register what should
  happen when it goes off.
- A **string** — text in quotes, like `"Hello!"`.

**What we'll build:** register a callback on the "a player joined" event
that sends a chat message to that player.

**Checkpoint:** join your world, see the Sifter's message appear in chat.

---

## Interlude — Writing tests (done — see `src/.../practice/MathWarmup*.java`)

**Goal:** understand what a test is and get comfortable running one,
*before* we depend on tests for real mod logic.

So far, the only way we've checked our code works is by building it,
copying it to Windows, launching Minecraft, and looking. That works, but
it's slow, and for some kinds of mistakes (bad maths, wrong logic) it's
overkill — we shouldn't need to start a whole game just to check that
`2 + 3` comes out right.

**Concepts introduced:**
- A **unit test** — a small piece of code whose only job is to run a bit
  of *our* code and check the result is what we expect. "Unit" because it
  checks one small piece in isolation.
- An **assertion** — a single check inside a test, e.g.
  `assertEquals(5, MathWarmup.add(2, 3))` means "I expect `add(2, 3)` to
  be `5` — shout if it isn't."
- **Red / green** — the two states a test can be in: **red** (failing —
  something's wrong, or you haven't written the code yet) and **green**
  (passing). Professional programmers often deliberately write a test
  *before* the code, watch it fail red (proving the test actually tests
  something), then write the code to turn it green.
- A **pure function** — code whose answer only depends on the values you
  give it, with no dependence on the game, the world, or anything else
  changing around it (`add(2, 3)` is always `5`, forever). Pure functions
  are what unit tests are good at checking. Most Minecraft mod code
  *isn't* pure (it pokes at the live world) — part of the skill we're
  building is noticing the small pure pieces hiding inside bigger,
  impure ones.

**What we built:** `MathWarmup.add(int, int)` — plain Java, nothing to do
with Minecraft — and `MathWarmupTest`, which checks `add(2, 3) == 5`.

**You type this:**
1. Run the test suite: `./gradlew test`. It should pass (green).
2. Break it on purpose — change the test's expected number to something
   wrong — and run `./gradlew test` again. Read the failure message.
   That's red.
3. Fix it back, run again, watch it go green.

**Checkpoint:** you've seen a test fail *and* pass, and you know the one
command (`./gradlew test`) that runs every test in the project.
`./gradlew build` runs the tests automatically too — from now on, if a
test breaks, the build fails, even if you forget to check by hand.

---

## Phase 1 — The Sifter exists (two yellow blocks)

**Goal:** a new mob called the Sifter spawns in the world. It can be
extremely simple-looking (a placeholder box or two) — the point is that
*it exists as a thing Minecraft knows about*, and it's yellow.

**Concepts introduced:**
- An **object** — a specific instance of a class. "Entity" is the class of
  *all* Minecraft creatures; "a Sifter" is one particular object made from
  our own SifterEntity class.
- **Inheritance** — our `SifterEntity` class *extends* (builds on top of)
  Minecraft's existing animal/mob classes, so we get walking, health, and
  gravity for free, and only write the *new* bits ourselves.
- A **constructor** — special code that runs when a new object is created,
  to set it up.
- A **registry** — Minecraft's "phone book" of every entity/item/block
  type that exists, keyed by name (like `"sifter:sifter"`). Before
  something can spawn, it has to be registered.
- A **constant** — a named value that never changes, written in
  ALL_CAPS by convention (e.g. the Sifter's registered ID).

**What we'll build:** `SifterEntity` class, register it, give it a
temporary yellow-colored model (a resized/recolored vanilla mob is fine
to start).

**Checkpoint:** spawn a Sifter with a command and see a yellow mob appear.

**Testing:** nothing to unit test here, and that's fine — this phase is
almost entirely "tell Minecraft's registry this thing exists," which
only means anything with a real running game behind it. We check it the
honest way: by playing. (Contrast this with the Interlude — a good
question to keep asking each phase is "pure logic, or wiring?")

---

## Phase 2 — The wavy dance

**Goal:** the Sifter moves in a distinctive wavy, dance-like way instead
of walking in a straight line like every other mob.

**Concepts introduced:**
- **The game loop / "tick"** — Minecraft calls our entity's `tick()`
  method roughly 20 times per second, forever, while it exists. This is
  our first real encounter with a program that *repeats* — a loop we don't
  even have to write ourselves, because the game engine is already doing
  it for us.
- **Variables** — a named box that holds a value that *can* change over
  time (unlike a constant). We'll keep a variable that tracks "how far
  through the dance are we."
- **If statements** — "do this, but only when some condition is true."
  E.g. *if* the Sifter is on the ground, wobble; otherwise don't.
- **Basic math on a wave** — using `Math.sin(...)` to turn "time passing"
  into a smooth up-and-down number, without needing to know trigonometry —
  just "this function gives you a wiggly number between -1 and 1."

**What we'll build:** override the Sifter's movement/tick behaviour so its
position or body rotation follows a sine wave over time. Crucially, we'll
write the actual wave maths as its own small pure method, e.g.
`SifterAnimation.wobbleOffset(int ticksAlive)`, and have `tick()` just
call it — rather than burying the maths inside `tick()` where it can't be
tested without a running game.

**Checkpoint:** watch a Sifter wobble/sway as it moves, distinct from
every other mob.

**Testing:** this is our first *real* unit test on mod code. Write
`SifterAnimationTest` and check things like: the offset at tick 0 is 0;
the offset never goes outside the range you designed (e.g. -1 to 1); the
wave repeats after however many ticks you chose for one cycle. None of
these need Minecraft running — that's the win.

---

## Phase 3 — Give it a real look

**Goal:** replace the placeholder box with an actual custom model and
texture — the "real" yellow Sifter design.

**Concepts introduced:**
- **Client vs. server code** — Minecraft mods split into code that runs
  everywhere (server: rules, health, AI) and code that only runs on the
  screen doing the drawing (client: models, textures, animations). This
  is why our project already has a `src/client` folder.
  This is a good point to introduce the idea of **separation of
  concerns** — keeping "what happens" separate from "how it looks."
- **Coordinates in 3D** — model parts are built from boxes positioned with
  x/y/z numbers, an early hands-on use of coordinate geometry.
- **Parameters with defaults / overloading** (as needed, kept light).

**What we'll build:** a `SifterModel` (client-only) and matching texture,
wired up through a `SifterRenderer`.

**Checkpoint:** the Sifter looks like an actual designed creature, not a
recolored box.

**Testing:** like Phase 1, this is mostly wiring plus something a test
can't really judge — "does it look right" is a job for your eyes, not an
assertion. (Real studios do have automated visual tests that compare
screenshots pixel-by-pixel, but that's well past what we need here.) We
go back to playing and looking for this checkpoint.

---

## Phase 4 — Mining skulk

**Goal:** the Sifter notices nearby skulk blocks and mines them.

**Concepts introduced:**
- **Loops we write ourselves** (`for` loops) — scanning every block
  position in a cube around the Sifter to find skulk. This is the natural
  moment to explicitly teach `for` loops, having already *felt* an
  implicit loop back in Phase 2.
- **Lists/arrays** — collecting "all the skulk blocks I found nearby"
  before deciding what to do about them.
- **Booleans** — a true/false value, e.g. `isNearSkulk`.
- **AI Goals** — Minecraft mobs pick their next action from a prioritised
  list of "goals" (wander, follow, attack, ...). We add a `MineSkulkGoal`
  the Sifter checks each tick: combines everything above — if statements,
  loops, and booleans — into one real behaviour.
- **State** — the idea that a mob needs to *remember* what it's currently
  doing (e.g. "mid-mining a block") between ticks, using a variable.

**What we'll build:** a goal that finds the nearest skulk block within
range and breaks it over time. Same trick as Phase 2: the "which block is
nearest" search is a pure method we can write and test on its own — e.g.
`SkulkFinder.nearest(BlockPos origin, List<BlockPos> candidates, int
range)` — fed with a plain list of made-up coordinates, no real world
needed. The `MineSkulkGoal` itself (which touches the live world) stays
untested by JUnit and gets checked by playing.

**Checkpoint:** place skulk near a Sifter and watch it get mined.

**Testing:** write `SkulkFinderTest` with a handful of made-up block
lists: nearest block picked correctly, blocks outside `range` ignored,
an empty list handled without crashing (this last one is a classic case
a test catches that manual play easily misses — you'd have to remember
to try it).

---

## Phase 5 — Trading with the player

**Goal:** right-click the Sifter to trade, like a villager.

**Concepts introduced:**
- **More complex objects** — a "trade offer" bundles several pieces of
  data together (what you give, what you get, how many times it can be
  used). This is a good moment to talk about **data modelling** — using
  code structures to represent a real-world idea.
- **Randomness** — picking which trades a Sifter offers, or how much it
  wants, using `Random`.
- **Return values** — methods that hand back an answer, not just perform
  an action (contrast with `onInitialize()`/`tick()` which return
  nothing).
- Tying it together: an **event** (player right-clicks) triggers a
  **method** that opens a trade **object built from a list**, guarded by
  an **if statement** (e.g. only trade if the player is holding the right
  item).

**What we'll build:** trade offers involving skulk-related or
sculk-themed items, opened on right-click. The choice of "which trade(s)
does this Sifter offer" is another pure-ish piece we can separate out
(e.g. `TradeOffers.choose(Random random, List<TradeOption> options)`) and
test with a fixed/seeded `Random` so the result is predictable.

**Checkpoint:** trade with a Sifter and receive an item back.

**Testing:** write a test that checks the trade-choosing logic never
returns an invalid offer (e.g. negative price), and — using a fixed
random seed — returns a specific, predictable offer, proving the
randomness itself is under control rather than mysterious. By this
phase, writing "is there a pure piece in here?" and a matching test
before wiring it into the entity should feel like a normal step, not an
extra chore.

---

## Where to go after Phase 5

Ideas to pick from once the core mod works, roughly in order of
difficulty: sounds, particle effects while dancing, a spawn egg, a
custom advancement, loot drops, a second growth stage/variant.

---

## Glossary (grows as we go)

| Term | Plain-English meaning |
|---|---|
| Program | A list of instructions a computer follows in order |
| Class | A blueprint for a "thing" — data plus what it can do |
| Object | One actual thing made from a class |
| Method / function | A named, reusable chunk of instructions |
| Parameter | Information handed into a method when it runs |
| Return value | Information a method hands back when it finishes |
| Variable | A named box holding a value that can change |
| Constant | A named value that never changes |
| Comment | A note in the code for humans; ignored by the computer |
| If statement | "Do this, only when a condition is true" |
| Loop | "Repeat this, either a set number of times or while true" |
| Boolean | A value that is only ever true or false |
| List / array | An ordered collection of values |
| Event / callback | "Run this automatically when something happens" |
| Registry | Minecraft's lookup table of every known type of thing |
| Inheritance | A class building on top of another class's behaviour |
| Constructor | Code that runs to set up a brand-new object |
| Tick | One step of the game's clock (~20 per second) |
| Client vs. server | Drawing/screen code vs. rules/logic code |
| Unit test | Small code that runs a bit of our code and checks the result |
| Assertion | One check inside a test, e.g. "I expect this to equal that" |
| Red / green | Failing / passing — the two states a test can be in |
| Pure function | Code whose answer depends only on its inputs, nothing else |
