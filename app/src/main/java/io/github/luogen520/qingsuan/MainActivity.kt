package io.github.luogen520.qingsuan

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.luogen520.qingsuan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 当前输入的表达式，用展示符号（×÷−）直接存，求值交给 CalcEngine
    private val input = StringBuilder("0")
    private var isError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindKeys()
    }

    private fun bindKeys() {
        val digitKeys = mapOf<Button, String>(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9",
        )
        digitKeys.forEach { (btn, d) -> btn.setOnClickListener { appendDigit(d) } }
        binding.btnPlus.setOnClickListener { appendOperator("+") }
        binding.btnMinus.setOnClickListener { appendOperator("−") }
        binding.btnMultiply.setOnClickListener { appendOperator("×") }
        binding.btnDivide.setOnClickListener { appendOperator("÷") }
        binding.btnDot.setOnClickListener { appendDot() }
        binding.btnPercent.setOnClickListener { appendPercent() }
        binding.btnBackspace.setOnClickListener { backspace() }
        binding.btnClear.setOnClickListener { clearAll() }
        binding.btnEquals.setOnClickListener { equals() }
        // TODO: ± 的补全逻辑下一轮加上
    }

    private fun appendDigit(d: String) {
        if (isError) clearAll()
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
        if (input.isNotEmpty() && input.last().isDigit()) input.append("%")
        refreshDisplay()
    }

    private fun backspace() {
        if (isError) { clearAll(); return }
        if (input.length > 1) input.deleteCharAt(input.length - 1) else input.clear().append("0")
        refreshDisplay()
    }

    private fun clearAll() {
        input.clear().append("0")
        isError = false
        refreshDisplay()
    }

    private fun equals() {
        if (isError) { clearAll(); return }
        val expr = input.toString()
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
        binding.txtExpr.text = "$expr ="
        input.clear().append(result)
        refreshDisplay(result)
    }

    /** 取末尾正在输入的那个数，用来判断小数点和前导零。 */
    private fun currentNumber(): String {
        var i = input.length - 1
        while (i >= 0 && (input[i].isDigit() || input[i] == '.' || input[i] == '%')) i--
        return input.substring(i + 1).trimEnd('%')
    }

    private fun refreshDisplay(main: String = input.toString()) {
        binding.txtMain.text = main
    }

    private fun isOperator(c: Char) = c in "+−×÷"
}
