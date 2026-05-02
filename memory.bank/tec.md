技术栈设计文档 — 简单且稳健的安卓日记App

1. 前端（Android客户端）
目标: 最小依赖、稳定兼容Android 6.0+

- 语言: Kotlin（现代、官方、简洁）
- UI: Jetpack Compose（声明式UI，适合列表、日历、卡片）
- 录音: Android MediaRecorder（原生稳定）
- 网络: Retrofit（轻量，支持JSON和文件上传）
- 异步: Kotlin Coroutines（内置异步支持，无需RxJava）

理由: Kotlin + Compose + Coroutines 提供完整前端功能，无额外复杂依赖。  

---

 2. 后端（云服务）
目标: 最小组件，处理AI与存储

- 框架: FastAPI (Python)
  - 处理REST接口：
    - 上传音频
    - 返回文字、分类标签、整理文章
    - 用户认证与日记查询
- 文件存储: 云对象存储（Tencent COS / Aliyun OSS）
  - 保存语音文件，通过签名URL供客户端访问
- 数据库: MongoDB Atlas（托管、灵活结构）
  - 存储日记内容、分类、标签
- 异步任务: Celery + Redis
  - 音频处理队列，保证稳定高并发处理

理由: 后端组件少，易维护且可横向扩展。  

---

 3. AI 层
目标: 可靠、无需自建复杂模型

- 语音识别: 百度语音/科大讯飞API（中文长语音识别稳定）
- 文本处理/标签生成/文章整理: OpenAI GPT API或等效服务
  - 自动生成动态标签
  - 保持口语风格，添加标点和分段

理由: 使用云端成熟AI，省去自建ML模型和运维成本。  

---

 4. 本地存储与同步
目标: 离线可用，保证数据安全

- 本地数据库: Room（SQLite封装）
  - 保存日记元数据、临时音频、搜索索引
- 同步: WorkManager后台任务
  - 网络恢复时自动上传未处理日记
- 用户认证: 手机号/邮箱 + 云端JWT Session Token

理由: 保证离线可用，简化同步逻辑。  

---

 5. 可选组件
- 搜索: MongoDB文本索引 + Room本地搜索备份
- 崩溃与分析: Firebase Crashlytics（轻量）
- 部署: 单Docker容器（FastAPI + Celery Worker）

---

 6. 核心优势
1. 最小化架构: Android ↔ FastAPI ↔ 云服务
2. 轻量: 无复杂微服务或自建ML
3. 云备份: 用户设备更换无影响
4. 离线支持: Room + WorkManager保证无网络可用
5. 可扩展: 云存储 + MongoDB + Celery轻松应对大流量