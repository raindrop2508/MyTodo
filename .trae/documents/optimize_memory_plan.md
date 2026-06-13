# 优化项目参数配置以减少内存占用计划 (Memory Optimization Plan)

## 1. 当前状态分析 (Current State Analysis)

* **Gradle 守护进程 (Gradle Daemon)**: 当前 `gradle.properties` 中 `org.gradle.jvmargs` 设置为 `-Xmx2048m`，未限制初始堆内存（Initial Heap Size）和元空间（Metaspace），这会导致系统在构建初期申请过多内存，并在后台持续持有不释放。

* **Kotlin 编译守护进程 (Kotlin Compiler Daemon)**: 目前项目中未对其进行单独的内存上限配置，默认情况下它会根据系统资源动态申请较大内存，容易与 IDE 本身争抢系统资源。

* **无用的高开销插件 (Unused High-Overhead Plugins)**: 审查发现 `app/build.gradle` 中应用了 `kotlin-kapt` 插件，但依赖项（Dependencies）中仅使用了 `ksp`（Kotlin 符号处理，Kotlin Symbol Processing）来处理 Room，并没有任何依赖使用 `kapt`（Kotlin 注解处理工具，Kotlin Annotation Processing Tool）。`kapt` 的存在会导致编译器强制执行 Java 存根生成（Java Stub Generation）阶段，这会极大地增加内存负担并拖慢构建流程。

## 2. 方案与决策 (Proposed Changes & Decisions)

根据你选择的\*\*“平衡优化”\*\*策略，我们将对项目进行适度调整，在保证构建速度（Build Speed）不出现明显下降的前提下，压降静态和动态内存分配。

### 2.1 修改 `gradle.properties` 参数配置

* **操作**: 调整现有的 `org.gradle.jvmargs` 参数，并显式增加 `kotlin.daemon.jvmargs`。

* **配置变更**:

  ```properties
  # 限制 Gradle Daemon 的最大堆内存为 1536m，初始堆内存为 256m，Metaspace 上限为 384m
  org.gradle.jvmargs=-Xmx1536m -Xms256m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8
  # 单独限制 Kotlin Compiler Daemon，防止其无限制膨胀
  kotlin.daemon.jvmargs=-Xmx512m -XX:MaxMetaspaceSize=256m
  ```

* **优点**: 将 Gradle 守护进程的理论最大占用从 2GB 降至 1.5GB，同时限制了初始分配（256MB）。通过显式限制 Metaspace 防止类加载导致的内存泄漏（Memory Leak）。分离配置后，能防止 Kotlin 守护进程过度贪婪。

* **缺点与潜在风险**:

  * 如果后续项目规模急剧扩大、引入大量新依赖项或复杂编译任务，1.5GB 的堆内存（Heap Memory）可能触及瓶颈，导致频繁触发垃圾回收（Garbage Collection），反而会拖慢构建速度，极端情况下甚至引发内存溢出（OutOfMemoryError）。

* **适用边界**: 该参数配置非常适合当前单模块（Single Module）、代码量中等的轻量级应用；若未来进行大型多模块化重构，需重新评估并调高内存上限。

### 2.2 移除 `app/build.gradle` 中的无用插件

* **操作**: 删除 `plugins` 块中的 `id 'kotlin-kapt'`。

* **优点**: 彻底消除 `kapt` 带来的 Java 存根生成（Java Stub Generation）任务，可瞬间释放可观的内存占用（通常在 200MB - 500MB 之间），同时明显加快增量构建（Incremental Build）速度。

* **缺点与潜在风险**:

  * 若未来你需要引入仅支持 `kapt` 而尚未适配 `ksp` 的第三方库（如旧版的依赖注入框架 Dagger 等），则必须重新引入此插件。

* **适用边界**: 只要项目当前及未来短期内坚持使用现代的 `ksp` 工具链，此项修改就是零副作用且绝对安全的。

## 3. 验证步骤 (Verification Steps)

1. 在 `gradle.properties` 和 `app/build.gradle` 中分别应用上述修改。
2. 在 Android Studio 中点击 `Sync Project with Gradle Files`。
3. 执行 `Build -> Clean Project` 然后 `Build -> Rebuild Project`，验证整体构建流程是否能成功通过，且没有性能退化。
4. 观察系统任务管理器（Task Manager）或 Android Studio 内部的内存指示器（Memory Indicator），确认 Java 进程的后台常驻内存占用是否有所回落。

