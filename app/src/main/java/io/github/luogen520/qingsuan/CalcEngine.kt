package io.github.luogen520.qingsuan

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** 计算过程中的一切错误都归到这，界面层好统一提示。 */
class CalcException(message: String) : Exception(message)

/**
 * 表达式求值。界面层直接把显示用的表达式字符串丢进来。
 *
 * 百分号按手机计算器的习惯解释：
 *   200−10% = 180   （十减去它的百分之十）
 *   50×10%  = 5     （就是 50×0.1）
 *   10%     = 0.1
 *
 * 中间运算用 20 位有效数字，展示时四舍五入到 12 位，
 * 这样 0.1+0.2 就是干净的 0.3，不会带出一串浮点尾巴。
 */
object CalcEngine {

    private val MC = MathContext(20, RoundingMode.HALF_EVEN)
    private val RESULT_SIG = MathContext(12, RoundingMode.HALF_UP)

    private const val PLAIN_MAX_EXP = 15   // 整数位超过 15 位就换科学计数法
    private const val PLAIN_MIN_EXP = -12  // 小数位低于 10^-12 同理

    fun evaluate(expression: String): BigDecimal {
        val tokens = tokenize(expression)
        // 结尾悬着的运算符直接丢掉，比如按了 5× 之后直接按等号
        while (tokens.isNotEmpty() && tokens.last() is Op) tokens.removeAt(tokens.size - 1)
        if (tokens.isEmpty()) throw CalcException("表达式有误")
        val resolved = resolvePercents(tokens)
        val c = Cursor(resolved)
        return try {
            val result = evalAddSub(c)
            // 防御一下：正常来说表达式到这里刚好读完
            if (c.i != resolved.size) throw CalcException("表达式有误")
            result
        } catch (e: ArithmeticException) {
            throw CalcException("不能除以零")
        }
    }

    /** 把结果整理成给人看的字符串，多余的零一律去掉，负号统一用显示用的 −。 */
    fun format(value: BigDecimal): String {
        if (value.signum() == 0) return "0"
        val rounded = value.round(RESULT_SIG).stripTrailingZeros()
        val adjusted = rounded.precision() - rounded.scale() - 1
        val s = if (adjusted in PLAIN_MIN_EXP..PLAIN_MAX_EXP) rounded.toPlainString() else rounded.toString()
        return s.replace('-', '−')
    }

    // ---------- 内部表示 ----------

    private sealed interface Token
    private data class Num(val value: BigDecimal, val percent: Boolean = false) : Token
    private data class Op(val symbol: Char) : Token // + − × ÷

    private class Cursor(val tokens: MutableList<Token>) {
        var i = 0
    }

    // ---------- 词法 ----------

    private fun tokenize(expr: String): MutableList<Token> {
        val tokens = ArrayList<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c == '%' -> {
                    val last = tokens.lastOrNull()
                    if (last is Num && !last.percent) tokens[tokens.size - 1] = last.copy(percent = true)
                    else throw CalcException("表达式有误")
                    i++
                }
                c == '+' || c == '×' || c == '÷' || c == '−' -> {
                    // 开头或者紧跟在运算符后面的 − 是负号，并进下一个数
                    if (c == '−' && (tokens.isEmpty() || tokens.last() is Op)) {
                        val (num, next) = readNumber(expr, i + 1)
                        if (num == null) {
                            // 结尾悬着的负号留给外层去丢
                            tokens.add(Op(c))
                            i++
                        } else {
                            tokens.add(Num(num.negate()))
                            i = next
                        }
                    } else {
                        tokens.add(Op(c))
                        i++
                    }
                }
                c.isDigit() || c == '.' -> {
                    val (num, next) = readNumber(expr, i)
                    if (num == null) throw CalcException("表达式有误")
                    tokens.add(Num(num))
                    i = next
                }
                else -> throw CalcException("表达式有误")
            }
        }
        return tokens
    }

    /** 从 from 开始读一个数，返回 null 表示这里没有数字。结尾悬着的小数点就地修掉。 */
    private fun readNumber(expr: String, from: Int): Pair<BigDecimal?, Int> {
        var end = from
        var dots = 0
        while (end < expr.length && (expr[end].isDigit() || expr[end] == '.')) {
            if (expr[end] == '.') dots++
            end++
        }
        if (end == from) return null to from
        if (dots > 1) throw CalcException("表达式有误")
        var s = expr.substring(from, end)
        // 科学计数法的结果接着算：1E+16、1E−13 这种
        if (end < expr.length && (expr[end] == 'E' || expr[end] == 'e')) {
            var e2 = end + 1
            if (e2 < expr.length && (expr[e2] == '−' || expr[e2] == '+')) e2++
            val dStart = e2
            while (e2 < expr.length && expr[e2].isDigit()) e2++
            if (e2 > dStart) {
                s = expr.substring(from, e2)
                end = e2
            }
        }
        s = s.trimEnd('.')
        if (s.isEmpty()) return BigDecimal.ZERO to end
        return BigDecimal(s.replace('−', '-')) to end
    }

    // ---------- 百分号 ----------

    /** 逐个把带百分号的数换算成实际数值。 */
    private fun resolvePercents(tokensIn: MutableList<Token>): MutableList<Token> {
        val tokens = tokensIn
        while (true) {
            val idx = tokens.indexOfFirst { it is Num && it.percent }
            if (idx < 0) break
            val num = tokens[idx] as Num
            val prevOp = (tokens.getOrNull(idx - 1) as? Op)?.symbol
            val effective = when (prevOp) {
                null, '×', '÷' -> num.value.divide(BigDecimal(100), MC)
                '+', '−' -> {
                    // 加减号后面的百分数按前面的结果取百分比
                    val left = evalAddSub(Cursor(tokens.subList(0, idx - 1) as MutableList<Token>))
                    left.multiply(num.value, MC).divide(BigDecimal(100), MC)
                }
                else -> throw CalcException("表达式有误")
            }
            tokens[idx] = Num(effective)
        }
        return tokens
    }

    // ---------- 求值（先乘除后加减） ----------

    private fun evalAddSub(c: Cursor): BigDecimal {
        var acc = evalMulDiv(c)
        while (c.i < c.tokens.size) {
            val op = c.tokens[c.i] as? Op ?: break
            if (op.symbol != '+' && op.symbol != '−') break
            c.i++
            val rhs = evalMulDiv(c)
            acc = if (op.symbol == '+') acc.add(rhs) else acc.subtract(rhs)
        }
        return acc
    }

    private fun evalMulDiv(c: Cursor): BigDecimal {
        var acc = evalNum(c)
        while (c.i < c.tokens.size) {
            val op = c.tokens[c.i] as? Op ?: break
            if (op.symbol != '×' && op.symbol != '÷') break
            c.i++
            val rhs = evalNum(c)
            acc = if (op.symbol == '×') acc.multiply(rhs, MC) else acc.divide(rhs, MC)
        }
        return acc
    }

    private fun evalNum(c: Cursor): BigDecimal {
        if (c.i >= c.tokens.size) throw CalcException("表达式有误")
        val t = c.tokens[c.i]
        if (t !is Num) throw CalcException("表达式有误")
        c.i++
        return t.value
    }
}
