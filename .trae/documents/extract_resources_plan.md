# 计划：提取硬编码资源到 Values 文件

## 目标

将 `MainViewModel.kt` 和 `activity_main.xml` 中的所有硬编码字符串和尺寸提取到资源文件中。
根据用户要求，为了避免单个文件过大，我们将采用**按页面分文件**的策略来管理资源。

## 评估与策略

* **Strings (文字)**: 将新建 `strings_main.xml` 专门存放 MainActivity 相关的文字。

* **Dimens (尺寸)**: 将新建 `dimens_main.xml` 专门存放 MainActivity 相关的尺寸。

* **Colors (颜色)**: 目前代码中未使用硬编码颜色（使用主题默认色），因此本次无需提取颜色资源，保持
  `colors.xml` 不变。

## 实施步骤

### 1. 创建页面专属资源文件

* **创建文件:** `app/src/main/res/values/strings_main.xml`

    * 添加内容：

      ```xml
      <resources>
          <string name="main_hello_from_viewmodel">Hello from ViewModel!</string>
          <string name="main_text_updated">Text updated via LiveData!</string>
          <string name="main_update_text_button">Update Text</string>
      </resources>
      ```

* **创建文件:** `app/src/main/res/values/dimens_main.xml`

    * 添加内容：

      ```xml
      <resources>
          <dimen name="main_button_margin_top">16dp</dimen>
      </resources>
      ```

### 2. 更新 ViewModel

* **文件:** `app/src/main/java/com/gordon/mypotato/MainViewModel.kt`

    * 将 `MainViewModel` 改为继承 `AndroidViewModel`。

    * 更新构造函数以接收 `Application` 参数。

    * 使用 `getApplication<Application>().getString(R.string.main_hello_from_viewmodel)` 等引用新资源。

### 3. 更新布局文件

* **文件:** `app/src/main/res/layout/activity_main.xml`

    * 将 `android:text="Update Text"` 替换为 `@string/main_update_text_button`。

    * 将 `android:layout_marginTop="16dp"` 替换为 `@dimen/main_button_margin_top`。

## 验证

* 构建项目以确保资源合并正确，R 类生成无误。

* 运行应用程序，验证文本和布局显示正常。

