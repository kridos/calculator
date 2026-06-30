# Calculator

A modern **scientific and graphing calculator** for Android, built entirely with **Jetpack Compose**.

---

## Features

### Calculator Mode
- **Full expression entry** — type a complete equation before evaluating, just like a real scientific calculator
- **Scientific functions** — sin, cos, tan, √, ln, log, x², xʸ, 1/x, |x|, n!, eˣ, 10ˣ, mod
- **Constants** — π and e
- **DEG / RAD toggle** — switch between degrees and radians for trig functions
- **Parentheses** — full bracket support for complex expressions
- **AC / CE / ⌫** — clear all, clear entry, or delete last character
- **±** — toggle sign on the current expression
- **History** — last 3 calculations shown above the display, tappable to restore
- **Persistent history** — history survives app restarts via SharedPreferences
- **Animated advanced panel** — scientific keys slide in/out so the basic keypad stays clean

### Graphing Mode
- **y = f(x) graphing** — type any expression using `x` as the variable
- **Custom x and y ranges** — manually set xMin, xMax, yMin, yMax
- **Purple curve** on a dark canvas with axis lines and tick labels
- **Discontinuity handling** — asymptotes and undefined regions break the curve cleanly
- **Plot button** — graph only updates when you explicitly press Plot
- **Reset** — restores `sin(x)` with sensible default ranges
- **Always evaluates in radians** for predictable trig results

### UI
- Dark theme throughout
- Smooth animated transitions between simple and advanced modes
- Switch between Calculator and Graph with a single button tap
- Compact history card at the top of the calculator

---

## Supported Functions

| Button | Meaning |
|--------|---------|
| `sin` `cos` `tan` | Trig (DEG or RAD) |
| `√` | Square root |
| `ln` | Natural logarithm |
| `log` | Base-10 logarithm |
| `x²` | Square |
| `xʸ` | Power |
| `1/x` | Reciprocal |
| `\|x\|` | Absolute value |
| `n!` | Factorial |
| `eˣ` | e to the power of x |
| `10ˣ` | 10 to the power of x |
| `mod` | Modulo |
| `π` `e` | Constants |

---

## Graph Examples

```
sin(x)
cos(x) + sin(x)
x^2 - 4
x^3 - 2*x + 1
1/x
tan(x)
sqrt(x)
ln(x)
```

---

## Tech Stack

- **Language** — Kotlin
- **UI** — Jetpack Compose (Material 3)
- **Minimum SDK** — 26
- **Target SDK** — 35
- **Storage** — SharedPreferences (history persistence)
- **Math** — custom recursive-descent expression parser

---

## Building

1. Clone the repository
2. Open in Android Studio (Hedgehog or later)
3. Run on a device or emulator with API 26+

```bash
git clone https://github.com/krishna-exe/calculator.git
cd calculator
./gradlew assembleDebug
```

---

## Architecture

All logic lives in a single `MainActivity.kt` for simplicity:

- **Expression parser** — recursive-descent parser supporting operators, functions, constants, and the `x` variable for graphing
- **State model** — Compose `remember` state for display, history, graph ranges, and mode
- **Graph canvas** — Compose `Canvas` with manual coordinate mapping, grid lines, axis labels, and discontinuity detection

---

## License

MIT
