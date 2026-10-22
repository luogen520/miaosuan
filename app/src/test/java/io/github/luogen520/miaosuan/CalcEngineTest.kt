package io.github.luogen520.miaosuan

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class CalcEngineTest {

    private fun eval(expr: String) = CalcEngine.format(CalcEngine.evaluate(expr))

    @Test fun 基本四则() {
        assertEquals("14", eval("2+3×4"))
        assertEquals("14", eval("14"))
        assertEquals("7", eval("2+3+2"))
        assertEquals("10", eval("2×3+4"))
        assertEquals("4", eval("12÷3"))
        assertEquals("1", eval("9−2×4"))
    }

    @Test fun 浮点尾巴要干净() {
        // 0.1+0.2 必须是 0.3，不是 0.30000000000000004
        assertEquals("0.3", eval("0.1+0.2"))
        assertEquals("0.3", eval("0.1×3"))
        // 1÷3×3 在浮点计算器上是 0.9999999999999999，这里应该归整为 1
        assertEquals("1", eval("1÷3×3"))
        assertEquals("2", eval("2÷3×3"))
    }

    @Test fun 先乘除后加减() {
        assertEquals("9", eval("1+2×4"))
        assertEquals("20", eval("2+3×6"))
        assertEquals("11", eval("1+2×5"))
    }

    @Test fun 百分号加减按前值取比例() {
        assertEquals("180", eval("200−10%"))
        assertEquals("220", eval("200+10%"))
        assertEquals("5.25", eval("2+3+5%"))
    }

    @Test fun 百分号乘除按自身换算() {
        assertEquals("5", eval("50×10%"))
        assertEquals("2000", eval("200÷10%"))
    }

    @Test fun 单独的百分号就是除以一百() {
        assertEquals("0.1", eval("10%"))
        assertEquals("0.5", eval("50%"))
    }

    @Test fun 混合百分号() {
        assertEquals("2.15", eval("2+3×5%"))
        assertEquals("198", eval("200−10%+10%"))
    }

    @Test fun 负数() {
        assertEquals("−2", eval("−5+3"))
        assertEquals("−15", eval("5×−3"))
        assertEquals("8", eval("5−−3"))
    }

    @Test fun 结尾悬着的运算符直接丢弃() {
        assertEquals("5", eval("5+"))
        assertEquals("5", eval("5×−"))
    }

    @Test fun 除以零要报错() {
        try {
            CalcEngine.evaluate("5÷0")
            fail("应该抛出 CalcException")
        } catch (e: CalcException) {
            assertEquals("不能除以零", e.message)
        }
    }

    @Test fun 结果格式化() {
        assertEquals("33.3333333333", eval("100÷3"))
        assertEquals("0.25", eval("1÷4"))
        assertEquals("0", eval("0÷5"))
        assertEquals("0", eval("5×0"))
    }

    @Test fun 大数换科学计数法() {
        assertEquals("1E+16", eval("9999999999999999"))
        assertEquals("1000000000000000", eval("999999999999999"))
    }

    @Test fun 很小的数() {
        assertEquals("0.000000000001", eval("0.000000000001"))
        assertEquals("1E−13", eval("0.0000000000001"))
    }

    @Test fun 结尾小数点修掉() {
        assertEquals("5", eval("5."))
        assertEquals("5.5", eval("5.5+0."))
    }

    @Test fun 循环小数的高精度() {
        // 中间过程保 20 位，最后展示 12 位
        assertEquals("0.333333333333", eval("1÷3"))
        assertEquals("0.666666666667", eval("2÷3"))
    }
}
