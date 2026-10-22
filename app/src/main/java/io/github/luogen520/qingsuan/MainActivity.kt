package io.github.luogen520.qingsuan

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.github.luogen520.qingsuan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 当前输入的表达式，用展示符号（×÷−）直接存，求值时再转
    private val input = StringBuilder("0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindDigitKeys()
        binding.btnClear.setOnClickListener { clearAll() }
        binding.btnBackspace.setOnClickListener { backspace() }
        binding.btnDot.setOnClickListener { appendDot() }
        binding.btnPercent.setOnClickListener { appendPercent() }
        // TODO: ± 和 = 在计算逻辑接上之后处理
    }

    private fun bindDigitKeys() {
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
    }

    private fun appendDigit(d: String) {
        if (input.toString() == "0") input.clear()
        input.append(d)
        refreshDisplay()
    }

    private fun appendOperator(op: String) {
        // 连按运算符时只替换最后一个
        val last = input.last()
        if (isOperator(last)) input.replace(input.length - 1, input.length, op) else input.append(op)
        refreshDisplay()
    }

    private fun appendDot() {
        if (input.isEmpty() || isOperator(input.last())) input.append("0")
        input.append(".")
        refreshDisplay()
    }

    private fun appendPercent() {
        if (input.isNotEmpty() && input.last().isDigit()) input.append("%")
        refreshDisplay()
    }

    private fun backspace() {
        if (input.length > 1) input.deleteCharAt(input.length - 1) else input.clear().append("0")
        refreshDisplay()
    }

    private fun clearAll() {
        input.clear().append("0")
        refreshDisplay()
    }

    private fun refreshDisplay() {
        binding.txtMain.text = input
    }

    private fun isOperator(c: Char) = c in "+−×÷"
}
