package io.github.luogen520.miaosuan

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.luogen520.miaosuan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 当前输入的表达式，用展示符号（×÷−）直接存，求值交给 CalcEngine
    private val input = StringBuilder("0")
    private var isError = false
    private var evaluated = false          // 刚按过等号，输入的是结果
    private var lastOp: Char? = null       // 重复按等号时重放上一次运算
    private var lastOperand: String? = null
    private val history = ArrayList<HistoryEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 全面屏：内容避开状态栏和手势条
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        if (savedInstanceState != null) restoreState(savedInstanceState)
        bindKeys()
    }

    /** 每个按键按下都给一下轻触感，像老计算器的键程。 */
    private fun View.key(action: () -> Unit) {
        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            action()
        }
    }

    private fun bindKeys() {
        val digitKeys = mapOf<Button, String>(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9",
        )
        digitKeys.forEach { (btn, d) -> btn.key { appendDigit(d) } }
        binding.btnPlus.key { appendOperator("+") }
        binding.btnMinus.key { appendOperator("−") }
        binding.btnMultiply.key { appendOperator("×") }
        binding.btnDivide.key { appendOperator("÷") }
        binding.btnDot.key { appendDot() }
        binding.btnPercent.key { appendPercent() }
        binding.btnBackspace.key { backspace() }
        binding.btnClear.key { clearAll() }
        binding.btnEquals.key { equals() }
        binding.btnToggleSign.key { toggleSign() }

        // 长按退格一键清空，长按结果复制到剪贴板
        binding.btnBackspace.setOnLongClickListener { clearAll(); true }
        binding.txtMain.setOnLongClickListener { copyResult(); true }
        binding.btnHistory.key { openHistory() }
    }

    private fun openHistory() {
        HistorySheet(this, history, onPick = { e ->
            isError = false
            binding.txtMain.setTextColor(ContextCompat.getColor(this, R.color.ink))
            input.clear().append(e.result)
            binding.txtExpr.text = "${grouped(e.expression)} ="
            rememberLastOperation(e.expression)
            evaluated = true
            refreshDisplay(e.result)
        }, onClear = {
            history.clear()
        }).show()
    }

    private fun copyResult() {
        if (isError) return
        val text = input.toString()
        if (text == "0") return
        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("result", text))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun appendDigit(d: String) {
        if (isError) clearAll()
        if (evaluated) startNewNumber()
        // 百分号后面直接按数字，默认接着乘，比如 50%3 就是 50%×3
        if (input.isNotEmpty() && input.last() == '%') input.append('×')
        // 开头的 0 直接顶掉，避免出现 007
        if (currentNumber() == "0" && d != "0") input.deleteCharAt(input.length - 1)
        else if (currentNumber() == "0" && d == "0") return
        input.append(d)
        refreshDisplay()
    }

    private fun appendOperator(op: String) {
        if (isError) clearAll()
        evaluated = false
        // 连按运算符时把结尾的运算符换掉
        while (input.isNotEmpty() && isOperator(input.last())) input.deleteCharAt(input.length - 1)
        if (input.isEmpty()) {
            // 空表达式只有减号有意义
            if (op != "−") return
        }
        input.append(op)
        refreshDisplay()
    }

    private fun appendDot() {
        if (isError) clearAll()
        if (evaluated) startNewNumber()
        if (input.isNotEmpty() && input.last() == '%') input.append('×')
        if (input.isEmpty() || isOperator(input.last())) {
            input.append("0.")
        } else if (currentNumber().contains('.')) {
            return
        } else {
            input.append('.')
        }
        refreshDisplay()
    }

    private fun appendPercent() {
        if (isError) clearAll()
        if (input.isNotEmpty() && input.last().isDigit()) {
            evaluated = false
            input.append("%")
        }
        refreshDisplay()
    }

    /** 正负号翻转：针对末尾正在输入的那个数。 */
    private fun toggleSign() {
        if (isError) clearAll()
        if (input.toString() == "0") return
        // 结尾挂着没写完的负号前缀，再按一次就摘掉
        if (input.last() == '−' && (input.length == 1 || isOperator(input[input.length - 2]))) {
            input.deleteCharAt(input.length - 1)
            if (input.isEmpty()) input.append("0")
            refreshDisplay()
            return
        }
        when (input.last()) {
            '+', '−' -> {
                // 还没输右边的数，直接把加减号翻面，效果等同于给后面的数取负
                val flipped = if (input.last() == '+') '−' else '+'
                input.replace(input.length - 1, input.length, flipped.toString())
                evaluated = false
            }
            '×', '÷' -> {
                // 乘除后面挂个负号前缀，等着下一个数
                input.append('−')
                evaluated = false
            }
            else -> {
                // 已有数在末尾，找到它的开头插入或摘掉负号
                var i = input.length - 1
                while (i >= 0 && (input[i].isDigit() || input[i] == '.' || input[i] == '%')) i--
                val numStart = i + 1
                if (numStart > 0 && input[numStart - 1] == '−' &&
                    (numStart - 1 == 0 || isOperator(input[numStart - 2]))
                ) {
                    input.deleteCharAt(numStart - 1)
                } else {
                    input.insert(numStart, '−')
                }
                evaluated = false
            }
        }
        refreshDisplay()
    }

    private fun backspace() {
        if (isError) { clearAll(); return }
        evaluated = false
        if (input.length > 1) input.deleteCharAt(input.length - 1) else input.clear().append("0")
        refreshDisplay()
    }

    private fun clearAll() {
        input.clear().append("0")
        isError = false
        evaluated = false
        lastOp = null
        lastOperand = null
        refreshDisplay()
    }

    private fun equals() {
        if (isError) { clearAll(); return }
        var expr = input.toString()
        if (evaluated) {
            // 重复按等号：把上一次的运算重放一遍，比如 200+50 = 之后连按 = 得 300
            val op = lastOp
            val operand = lastOperand
            if (op != null && operand != null) expr = "$input$op$operand"
        }
        val result = try {
            CalcEngine.format(CalcEngine.evaluate(expr))
        } catch (e: CalcException) {
            isError = true
            binding.txtMain.text = "错误"
            binding.txtMain.setTextColor(ContextCompat.getColor(this, R.color.error_text))
            binding.txtExpr.text = e.message
            return
        }
        binding.txtMain.setTextColor(ContextCompat.getColor(this, R.color.ink))
        binding.txtExpr.text = "${grouped(expr)} ="
        rememberLastOperation(expr)
        if (history.isEmpty() || history[0].expression != expr) {
            history.add(0, HistoryEntry(expr, result))
            if (history.size > 50) history.removeAt(history.size - 1)
        }
        input.clear().append(result)
        evaluated = true
        refreshDisplay(result)
    }

    /** 记下表达式末尾的那次运算，供重复等号用。 */
    private fun rememberLastOperation(expr: String) {
        var end = expr.length
        while (end > 0 && isOperator(expr[end - 1])) end--
        var i = end - 1
        while (i >= 0 && (expr[i].isDigit() || expr[i] == '.' || expr[i] == '%')) i--
        // 数字前面若挂着负号，往前多看一位判断是负号还是减号
        if (i >= 0 && expr[i] == '−' && (i == 0 || isOperator(expr[i - 1]))) {
            i--
            while (i >= 0 && (expr[i].isDigit() || expr[i] == '.' || expr[i] == '%')) i--
        }
        if (i < 0 || !isOperator(expr[i])) {
            lastOp = null
            lastOperand = null
            return
        }
        lastOp = expr[i]
        lastOperand = expr.substring(i + 1, end)
    }

    /** 刚出结果又按了数字：清掉重新开一个数。 */
    private fun startNewNumber() {
        input.clear()
        evaluated = false
    }

    /** 取末尾正在输入的那个数，用来判断小数点和前导零。 */
    private fun currentNumber(): String {
        var i = input.length - 1
        while (i >= 0 && (input[i].isDigit() || input[i] == '.' || input[i] == '%')) i--
        return input.substring(i + 1).trimEnd('%')
    }

    private fun refreshDisplay(main: String = input.toString()) {
        binding.txtMain.text = grouped(main)
    }

    /** 展示用的千位分隔：只动整数部分，小数和科学计数法不碰。 */
    private fun grouped(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c.isDigit() || c == '.') {
                var j = i
                while (j < s.length && (s[j].isDigit() || s[j] == '.')) j++
                sb.append(groupNumber(s.substring(i, j)))
                i = j
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun groupNumber(n: String): String {
        val dot = n.indexOf('.')
        val intPart = if (dot < 0) n else n.substring(0, dot)
        val frac = if (dot < 0) "" else n.substring(dot)
        if (intPart.length <= 3) return n
        val sb = StringBuilder()
        for ((idx, ch) in intPart.withIndex()) {
            if (idx > 0 && (intPart.length - idx) % 3 == 0) sb.append(',')
            sb.append(ch)
        }
        return sb.toString() + frac
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("input", input.toString())
        outState.putBoolean("error", isError)
        outState.putBoolean("evaluated", evaluated)
        lastOp?.let { outState.putChar("lastOp", it) }
        outState.putString("lastOperand", lastOperand)
        outState.putString("exprLine", binding.txtExpr.text.toString())
        val packed = history.map { "${it.expression}\u001F${it.result}" }
        outState.putStringArrayList("history", ArrayList(packed))
    }

    private fun restoreState(state: Bundle) {
        input.clear().append(state.getString("input") ?: "0")
        isError = state.getBoolean("error")
        evaluated = state.getBoolean("evaluated")
        lastOp = if (state.containsKey("lastOp")) state.getChar("lastOp") else null
        lastOperand = state.getString("lastOperand")
        state.getStringArrayList("history")?.forEach { line ->
            val sep = line.indexOf('\u001F')
            if (sep > 0) history.add(HistoryEntry(line.substring(0, sep), line.substring(sep + 1)))
        }
        if (isError) {
            binding.txtMain.text = "错误"
            binding.txtMain.setTextColor(ContextCompat.getColor(this, R.color.error_text))
        } else {
            refreshDisplay()
        }
        binding.txtExpr.text = state.getString("exprLine") ?: ""
    }

    private fun isOperator(c: Char) = c in "+−×÷"
}
