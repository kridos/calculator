package com.example.calculator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.ui.theme.CalculatorTheme
import java.util.Locale
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan


private val BgTop = Color(0xFF15151D)
private val BgMiddle = Color(0xFF0B0D12)
private val BgBottom = Color(0xFF050608)
private val GlowPurple = Color(0xFF7C3AED)
private val GlowBlue = Color(0xFF22D3EE)
private val GlowOrange = Color(0xFFFF9F0A)
private val ShellColor = Color(0xFF11131A)
private val ShellStroke = Color(0x26FFFFFF)
private val DisplayColor = Color(0xFF171A22)
private val DisplayHighlightColor = Color(0xFF1B1F2B)
private val NumColor = Color(0xFF2B2E37)
private val NumStroke = Color(0x16FFFFFF)
private val FnColor = Color(0xFF4C5160)
private val SciColor = Color(0xFF344055)
private val OpColor = Color(0xFFFF9F0A)
private val OpGlow = Color(0x33FF9F0A)
private val ActiveOpTextColor = Color(0xFF101214)
private val BodyTextColor = Color.White
private val SecondaryTextColor = Color(0x99FFFFFF)

private data class Key(
    val label: String,
    val action: String,
    val bg: Color,
    val fg: Color = BodyTextColor,
    val kind: ButtonKind
)

private enum class ButtonKind {
    Number,
    Function,
    Scientific,
    Operator,
    Equals,
    Toggle
}

private enum class AppMode {
    Calculator,
    Graph
}

private data class GraphPoint(
    val x: Float,
    val y: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorTheme {
                CalculatorScreen()
            }
        }
    }
}

