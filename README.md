# 轻算 Qingsuan

简洁精准的安卓计算器。不联网、无广告、不申请任何权限。

[![Get it on Orion Store](https://raw.githubusercontent.com/RookieEnough/Orion-Store/refs/heads/main/assets/orion-badge.png)](https://rookieenough.github.io/Orion-Data/redirect.html?id=qingsuan)

## 特性

- 四则运算，先乘除后加减
- 精确小数：`0.1 + 0.2 = 0.3`，中间过程保留 20 位有效数字，展示四舍五入到 12 位，`1÷3×3` 干净地归整为 `1`
- 百分号按手机计算器的习惯解释：`200−10% = 180`，`50×10% = 5`，单独的 `10% = 0.1`
- 千位分隔显示，长按结果一键复制
- 历史记录保留最近 50 条，点一条接着算
- 连按等号重复上一次运算，和系统计算器一致
- 深色模式跟随系统，支持 Android 13 主题图标
- 全部按键带触感反馈

## 下载

在 [Releases](https://github.com/luogen520/qingsuan/releases) 里拿 APK，需要 Android 8.0（API 26）及以上。也可以在 Orion Store 搜索「轻算」。

## 构建

```bash
./gradlew assembleDebug       # 调试包
./gradlew testDebugUnitTest   # 计算引擎的单元测试
```

编译 release 需要自己的签名 keystore，在 `local.properties`（不入库）里配置：

```properties
QS_STORE_FILE=/path/to/keystore
QS_STORE_PASSWORD=******
QS_KEY_ALIAS=qingsuan
QS_KEY_PASSWORD=******
```

## 代码结构

```
app/src/main/java/io/github/luogen520/qingsuan/
├── MainActivity.kt   界面、输入状态机、展示格式化
├── CalcEngine.kt     词法、百分号语义、加减乘除求值
├── HistorySheet.kt   历史记录弹层
└── HistoryEntry.kt   一条记录
```

界面是传统的 View + XML，没有引入额外框架；计算部分是纯 Kotlin，不依赖安卓，单元测试直接覆盖。

## 许可

[MIT](LICENSE)
