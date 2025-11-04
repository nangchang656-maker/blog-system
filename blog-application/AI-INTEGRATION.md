# 智普AI集成说明 - LangChain4j版本

## 改造说明

本项目已从智普AI官方SDK迁移到 **LangChain4j** 框架集成智普AI。

### 为什么选择LangChain4j?

1. **统一接口**: LangChain4j提供统一的AI接口,方便未来切换到其他AI模型(如OpenAI、文心一言等)
2. **功能强大**: 支持更多高级功能(如对话记忆、工具调用、RAG等)
3. **社区活跃**: 持续更新维护,支持最新的AI特性
4. **Spring Boot集成**: 天然支持Spring Boot生态

---

## 依赖配置

### 父POM (`pom.xml`)

```xml
<properties>
    <langchain4j.version>0.36.2</langchain4j.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- LangChain4j Core -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>

        <!-- LangChain4j ZhipuAI Integration -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-zhipu-ai</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 应用模块POM (`blog-application/pom.xml`)

```xml
<dependencies>
    <!-- LangChain4j Core -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
    </dependency>

    <!-- LangChain4j ZhipuAI Integration -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-zhipu-ai</artifactId>
    </dependency>
</dependencies>
```

---

## 配置文件

### application.yml

```yaml
# 智普AI 配置 (LangChain4j)
zhipu:
  ai:
    api-key: your_api_key_here
    model: glm-4  # 可选: glm-4, glm-4-plus, glm-3-turbo
    timeout: 30000  # 超时时间(毫秒)
    temperature: 0.7  # 温度参数(0.0-1.0), 控制随机性
    top-p: 0.95  # Top P参数(0.0-1.0), 控制多样性
    max-tokens: 2000  # 最大生成token数
```

**配置参数说明:**

- `api-key`: 智普AI的API密钥(必填)
- `model`: 使用的模型名称
  - `glm-4`: 通用模型,平衡性能和成本
  - `glm-4-plus`: 增强版,更强大但成本更高
  - `glm-3-turbo`: 更快速度,适合简单任务
- `temperature`: 控制输出的随机性
  - 0.0: 完全确定性,每次输出相同
  - 1.0: 最大随机性,输出更有创意
- `top-p`: 控制输出的多样性(建议与temperature只设置一个)
- `max-tokens`: 限制单次生成的最大token数

---

## 核心代码

### 1. 配置类 (`ZhipuAIConfig.java`)

```java
@Configuration
public class ZhipuAIConfig {

    @Bean
    public ChatLanguageModel zhipuAiChatModel() {
        return ZhipuAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .topP(properties.getTopP())
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
```

**关键点:**
- 使用 `ChatLanguageModel` 统一接口
- 返回 `ZhipuAiChatModel` 实现类
- 启用请求/响应日志便于调试

### 2. 服务类 (`ZhipuAIService.java`)

```java
@Service
public class ZhipuAIService {

    private final ChatLanguageModel chatLanguageModel;

    public String generateSummary(String content) {
        String prompt = String.format(AIConstants.PROMPT_SUMMARY, content);
        return chatLanguageModel.generate(prompt);
    }

    public String polishContent(String content) {
        String prompt = String.format(AIConstants.PROMPT_POLISH, content);
        return chatLanguageModel.generate(prompt);
    }

    public String generateOutline(String topic) {
        String prompt = String.format(AIConstants.PROMPT_OUTLINE, topic);
        return chatLanguageModel.generate(prompt);
    }

    public String chat(String userMessage) {
        return chatLanguageModel.generate(userMessage);
    }
}
```

**API变化对比:**

| 旧SDK方式 | LangChain4j方式 |
|----------|----------------|
| `ClientV4.invokeModelApi(request)` | `chatLanguageModel.generate(prompt)` |
| 需要构建复杂的Request对象 | 直接传入String提示词 |
| 需要手动解析Response | 直接返回String结果 |

---

## 测试接口

已创建 `AITestController` 用于测试AI功能:

### 1. 测试AI对话

```bash
GET http://localhost:8088/api/ai/test/chat?message=你好,请介绍一下Spring Boot
```

### 2. 测试生成文章摘要

```bash
POST http://localhost:8088/api/ai/test/summary
Content-Type: text/plain

这里是文章内容...
```

### 3. 测试生成文章大纲

```bash
GET http://localhost:8088/api/ai/test/outline?topic=Spring Boot微服务架构
```

### 4. 测试润色文章

```bash
POST http://localhost:8088/api/ai/test/polish
Content-Type: text/plain

这里是需要润色的文章内容...
```

---

## 启动验证

### 1. 启动项目

```bash
mvn spring-boot:run
```

### 2. 查看日志

启动成功后会看到类似日志:

```
初始化智普AI聊天模型 (LangChain4j), model=glm-4
```

### 3. 访问Swagger文档

访问 http://localhost:8088/swagger-ui.html

在 **AI测试接口** 分组下测试各个功能

---

## 高级用法

### 1. 对话记忆(未来扩展)

LangChain4j支持对话历史管理:

```java
@Bean
public ChatMemory chatMemory() {
    return MessageWindowChatMemory.withMaxMessages(10);
}
```

### 2. 流式响应(未来扩展)

支持SSE流式输出:

```java
public Flux<String> chatStream(String message) {
    return streamingChatLanguageModel.generateStream(message);
}
```

### 3. 工具调用(未来扩展)

支持让AI调用Java方法:

```java
@Tool("获取当前天气")
public String getWeather(String city) {
    return weatherService.getWeather(city);
}
```

---

## 故障排查

### 问题1: 编译失败,找不到ZhipuAiChatModel

**解决方案:** 确保Maven依赖已正确下载

```bash
mvn clean install -U
```

### 问题2: 运行时报错 "API key is required"

**解决方案:** 检查application.yml中的api-key配置是否正确

### 问题3: AI响应超时

**解决方案:**
1. 检查网络连接
2. 增大timeout配置
3. 使用更快的模型(如glm-3-turbo)

### 问题4: 响应内容为空

**解决方案:**
1. 检查API密钥是否有效
2. 检查账户余额是否充足
3. 查看日志中的详细错误信息(已开启logRequests和logResponses)

---

## 参考资料

- [LangChain4j官方文档](https://docs.langchain4j.dev/)
- [智普AI开放平台](https://open.bigmodel.cn/)
- [LangChain4j GitHub](https://github.com/langchain4j/langchain4j)

---

## 迁移记录

**迁移时间:** 2025-11-04

**旧版SDK:** `cn.bigmodel.openapi:oapi-java-sdk:release-V4-2.4.1`

**新版框架:** `dev.langchain4j:langchain4j-zhipu-ai:0.36.2`

**改动文件:**
- `pom.xml` - 依赖管理
- `blog-application/pom.xml` - 应用依赖
- `ZhipuAIConfig.java` - 配置类重构
- `ZhipuAIProperties.java` - 增加新配置项
- `ZhipuAIService.java` - API调用方式改造
- `application.yml` - 配置项更新
- `AITestController.java` - 新增测试接口

**优势:**
- 代码更简洁(减少约40%代码量)
- 接口更统一(未来可轻松切换其他AI)
- 功能更强大(支持更多高级特性)