@Composable
fun CalculatorScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("calculator_prefs", Context.MODE_PRIVATE)
    val haptic = LocalHapticFeedback.current
    var display by remember { mutableStateOf("0") }
    var lastExpression by remember { mutableStateOf("") }
    var justEvaluated by remember { mutableStateOf(false) }
    var degreesMode by remember { mutableStateOf(true) }
    var showAdvanced by remember { mutableStateOf(false) }
    var appMode by remember { mutableStateOf(AppMode.Calculator) }
    var graphDraftExpression by remember { mutableStateOf("sin(x)") }
    var graphPlottedExpression by remember { mutableStateOf("sin(x)") }
    var graphXMin by remember { mutableStateOf(-6.28f) }
    var graphXMax by remember { mutableStateOf(6.28f) }
    var graphYMin by remember { mutableStateOf(-2f) }
    var graphYMax by remember { mutableStateOf(2f) }
    val history = remember { mutableStateListOf<String>() }
    val historyScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        val saved = prefs.getString("history", "").orEmpty()
        history.clear()
        if (saved.isNotBlank()) {
            saved.lineSequence()
                .filter { it.isNotBlank() }
                .take(3)
                .forEach { history.add(it) }
        }
    }

    fun fmt(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "Error"
        val asLong = v.toLong()
        return if (v == asLong.toDouble()) {
            asLong.toString()
        } else {
            String.format(Locale.US, "%.10g", v)
        }
    }

    fun expressionText(): String = if (display == "0") "" else display

    fun clearAll() {
        display = "0"
        lastExpression = ""
        justEvaluated = false
    }

    fun saveHistory() {
        prefs.edit().putString("history", history.joinToString("\n")).apply()
    }

    fun addHistoryEntry(expression: String, resultText: String) {
        if (expression.isBlank()) return
        history.add(0, "$expression = $resultText")
        while (history.size > 3) {
            history.removeAt(history.lastIndex)
        }
        saveHistory()
    }

    fun clearEntryText(expression: String): String {
        if (expression.isBlank()) return ""
        var text = expression
        while (text.isNotEmpty() && text.last().isWhitespace()) {
            text = text.dropLast(1)
        }
        if (text.isEmpty()) return ""

        return when {
            text.last().isDigit() || text.last() == '.' -> text.dropLastWhile { it.isDigit() || it == '.' }
            text.last().isLetter() -> text.dropLastWhile { it.isLetter() }
            text.last() == ')' -> {
                var depth = 0
                var index = text.lastIndex
                while (index >= 0) {
                    when (text[index]) {
                        ')' -> depth++
                        '(' -> {
                            depth--
                            if (depth == 0) {
                                var start = index
                                while (start > 0 && text[start - 1].isLetter()) start--
                                return text.substring(0, start)
                            }
                        }
                    }
                    index--
                }
                text.dropLast(1)
            }
            else -> text.dropLast(1)
        }
    }

    fun appendText(text: String, continueAfterEvaluation: Boolean = false) {
        val current = expressionText()
        display = when {
            display == "Error" -> text
            justEvaluated && continueAfterEvaluation && current.isNotBlank() -> current + text
            justEvaluated -> text
            current == "" -> text
            else -> current + text
        }
        lastExpression = ""
        justEvaluated = false
    }

    fun appendUnary(prefix: String) {
        appendText("$prefix(")
    }

    fun onDigit(d: String) {
        appendText(d)
    }

    fun onDot() {
        val current = expressionText()
        val lastToken = current.takeLastWhile { it.isDigit() || it == '.' }
        if (lastToken.contains('.')) return
        appendText(if (lastToken.isEmpty()) "0." else ".")
    }

    fun onClear() {
        clearAll()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun onClearEntry() {
        if (display == "Error" || justEvaluated) {
            display = "0"
            lastExpression = ""
            justEvaluated = false
            return
        }

        display = clearEntryText(expressionText()).ifBlank { "0" }
        lastExpression = ""
    }

    fun onBackspace() {
        if (display == "Error") {
            display = "0"
            lastExpression = ""
            justEvaluated = false
            return
        }

        display = when {
            justEvaluated -> display.dropLast(1).ifBlank { "0" }
            display.length <= 1 -> "0"
            display == "0" -> "0"
            else -> display.dropLast(1).ifBlank { "0" }
        }
        lastExpression = ""
        justEvaluated = false
    }

    fun onToggleSign() {
        val current = expressionText()
        display = when {
            current.startsWith("-") -> current.drop(1).ifBlank { "0" }
            current == "0" -> "-"
            else -> "-$current"
        }
        lastExpression = ""
    }

    fun onPercent() {
        val current = display.toDoubleOrNull() ?: return
        display = fmt(current / 100.0)
        lastExpression = ""
        justEvaluated = false
    }

    fun onConstant(value: Double) {
        appendText(fmt(value))
    }

    fun onUnary(action: String) {
        when (action) {
            "sqrt" -> appendUnary("sqrt")
            "ln" -> appendUnary("ln")
            "log" -> appendUnary("log")
            "sin" -> appendUnary("sin")
            "cos" -> appendUnary("cos")
            "tan" -> appendUnary("tan")
            "abs" -> appendUnary("abs")
            "reciprocal" -> appendText("1/(", continueAfterEvaluation = true)
            "square" -> appendText("^2", continueAfterEvaluation = true)
            "factorial" -> appendText("!")
            "exp" -> appendText("e^(")
            "tenpow" -> appendText("10^(")
            "mod" -> appendText(" mod ")
        }
    }

    fun balanceParentheses(expression: String): String {
        var open = 0
        expression.forEach { ch ->
            when (ch) {
                '(' -> open++
                ')' -> if (open > 0) open--
            }
        }
        return expression + ")".repeat(open)
    }

    fun onEquals() {
        val expression = balanceParentheses(expressionText())
        val result = evaluateExpression(expression, degreesMode)
        lastExpression = expression
        display = fmt(result)
        justEvaluated = true
        addHistoryEntry(expression, display)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }



    val angleLabel = if (degreesMode) "DEG" else "RAD"
    val topLineText = when {
        justEvaluated && lastExpression.isNotBlank() -> "$lastExpression ="
        else -> "Expression • $angleLabel"
    }

    val buttonRows = remember(showAdvanced, degreesMode) {
        val basicRows = listOf(
            listOf(
                Key("AC", "clear", FnColor, Color.Black, ButtonKind.Function),
                Key("CE", "clearEntry", FnColor, Color.Black, ButtonKind.Function),
                Key("⌫", "backspace", FnColor, Color.Black, ButtonKind.Function),
                Key("±", "sign", FnColor, Color.Black, ButtonKind.Function)
            ),
            listOf(
                Key("7", "7", NumColor, BodyTextColor, ButtonKind.Number),
                Key("8", "8", NumColor, BodyTextColor, ButtonKind.Number),
                Key("9", "9", NumColor, BodyTextColor, ButtonKind.Number),
                Key("÷", "/", OpColor, ActiveOpTextColor, ButtonKind.Operator)
            ),
            listOf(
                Key("4", "4", NumColor, BodyTextColor, ButtonKind.Number),
                Key("5", "5", NumColor, BodyTextColor, ButtonKind.Number),
                Key("6", "6", NumColor, BodyTextColor, ButtonKind.Number),
                Key("×", "*", OpColor, ActiveOpTextColor, ButtonKind.Operator)
            ),
            listOf(
                Key("1", "1", NumColor, BodyTextColor, ButtonKind.Number),
                Key("2", "2", NumColor, BodyTextColor, ButtonKind.Number),
                Key("3", "3", NumColor, BodyTextColor, ButtonKind.Number),
                Key("−", "-", OpColor, ActiveOpTextColor, ButtonKind.Operator)
            ),
            listOf(
                Key("0", "0", NumColor, BodyTextColor, ButtonKind.Number),
                Key(".", ".", NumColor, BodyTextColor, ButtonKind.Number),
                Key("=", "=", OpColor, ActiveOpTextColor, ButtonKind.Equals),
                Key("+", "+", OpColor, ActiveOpTextColor, ButtonKind.Operator)
            )
        )

        val advancedRows = listOf(
            listOf(
                Key(angleLabel, "angle", SciColor, BodyTextColor, ButtonKind.Toggle),
                Key("(", "(", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key(")", ")", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("xʸ", "^", OpColor, ActiveOpTextColor, ButtonKind.Operator)
            ),
            listOf(
                Key("e", "e", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("π", "pi", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("sin", "sin", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("cos", "cos", SciColor, BodyTextColor, ButtonKind.Scientific)
            ),
            listOf(
                Key("tan", "tan", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("√", "sqrt", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("ln", "ln", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("log", "log", SciColor, BodyTextColor, ButtonKind.Scientific)
            ),
            listOf(
                Key("x²", "square", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("1/x", "reciprocal", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("|x|", "abs", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("n!", "factorial", SciColor, BodyTextColor, ButtonKind.Scientific)
            ),
            listOf(
                Key("eˣ", "exp", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("10ˣ", "tenpow", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("mod", "mod", SciColor, BodyTextColor, ButtonKind.Scientific),
                Key("CE", "clearEntry", FnColor, GlowPurple, ButtonKind.Function)
            )
        )

        basicRows to advancedRows
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgTop, BgMiddle, BgBottom)
                )
            )
    ) {
        GlowOrb(
            modifier = Modifier.align(Alignment.TopStart).offset(x = (-80).dp, y = (-40).dp),
            baseColor = GlowPurple,
            alpha = 0.34f,
            size = 280.dp
        )
        GlowOrb(
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = 40.dp),
            baseColor = GlowBlue,
            alpha = 0.28f,
            size = 240.dp
        )
        GlowOrb(
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = 120.dp),
            baseColor = GlowOrange,
            alpha = 0.18f,
            size = 300.dp
        )

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .fillMaxSize(),
            shape = RoundedCornerShape(34.dp),
            color = ShellColor.copy(alpha = 0.94f),
            shadowElevation = 24.dp,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, ShellStroke)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(18.dp)
            ) {
                Text(
                    text = if (appMode == AppMode.Calculator) "Scientific Calculator" else "Graphing Calculator",
                    style = MaterialTheme.typography.labelLarge,
                    color = SecondaryTextColor,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF12151D),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .verticalScroll(historyScrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "History",
                            color = SecondaryTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        if (history.isEmpty()) {
                            Text(
                                text = "No calculations yet",
                                modifier = Modifier.fillMaxWidth(),
                                color = SecondaryTextColor,
                                fontSize = 13.sp,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            history.take(3).forEach { item ->
                                Text(
                                    text = item,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val restored = item.substringBefore(" = ")
                                            display = restored.ifBlank { "0" }
                                            lastExpression = ""
                                            justEvaluated = false
                                        },
                                    color = SecondaryTextColor,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = DisplayColor,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ShellStroke)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = topLineText,
                            modifier = Modifier.fillMaxWidth(),
                            color = SecondaryTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = display,
                            modifier = Modifier.fillMaxWidth(),
                            color = BodyTextColor,
                            fontSize = when {
                                display.length > 20 -> 24.sp
                                display.length > 16 -> 28.sp
                                display.length > 13 -> 34.sp
                                display.length > 10 -> 42.sp
                                display.length > 7 -> 56.sp
                                else -> 72.sp
                            },
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showAdvanced = !showAdvanced },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showAdvanced) SciColor else FnColor,
                            contentColor = BodyTextColor
                        )
                    ) {
                        Text(
                            text = if (showAdvanced) "Hide advanced" else "Show advanced",
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Button(
                        onClick = {
                            appMode = if (appMode == AppMode.Calculator) AppMode.Graph else AppMode.Calculator
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (appMode == AppMode.Graph) OpColor else SciColor,
                            contentColor = if (appMode == AppMode.Graph) ActiveOpTextColor else BodyTextColor
                        )
                    ) {
                        Text(
                            text = if (appMode == AppMode.Graph) "Calc" else "Graph",
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (appMode == AppMode.Calculator) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val gap = 12.dp
                        val btnSize = (maxWidth - (gap * 3)) / 4
                        val (basicRows, advancedRows) = buttonRows
                        val scrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .animateContentSize()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            basicRows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(gap)
                                ) {
                                    row.forEach { key ->
                                        CalcButton(
                                            label = key.label,
                                            size = btnSize,
                                            bgColor = key.bg,
                                            textColor = key.fg,
                                            kind = key.kind,
                                            borderColor = when (key.kind) {
                                                ButtonKind.Operator, ButtonKind.Equals -> OpGlow
                                                ButtonKind.Function -> NumStroke
                                                else -> NumStroke
                                            },
                                            onClick = {
                                                when (key.action) {
                                                    "clear" -> onClear()
                                                    "clearEntry" -> onClearEntry()
                                                    "backspace" -> onBackspace()
                                                    "sign" -> onToggleSign()
                                                    "percent" -> onPercent()
                                                    "+", "-", "*", "/", "^" -> appendText(key.action, continueAfterEvaluation = true)
                                                    "=" -> onEquals()
                                                    else -> onDigit(key.action)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = showAdvanced,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                    advancedRows.forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(gap)
                                        ) {
                                            row.forEach { key ->
                                                CalcButton(
                                                    label = key.label,
                                                    size = btnSize,
                                                    bgColor = key.bg,
                                                    textColor = key.fg,
                                                    kind = key.kind,
                                                    borderColor = when (key.kind) {
                                                        ButtonKind.Operator -> OpGlow
                                                        ButtonKind.Function -> NumStroke
                                                        ButtonKind.Toggle -> Color(0x2AFFFFFF)
                                                        else -> NumStroke
                                                    },
                                                    onClick = {
                                                        when (key.action) {
                                                            "angle" -> degreesMode = !degreesMode
                                                            "pi" -> onConstant(Math.PI)
                                                            "e" -> onConstant(E)
                                                            "(" -> appendText("(")
                                                            ")" -> appendText(")")
                                                            "sqrt", "square", "reciprocal", "ln", "log", "sin", "cos", "tan", "abs", "factorial", "exp", "tenpow", "mod" -> onUnary(key.action)
                                                            "+", "-", "*", "/", "^" -> appendText(key.action, continueAfterEvaluation = true)
                                                            else -> onDigit(key.action)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    GraphModeScreen(
                        expression = graphDraftExpression,
                        onExpressionChange = { graphDraftExpression = it },
                        plottedExpression = graphPlottedExpression,
                        onPlot = { plotted -> graphPlottedExpression = plotted },
                        xMin = graphXMin,
                        xMax = graphXMax,
                        yMin = graphYMin,
                        yMax = graphYMax,
                        onXMinChange = { graphXMin = it },
                        onXMaxChange = { graphXMax = it },
                        onYMinChange = { graphYMin = it },
                        onYMaxChange = { graphYMax = it },
                        degreesMode = degreesMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GlowOrb(
    modifier: Modifier = Modifier,
    baseColor: Color,
    alpha: Float,
    size: Dp
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(alpha)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.52f),
                        baseColor.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
    )
}

@Composable
private fun GraphModeScreen(
    expression: String,
    onExpressionChange: (String) -> Unit,
    plottedExpression: String,
    onPlot: (String) -> Unit,
    xMin: Float,
    xMax: Float,
    yMin: Float,
    yMax: Float,
    onXMinChange: (Float) -> Unit,
    onXMaxChange: (Float) -> Unit,
    onYMinChange: (Float) -> Unit,
    onYMaxChange: (Float) -> Unit,
    degreesMode: Boolean,
    modifier: Modifier = Modifier
) {
    fun parseRangeValue(text: String, fallback: Float): Float = text.toFloatOrNull() ?: fallback

    var xMinText by remember { mutableStateOf(xMin.toString()) }
    var xMaxText by remember { mutableStateOf(xMax.toString()) }
    var yMinText by remember { mutableStateOf(yMin.toString()) }
    var yMaxText by remember { mutableStateOf(yMax.toString()) }

    LaunchedEffect(xMin, xMax, yMin, yMax) {
        xMinText = xMin.toString()
        xMaxText = xMax.toString()
        yMinText = yMin.toString()
        yMaxText = yMax.toString()
    }

    val graphScrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(graphScrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = DisplayColor,
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, ShellStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Graph y = f(x)",
                    color = SecondaryTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                OutlinedTextField(
                    value = expression,
                    onValueChange = onExpressionChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("sin(x)", color = SecondaryTextColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BodyTextColor,
                        unfocusedTextColor = BodyTextColor,
                        cursorColor = GlowPurple,
                        focusedBorderColor = GlowPurple,
                        unfocusedBorderColor = GlowPurple.copy(alpha = 0.5f),
                        focusedLabelColor = GlowPurple,
                        unfocusedLabelColor = SecondaryTextColor,
                        focusedPlaceholderColor = SecondaryTextColor,
                        unfocusedPlaceholderColor = SecondaryTextColor
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CalcButton(
                        label = "Plot",
                        size = 72.dp,
                        bgColor = OpColor,
                        textColor = ActiveOpTextColor,
                        kind = ButtonKind.Equals,
                        borderColor = OpGlow,
                        shape = RoundedCornerShape(16.dp),
                        onClick = { onPlot(expression.trim().ifBlank { "sin(x)" }) }
                    )
                    CalcButton(
                        label = "Reset",
                        size = 72.dp,
                        bgColor = FnColor,
                        textColor = BodyTextColor,
                        kind = ButtonKind.Function,
                        borderColor = NumStroke,
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            onExpressionChange("sin(x)")
                            onPlot("sin(x)")
                            onXMinChange(-6.28f)
                            onXMaxChange(6.28f)
                            onYMinChange(-2f)
                            onYMaxChange(2f)
                        }
                    )
                }
                Text(
                    text = if (degreesMode) "Trig functions use DEG" else "Trig functions use RAD",
                    color = SecondaryTextColor,
                    fontSize = 12.sp
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF12151D),
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, ShellStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Ranges", color = SecondaryTextColor, fontSize = 12.sp)
                val rangeFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BodyTextColor,
                    unfocusedTextColor = BodyTextColor,
                    cursorColor = GlowPurple,
                    focusedBorderColor = GlowPurple,
                    unfocusedBorderColor = GlowPurple.copy(alpha = 0.5f),
                    focusedLabelColor = GlowPurple,
                    unfocusedLabelColor = SecondaryTextColor
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = xMinText,
                        onValueChange = {
                            xMinText = it
                            onXMinChange(parseRangeValue(it, xMin))
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("x min") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = rangeFieldColors
                    )
                    OutlinedTextField(
                        value = xMaxText,
                        onValueChange = {
                            xMaxText = it
                            onXMaxChange(parseRangeValue(it, xMax))
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("x max") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = rangeFieldColors
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = yMinText,
                        onValueChange = {
                            yMinText = it
                            onYMinChange(parseRangeValue(it, yMin))
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("y min") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = rangeFieldColors
                    )
                    OutlinedTextField(
                        value = yMaxText,
                        onValueChange = {
                            yMaxText = it
                            onYMaxChange(parseRangeValue(it, yMax))
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("y max") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = rangeFieldColors
                    )
                }
                GraphCanvas(
                    expression = plottedExpression.ifBlank { "sin(x)" },
                    degreesMode = degreesMode,
                    xMin = xMin,
                    xMax = xMax,
                    yMin = yMin,
                    yMax = yMax
                )
            }
        }
    }
}


@Composable
@OptIn(ExperimentalTextApi::class)
private fun GraphCanvas(
    expression: String,
    degreesMode: Boolean,
    xMin: Float,
    xMax: Float,
    yMin: Float,
    yMax: Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFF0F1117), RoundedCornerShape(18.dp))
    ) {
        val widthPx = size.width
        val heightPx = size.height
        val xSpan = (xMax - xMin).takeIf { kotlin.math.abs(it) > 0.0001f } ?: 0.0001f
        val ySpan = (yMax - yMin).takeIf { kotlin.math.abs(it) > 0.0001f } ?: 0.0001f
        val xScale = widthPx / xSpan
        val yScale = heightPx / ySpan
        val axisColor = Color(0x66FFFFFF)
        val gridColor = Color(0x18FFFFFF)
        val curveColor = Color(0xFF7C3AED)

        fun mapX(x: Float): Float = (x - xMin) * xScale
        fun mapY(y: Float): Float = heightPx - ((y - yMin) * yScale)

        drawRect(color = Color(0xFF0F1117))

        // Grid lines and tick labels
        val xTickCount = 6
        val yTickCount = 6
        val xStep = xSpan / xTickCount
        val yStep = ySpan / yTickCount

        for (i in 0..xTickCount) {
            val xValue = xMin + (i * xStep)
            val xPos = mapX(xValue)
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(xPos, 0f), androidx.compose.ui.geometry.Offset(xPos, heightPx))
            drawText(
                textMeasurer = textMeasurer,
                text = String.format(Locale.US, "%.1f", xValue),
                style = TextStyle(color = GlowPurple, fontSize = 11.sp),
                topLeft = androidx.compose.ui.geometry.Offset(xPos + 4f, heightPx - 14f)
            )
        }

        for (i in 0..yTickCount) {
            val yValue = yMin + (i * yStep)
            val yPos = mapY(yValue)
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, yPos), androidx.compose.ui.geometry.Offset(widthPx, yPos))
            drawText(
                textMeasurer = textMeasurer,
                text = String.format(Locale.US, "%.1f", yValue),
                style = TextStyle(color = GlowPurple, fontSize = 11.sp),
                topLeft = androidx.compose.ui.geometry.Offset(6f, yPos - 12f)
            )
        }

        if (xMin <= 0f && xMax >= 0f) {
            val zeroX = mapX(0f)
            drawLine(axisColor, androidx.compose.ui.geometry.Offset(zeroX, 0f), androidx.compose.ui.geometry.Offset(zeroX, heightPx), strokeWidth = 2f)
        }
        if (yMin <= 0f && yMax >= 0f) {
            val zeroY = mapY(0f)
            drawLine(axisColor, androidx.compose.ui.geometry.Offset(0f, zeroY), androidx.compose.ui.geometry.Offset(widthPx, zeroY), strokeWidth = 2f)
        }

        val samples = size.width.toInt().coerceAtLeast(200)
        var prev: androidx.compose.ui.geometry.Offset? = null
        for (i in 0..samples) {
            val t = i / samples.toFloat()
            val xVal = xMin + (xMax - xMin) * t
            val yVal = evaluateGraphExpression(expression, false, xVal.toDouble())
            val current = if (yVal.isFinite()) androidx.compose.ui.geometry.Offset(mapX(xVal), mapY(yVal.toFloat())) else null

            if (current == null || current.y.isNaN() || current.y.isInfinite()) {
                prev = null
                continue
            }

            if (prev != null) {
                val jump = kotlin.math.abs(current.y - prev!!.y)
                if (jump < heightPx * 0.45f) {
                    drawLine(curveColor, prev!!, current, strokeWidth = 4f)
                } else {
                    prev = current
                    continue
                }
            }
            prev = current
        }
    }
}


@Composable
private fun CalcButton(
    label: String,
    size: Dp,
    bgColor: Color,
    textColor: Color,
    kind: ButtonKind,
    borderColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = 900f),
        label = "buttonScale"
    )
    val buttonBg by animateColorAsState(
        targetValue = when {
            pressed && kind == ButtonKind.Equals -> Color(0xFFFFB340)
            pressed && kind == ButtonKind.Operator -> Color(0xFFFFB347)
            pressed -> bgColor.copy(alpha = 0.88f)
            else -> bgColor
        },
        animationSpec = tween(120),
        label = "buttonBg"
    )
    val buttonElevation by animateFloatAsState(
        targetValue = if (pressed) 3f else 12f,
        animationSpec = tween(120),
        label = "buttonElevation"
    )
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(
                when (kind) {
                    ButtonKind.Equals -> HapticFeedbackType.LongPress
                    ButtonKind.Function -> HapticFeedbackType.TextHandleMove
                    ButtonKind.Operator -> HapticFeedbackType.TextHandleMove
                    else -> HapticFeedbackType.TextHandleMove
                }
            )
            onClick()
        },
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
        color = buttonBg,
        contentColor = textColor,
        shadowElevation = buttonElevation.dp,
        tonalElevation = if (pressed) 1.dp else 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = when {
                    label.length >= 5 -> 14.sp
                    label.length >= 3 -> 18.sp
                    else -> 28.sp
                },
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun evaluateExpression(expression: String, degreesMode: Boolean): Double {
    return evaluateExpression(expression, degreesMode, null)
}

private fun evaluateGraphExpression(expression: String, degreesMode: Boolean, xValue: Double): Double {
    return evaluateExpression(expression, degreesMode, xValue)
}

private fun evaluateExpression(expression: String, degreesMode: Boolean, xValue: Double?): Double {
    class Parser(private val input: String, private val xValue: Double?) {
        private var pos = -1
        private var ch = '\u0000'

        init {
            nextChar()
        }

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            return if (ch == charToEat) {
                nextChar()
                true
            } else {
                false
            }
        }

        private fun eatWord(word: String): Boolean {
            while (ch == ' ') nextChar()
            val start = pos
            for (expected in word) {
                if (ch != expected) {
                    pos = start
                    ch = input.getOrElse(start) { '\u0000' }
                    return false
                }
                nextChar()
            }
            return true
        }

        fun parse(): Double {
            val x = parseExpression()
            if (pos < input.length) throw IllegalArgumentException("Unexpected: $ch")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                x = when {
                    eat('+') -> x + parseTerm()
                    eat('-') -> x - parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parsePower()
            while (true) {
                x = when {
                    eat('*') -> x * parsePower()
                    eat('/') -> {
                        val divisor = parsePower()
                        if (divisor == 0.0) throw IllegalArgumentException("Division by zero")
                        x / divisor
                    }
                    eatWord("mod") -> {
                        val divisor = parsePower()
                        if (divisor == 0.0) throw IllegalArgumentException("Division by zero")
                        x % divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parsePower(): Double {
            var x = parseUnary()
            if (eat('^')) {
                x = x.pow(parsePower())
            }
            return x
        }

        private fun parseUnary(): Double {
            return when {
                eat('+') -> parseUnary()
                eat('-') -> -parseUnary()
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Double {
            if (eat('(')) {
                val x = parseExpression()
                if (!eat(')')) throw IllegalArgumentException("Missing )")
                return applyPostfix(x)
            }

            if (ch.isLetter()) {
                val start = pos
                while (ch.isLetter()) nextChar()
                val name = input.substring(start, pos)
                val value = if (eat('(')) {
                    val arg = parseExpression()
                    if (!eat(')')) throw IllegalArgumentException("Missing )")
                    when (name.lowercase()) {
                        "exp" -> kotlin.math.exp(arg)
                        "tenpow" -> 10.0.pow(arg)
                        else -> applyFunction(name, arg, degreesMode)
                    }
                } else {
                    when (name.lowercase()) {
                        "pi" -> Math.PI
                        "e" -> E
                        "x" -> xValue ?: throw IllegalArgumentException("Unknown identifier: $name")
                        else -> throw IllegalArgumentException("Unknown identifier: $name")
                    }
                }
                return applyPostfix(value)
            }

            if (ch.isDigit() || ch == '.') {
                val start = pos
                while (ch.isDigit() || ch == '.') nextChar()
                return applyPostfix(input.substring(start, pos).toDouble())
            }

            throw IllegalArgumentException("Unexpected: $ch")
        }

        private fun applyPostfix(value: Double): Double {
            var result = value
            while (true) {
                result = when {
                    eat('!') -> factorial(result)
                    eat('%') -> result / 100.0
                    else -> return result
                }
            }
        }
    }

    return try {
        Parser(expression.replace("×", "*").replace("÷", "/").replace("−", "-"), xValue).parse()
    } catch (_: Exception) {
        Double.NaN
    }
}

private fun applyFunction(name: String, value: Double, degreesMode: Boolean): Double {
    val radians = if (degreesMode) Math.toRadians(value) else value
    return when (name.lowercase()) {
        "sqrt" -> if (value < 0.0) Double.NaN else sqrt(value)
        "ln" -> if (value <= 0.0) Double.NaN else ln(value)
        "log" -> if (value <= 0.0) Double.NaN else log10(value)
        "sin" -> sin(radians)
        "cos" -> cos(radians)
        "tan" -> tan(radians)
        "abs" -> kotlin.math.abs(value)
        else -> Double.NaN
    }
}

private fun factorial(value: Double): Double {
    if (value < 0.0 || value != value.toInt().toDouble()) return Double.NaN
    var result = 1.0
    var n = value.toInt()
    while (n > 1) {
        result *= n.toDouble()
        n--
    }
    return result
}
