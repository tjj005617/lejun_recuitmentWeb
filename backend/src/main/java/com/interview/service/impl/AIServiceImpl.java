package com.interview.service.impl;

import com.interview.domain.po.InterviewQA;
import com.interview.domain.po.Job;
import com.interview.domain.po.Resume;
import com.interview.service.AIService;
import com.interview.service.ai.AiClient;
import com.interview.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 面试服务实现类
 * 调用 MiMo 模型（通过 AiClient）完成：
 * - 开场白生成：概括候选人优势 + 介绍流程 + 首个问题
 * - 问题生成：根据简历和历史回答逐轮生成问题
 * - 批量问题生成：单次 AI 调用生成全部 10 道面试题
 * - 报告生成：综合所有轮次的问答记录生成评估报告
 *
 * <p>支持会话记忆：通过 conversationId 参数启用 Redis 持久化的对话历史，
 * 使 AI 能够参考前面轮次的内容，生成更有针对性的问题和报告。</p>
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private final AiClient aiClient;

    /**
     * 手动构造函数，确保 @Qualifier 生效
     * Lombok 的 @RequiredArgsConstructor 不会将字段上的 @Qualifier 传递到构造函数参数
     */
    public AIServiceImpl(@Qualifier("mimoAiClient") AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public String generateOpening(Resume resume, String jobType) {
        String systemPrompt = "你是一位专业的面试官。";
        String userPrompt = String.format(
            "候选人简历内容如下：\n%s\n\n" +
            "请仔细阅读简历内容，然后：\n" +
            "1. 用一句话概括候选人的核心优势\n" +
            "2. 简单介绍面试流程（共10轮，每轮会根据回答评分）\n" +
            "3. 提出第一个针对简历中具体项目经验的问题\n\n" +
            "注意：问题必须基于简历中的实际内容，不要问与简历无关的问题。",
            resume.getRawContent()
        );
        return aiClient.call(systemPrompt, userPrompt);
    }

    @Override
    public String generateQuestion(Resume resume, String previousAnswer, int round, List<InterviewQA> history) {
        StringBuilder historyContext = new StringBuilder();
        for (InterviewQA qa : history) {
            historyContext.append(String.format("第%d轮：\n问题：%s\n回答：%s\n\n", qa.getRound(), qa.getQuestion(), qa.getAnswer()));
        }

        String systemPrompt = "你是一位专业的技术面试官。";
        String userPrompt = String.format(
            "候选人简历内容如下：\n%s\n\n" +
            "已完成的面试轮次：\n%s\n" +
            "上一轮回答：%s\n\n" +
            "当前是第%d轮/共10轮面试。\n\n" +
            "请根据候选人的简历和之前的回答，生成下一个面试问题。\n\n" +
            "要求：\n" +
            "1. 问题必须基于简历中的具体项目或技术栈\n" +
            "2. 逐步深入，从项目细节到原理\n" +
            "3. 结合实际场景提问\n" +
            "4. 不要重复已经问过的问题\n\n" +
            "请直接返回问题，不要添加其他内容。",
            resume.getRawContent(), historyContext, previousAnswer, round
        );
        return aiClient.call(systemPrompt, userPrompt);
    }

    @Override
    public List<String> generateAllQuestions(Resume resume, String jobType) {
        return generateAllQuestions(resume, jobType, null, null, null);
    }

    @Override
    public List<String> generateAllQuestions(Resume resume, String jobType, String conversationId) {
        return generateAllQuestions(resume, jobType, conversationId, null, null);
    }

    /**
     * 批量生成问题（支持三种模式）
     * - resume: 仅根据简历生成
     * - topic: 仅根据八股分类生成
     * - hybrid: 简历 + 八股分类
     */
    @Override
    public List<String> generateAllQuestions(Resume resume, String jobType, String conversationId, String interviewMode, List<String> categoryNames) {
        StringBuilder contextBuilder = new StringBuilder();

        // 简历上下文（resume/hybrid模式）
        if (resume != null) {
            contextBuilder.append("候选人简历内容如下：\n").append(resume.getRawContent()).append("\n\n");
        }

        // 八股分类上下文（topic/hybrid模式）
        if (categoryNames != null && !categoryNames.isEmpty()) {
            contextBuilder.append("请重点考察以下技术领域的八股知识：")
                    .append(String.join("、", categoryNames)).append("\n\n");
        }

        String systemPrompt = "你是一位专业的技术面试官。请严格返回合法的JSON数组，不要包含任何额外文本。";
        String userPrompt = String.format(
            "%s" +
            "请根据以上信息生成10道面试题，要求：\n" +
            "1. 第1题为自我介绍（如果是八股模式，第1题改为该领域的基础概念题）\n" +
            "2. 覆盖技术深度、基础知识、编码能力、系统设计\n" +
            "3. 难度从易到难递进\n" +
            "4. 结合具体技术领域出题，不要出过于宽泛的题目\n" +
            "5. 不要重复考察相同知识点\n\n" +
            "随机性要求：\n" +
            "- 每个技术领域从多个子话题中随机选取，如MySQL可从索引、事务、锁、优化、架构等不同角度出题\n" +
            "- 同一领域的问题要变换切入角度，不要总是从相同维度提问\n" +
            "- 混合不同难度和类型的概念题、场景题、原理题\n\n" +
            "返回纯JSON数组，格式：\n" +
            "[{\"index\":0,\"question\":\"题目内容\",\"category\":\"分类\"}]\n" +
            "分类包括：self-intro, project, technical, coding, system-design\n\n" +
            "重要约束：\n" +
            "- question字段中的双引号必须用\\\"转义\n" +
            "- question字段中不要使用方括号[]或花括号{}\n" +
            "- 如果需要列举内容，用顿号、逗号或换行分隔，不要用JSON数组格式\n" +
            "- 直接返回JSON数组，不要添加markdown代码块或其他内容",
            contextBuilder
        );

        String response = aiClient.call(systemPrompt, userPrompt, 2048, conversationId);
        return parseQuestionArray(response);
    }

    /**
     * 生成20道选择题（四选一）
     * 分4批生成，每批5道，避免单次请求过大导致超时
     */
    @Override
    public List<Map<String, Object>> generateChoiceQuestions(Resume resume, String jobType,
            String conversationId, String interviewMode, List<String> categoryNames) {
        StringBuilder contextBuilder = new StringBuilder();

        if (resume != null) {
            contextBuilder.append("候选人简历内容如下：\n").append(resume.getRawContent()).append("\n\n");
        }

        if (categoryNames != null && !categoryNames.isEmpty()) {
            contextBuilder.append("请重点考察以下技术领域的八股知识：")
                    .append(String.join("、", categoryNames)).append("\n\n");
        }

        String systemPrompt = "你是一位拥有10年经验的高级技术面试官，精通Java、数据库、分布式系统、微服务等技术领域。" +
            "你的任务是出选择题来考察候选人的八股文水平。" +
            "请严格返回合法的JSON数组，不要包含任何额外文本。";

        String diversityGuide = buildDiversityGuide(categoryNames);

        // 分4批生成，每批5道
        int batchSize = 5;
        int totalQuestions = 20;
        List<Map<String, Object>> allQuestions = new ArrayList<>();

        for (int batch = 0; batch < 4; batch++) {
            int startIdx = batch * batchSize;
            int endIdx = Math.min(startIdx + batchSize, totalQuestions);

            String userPrompt = String.format(
                "%s" +
                "请生成第%d~%d题（共5道选择题，四选一），要求：\n" +
                "1. 每道题有A、B、C、D四个选项，每个选项必须是具体的技术内容描述，不能是\"选项A\"这样的占位符\n" +
                "2. 选项要有干扰性，错误选项不能太明显\n" +
                "3. 每道题必须有解析说明\n" +
                "4. 这是第%d批，每批5道题，不要和之前的题目重复\n\n" +
                "%s\n" +
                "题目多样性要求：\n" +
                "- 每道题考察不同的知识点，不要重复\n" +
                "- 变换提问方式，不要都用\"以下关于XX的描述中，正确的是\"\n" +
                "- 从概念辨析、代码输出、原理分析、对比比较、最佳实践等角度出题\n\n" +
                "返回纯JSON数组，格式：\n" +
                "[{\"index\":%d,\"question\":\"具体题目内容\",\"options\":{\"A\":\"第一个选项的具体内容\",\"B\":\"第二个选项的具体内容\",\"C\":\"第三个选项的具体内容\",\"D\":\"第四个选项的具体内容\"},\"correctAnswer\":\"B\",\"explanation\":\"解析说明\",\"category\":\"分类\"}]\n" +
                "分类包括：technical, concept, theory, coding, system-design\n\n" +
                "重要约束：\n" +
                "- options的值必须是具体的选项内容（如\"HashMap底层使用数组+链表\"），绝对不能是\"选项A\"这种占位符\n" +
                "- question和options中的双引号必须用\\\"转义\n" +
                "- question中不要使用方括号[]或花括号{}\n" +
                "- correctAnswer只能是A、B、C、D中的一个\n" +
                "- 直接返回JSON数组，不要添加markdown代码块或其他内容",
                contextBuilder,
                startIdx + 1, endIdx,
                batch + 1,
                diversityGuide,
                startIdx
            );

            try {
                String response = aiClient.call(systemPrompt, userPrompt, 4096, conversationId);
                List<Map<String, Object>> batchQuestions = parseChoiceQuestionArray(response);
                allQuestions.addAll(batchQuestions);
                log.info("选择题第{}批生成成功，{}道", batch + 1, batchQuestions.size());
            } catch (Exception e) {
                log.warn("选择题第{}批生成失败: {}", batch + 1, e.getMessage());
            }
        }

        // 如果AI全部失败，使用兜底题库
        if (allQuestions.isEmpty()) {
            log.warn("AI生成选择题全部失败，使用兜底题库");
            allQuestions = generateFallbackChoiceQuestions();
        }

        log.info("选择题生成完成，共{}道", allQuestions.size());
        return allQuestions;
    }

    @Override
    public String generateReport(Resume resume, List<InterviewQA> qaList, String jobType) {
        return generateReport(resume, qaList, jobType, null);
    }

    @Override
    public String generateReport(Resume resume, List<InterviewQA> qaList, String jobType, String conversationId) {
        StringBuilder qaContext = new StringBuilder();
        for (InterviewQA qa : qaList) {
            qaContext.append(String.format("第%d轮：\n问题：%s\n回答：%s\n评分：%s\n\n",
                qa.getRound(), qa.getQuestion(), qa.getAnswer(), qa.getScores()));
        }

        String systemPrompt = "你是一位专业的面试评估专家。";
        String userPrompt = String.format(
            "请根据以下面试记录生成综合评估报告。\n\n" +
            "候选人简历：%s\n" +
            "应聘岗位：%s\n" +
            "面试记录：%s\n\n" +
            "请生成包含以下内容的报告：\n" +
            "1. 综合评分\n" +
            "2. 优势分析\n" +
            "3. 不足与改进建议\n" +
            "4. 录用建议（强烈推荐/推荐/待定/不推荐）",
            resume.getRawContent(), jobType, qaContext
        );
        return aiClient.call(systemPrompt, userPrompt, conversationId);
    }

    private List<String> parseQuestionArray(String response) {
        try {
            String json = JsonUtils.extractJsonArray(response);
            List<Map<String, Object>> questionList = JsonUtils.parseJsonList(json);

            List<String> questions = new ArrayList<>();
            for (Map<String, Object> item : questionList) {
                questions.add((String) item.get("question"));
            }
            log.info("批量生成问题成功，共{}道", questions.size());
            return questions;
        } catch (Exception e) {
            log.warn("JSON标准解析失败，尝试正则提取: {}", e.getMessage());
            return parseQuestionArrayByRegex(response);
        }
    }

    /**
     * 正则兜底解析：AI 返回的 JSON 可能包含未转义的引号导致 Jackson 解析失败，
     * 此时用正则逐个提取 "question":"..." 字段
     */
    private List<String> parseQuestionArrayByRegex(String response) {
        List<String> questions = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\"question\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
        );
        java.util.regex.Matcher matcher = pattern.matcher(response);
        while (matcher.find()) {
            String question = matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
            questions.add(question);
        }
        if (questions.isEmpty()) {
            log.error("正则提取也失败，原始响应: {}", response);
            questions.add("请做一下自我介绍");
            for (int i = 1; i < 10; i++) {
                questions.add("请描述你的技术栈和项目经验（第" + (i + 1) + "题）");
            }
        } else {
            log.info("正则提取问题成功，共{}道", questions.size());
        }
        return questions;
    }

    /**
     * 根据选择的分类构建知识点多样性指南，引导 AI 覆盖不同子话题
     */
    private String buildDiversityGuide(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            return "请覆盖Java核心知识的各个子领域：集合框架、多线程与并发、JVM原理、Spring框架、数据库、网络协议、设计模式、异常处理等。";
        }

        StringBuilder guide = new StringBuilder("你选择的考察领域及对应的知识子话题如下，请确保每个子话题至少出1-2道题：\n");

        Map<String, List<String>> topicMap = new LinkedHashMap<>();
        for (String cat : categoryNames) {
            String lower = cat.toLowerCase();
            if (lower.contains("java") || lower.contains("基础")) {
                topicMap.put(cat, List.of(
                    "集合框架（HashMap/ConcurrentHashMap原理、ArrayList区别）",
                    "多线程与并发（synchronized/ReentrantLock/线程池/CAS/AQS）",
                    "JVM（内存模型/垃圾回收/类加载/双亲委派）",
                    "异常体系（checked/unchecked/try-with-resources）",
                    "泛型与反射",
                    "Stream API与Lambda表达式"
                ));
            } else if (lower.contains("spring") || lower.contains("框架")) {
                topicMap.put(cat, List.of(
                    "IoC与DI原理",
                    "AOP实现机制（JDK动态代理/CGLIB）",
                    "Bean生命周期与作用域",
                    "事务管理（传播行为/隔离级别/@Transactional）",
                    "Spring Boot自动配置原理",
                    "Spring MVC请求处理流程"
                ));
            } else if (lower.contains("数据库") || lower.contains("mysql") || lower.contains("sql")) {
                topicMap.put(cat, List.of(
                    "索引原理（B+树/覆盖索引/联合索引最左匹配）",
                    "事务隔离级别与MVCC",
                    "SQL优化与执行计划分析",
                    "锁机制（行锁/表锁/间隙锁/死锁）",
                    "分库分表与读写分离"
                ));
            } else if (lower.contains("redis") || lower.contains("缓存")) {
                topicMap.put(cat, List.of(
                    "数据结构底层实现（ziplist/quicklist/intset）",
                    "持久化（RDB/AOF/混合持久化）",
                    "缓存问题（穿透/击穿/雪崩/一致性）",
                    "分布式锁实现",
                    "过期策略与内存淘汰"
                ));
            } else if (lower.contains("分布式") || lower.contains("微服务") || lower.contains("架构")) {
                topicMap.put(cat, List.of(
                    "CAP定理与BASE理论",
                    "分布式事务（2PC/TCC/Saga/最终一致性）",
                    "服务注册发现（Nacos/Eureka）",
                    "负载均衡与熔断降级",
                    "分布式ID生成方案",
                    "消息队列（Kafka/RabbitMQ）保证可靠性"
                ));
            } else if (lower.contains("网络") || lower.contains("http") || lower.contains("tcp")) {
                topicMap.put(cat, List.of(
                    "TCP三次握手/四次挥手",
                    "HTTP与HTTPS区别",
                    "DNS解析过程",
                    "WebSocket原理",
                    "网络IO模型（select/poll/epoll）"
                ));
            } else if (lower.contains("设计模式") || lower.contains("模式")) {
                topicMap.put(cat, List.of(
                    "单例模式（双重检查锁/枚举/静态内部类）",
                    "工厂模式与策略模式",
                    "代理模式（静态/动态代理）",
                    "观察者模式与责任链模式",
                    "模板方法模式"
                ));
            } else {
                // 未知分类，给出通用指引
                topicMap.put(cat, List.of(
                    "基础概念辨析",
                    "原理与底层实现",
                    "常见使用场景与最佳实践",
                    "常见错误与陷阱",
                    "性能优化与对比分析"
                ));
            }
        }

        for (Map.Entry<String, List<String>> entry : topicMap.entrySet()) {
            guide.append("【").append(entry.getKey()).append("】：");
            guide.append(String.join("、", entry.getValue()));
            guide.append("\n");
        }

        return guide.toString();
    }

    /**
     * 解析选择题 JSON 数组
     */
    private List<Map<String, Object>> parseChoiceQuestionArray(String response) {
        try {
            String json = JsonUtils.extractJsonArray(response);
            List<Map<String, Object>> questionList = JsonUtils.parseJsonList(json);

            List<Map<String, Object>> questions = new ArrayList<>();
            for (Map<String, Object> item : questionList) {
                Map<String, Object> q = new HashMap<>();
                q.put("question", item.get("question"));
                q.put("options", item.get("options"));
                q.put("correctAnswer", item.get("correctAnswer"));
                q.put("explanation", item.get("explanation"));
                q.put("category", item.get("category"));
                questions.add(q);
            }
            log.info("选择题生成成功，共{}道", questions.size());
            return questions;
        } catch (Exception e) {
            log.warn("选择题JSON解析失败，尝试正则提取: {}", e.getMessage());
            return parseChoiceQuestionByRegex(response);
        }
    }

    /**
     * 正则兜底解析选择题
     */
    private List<Map<String, Object>> parseChoiceQuestionByRegex(String response) {
        List<Map<String, Object>> questions = new ArrayList<>();
        // 提取每个题目的 question 字段
        java.util.regex.Pattern qPattern = java.util.regex.Pattern.compile(
            "\"question\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
        );
        // 提取 correctAnswer
        java.util.regex.Pattern aPattern = java.util.regex.Pattern.compile(
            "\"correctAnswer\"\\s*:\\s*\"([ABCD])\""
        );
        // 提取 explanation
        java.util.regex.Pattern ePattern = java.util.regex.Pattern.compile(
            "\"explanation\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
        );

        java.util.regex.Matcher qMatcher = qPattern.matcher(response);
        java.util.regex.Matcher aMatcher = aPattern.matcher(response);
        java.util.regex.Matcher eMatcher = ePattern.matcher(response);

        while (qMatcher.find()) {
            Map<String, Object> q = new HashMap<>();
            q.put("question", qMatcher.group(1).replace("\\\"", "\"").replace("\\n", "\n"));
            q.put("correctAnswer", aMatcher.find() ? aMatcher.group(1) : "A");
            q.put("explanation", eMatcher.find() ?
                eMatcher.group(1).replace("\\\"", "\"").replace("\\n", "\n") : "");
            q.put("options", Map.of("A", "选项A", "B", "选项B", "C", "选项C", "D", "选项D"));
            q.put("category", "technical");
            questions.add(q);
        }

        if (questions.isEmpty()) {
            log.error("选择题正则提取也失败，原始响应长度: {}", response.length());
            // 生成多样化的兜底选择题
            questions = generateFallbackChoiceQuestions();
        } else {
            log.info("正则提取选择题成功，共{}道", questions.size());
        }
        return questions;
    }

    /**
     * AI 调用失败时的兜底选择题（多样化，非全部相同）
     * 格式：{题目, 正确答案字母, A选项, B选项, C选项, D选项, 解析}
     */
    private List<Map<String, Object>> generateFallbackChoiceQuestions() {
        List<Map<String, Object>> questions = new ArrayList<>();
        String[][] bank = {
            // 题目, 正确答案, A, B, C, D, 解析
            {"Java中哪个关键字用于定义常量？", "C", "var", "const", "final", "static", "final关键字修饰变量后不可重新赋值。"},
            {"以下哪个不是Java基本数据类型？", "D", "boolean", "int", "char", "String", "String是引用类型，不属于8种基本数据类型。"},
            {"HashMap的底层数据结构是？", "C", "数组+链表", "纯链表", "数组+链表+红黑树", "纯红黑树", "JDK8中HashMap链表长度超过8时转为红黑树。"},
            {"线程池的核心参数不包括？", "D", "核心线程数", "最大线程数", "keepAliveTime", "队列容量", "队列容量是BlockingQueue的属性，非ThreadPoolExecutor直接参数。"},
            {"Spring Bean的默认作用域是？", "C", "prototype", "request", "singleton", "session", "Spring Bean默认是单例的，整个容器只有一个实例。"},
            {"InnoDB使用的索引结构是？", "B", "B树", "B+树", "哈希表", "跳表", "InnoDB使用B+树作为索引结构，叶子节点存储数据。"},
            {"Redis的持久化方式不包括？", "D", "RDB", "AOF", "混合持久化", "WAL", "WAL是数据库日志机制，Redis使用RDB和AOF持久化。"},
            {"TCP三次握手第二步服务端发送？", "B", "SYN", "SYN+ACK", "ACK", "FIN", "服务端收到SYN后回复SYN+ACK确认。"},
            {"JVM垃圾回收算法不包括？", "D", "标记-清除", "复制算法", "标记-整理", "顺序回收", "顺序回收不是JVM的垃圾回收算法。"},
            {"Spring AOP的底层实现是？", "B", "字节码注入", "动态代理", "编译时增强", "类加载器", "Spring AOP通过JDK动态代理或CGLIB实现。"},
            {"volatile关键字保证的是？", "C", "原子性", "可见性", "可见性和有序性", "原子性和可见性", "volatile保证可见性和有序性，但不保证原子性。"},
            {"MySQL中可能出现幻读的隔离级别？", "A", "READ COMMITTED", "REPEATABLE READ", "SERIALIZABLE", "READ UNCOMMITTED", "RC级别下可能出现幻读，RR通过MVCC和间隙锁部分解决。"},
            {"Redis哪个数据结构适合做消息队列？", "B", "String", "List", "Set", "Hash", "Redis List的LPUSH+BRPOP可以实现简单消息队列。"},
            {"Linux中查看进程的命令是？", "A", "ps", "ls", "cat", "grep", "ps命令用于查看当前系统进程信息。"},
            {"HTTP状态码304表示？", "D", "永久重定向", "临时重定向", "forbidden", "未修改", "304 Not Modified表示资源未修改，客户端可使用缓存。"},
            {"MySQL VARCHAR(50)最多存储？", "B", "50字节", "50个字符", "255个字符", "不限", "VARCHAR(50)表示最多50个字符，字节数取决于字符集。"},
            {"Docker查看容器日志的命令？", "C", "docker ps", "docker inspect", "docker logs", "docker stats", "docker logs用于查看容器的标准输出和错误日志。"},
            {"Spring Boot自动配置的核心注解？", "B", "@Configuration", "@EnableAutoConfiguration", "@ComponentScan", "@SpringBootApplication", "@EnableAutoConfiguration触发自动配置机制。"},
            {"Kafka消费者组的作用是？", "C", "数据备份", "消息过滤", "负载均衡", "消息加密", "消费者组内多个消费者分摊消费消息，实现负载均衡。"},
            {"哪个不是Linux文件操作命令？", "D", "cat", "grep", "chmod", "apt", "apt是包管理命令，不是文件操作命令。"},
        };

        for (int i = 0; i < 20; i++) {
            String[] item = bank[i % bank.length];
            Map<String, Object> q = new HashMap<>();
            q.put("question", item[0] + "（第" + (i + 1) + "题）");
            q.put("options", Map.of("A", item[2], "B", item[3], "C", item[4], "D", item[5]));
            q.put("correctAnswer", item[1]);
            q.put("explanation", item[6]);
            q.put("category", "technical");
            questions.add(q);
        }
        log.info("使用兜底选择题库，共{}道", questions.size());
        return questions;
    }

    // ==================== 简历/岗位摘要生成（用于 Embedding） ====================

    @Override
    public String generateResumeSummary(Resume resume) {
        String systemPrompt = "你是一个专业的简历分析助手。请从简历中提取关键信息，生成一段200-300字的结构化摘要，用于岗位匹配。";

        String userPrompt = String.format("""
                请分析以下简历内容，提取并总结以下关键信息：
                1. 核心技术栈和技能（列出主要技术）
                2. 工作经验年限和主要工作内容
                3. 项目经验中的亮点
                4. 教育背景
                5. 求职意向（如果有）

                要求：
                - 输出200-300字的中文摘要
                - 重点突出技术能力和经验
                - 语言简洁专业

                简历内容：
                %s
                """, resume.getRawContent());

        String response = aiClient.call(systemPrompt, userPrompt, 512);
        return response != null ? response.trim() : "";
    }

    @Override
    public String generateJobSummary(Job job) {
        String systemPrompt = "你是一个专业的岗位分析助手。请从岗位描述中提取关键信息，生成一段200-300字的结构化摘要，用于人才匹配。";

        String userPrompt = String.format("""
                请分析以下岗位信息，提取并总结以下关键内容：
                1. 岗位名称和所属行业
                2. 核心技术要求和技能需求
                3. 经验要求（年限、领域）
                4. 岗位职责要点
                5. 学历要求
                6. 薪资范围（如果有）

                要求：
                - 输出200-300字的中文摘要
                - 重点突出技术需求和经验要求
                - 语言简洁专业

                岗位信息：
                岗位名称：%s
                岗位描述：%s
                岗位要求：%s
                经验要求：%s
                学历要求：%s
                薪资范围：%s-%s元/月
                """,
                job.getTitle(),
                job.getDescription() != null ? job.getDescription() : "",
                job.getRequirements() != null ? job.getRequirements() : "",
                job.getExperience() != null ? job.getExperience() : "",
                job.getEducation() != null ? job.getEducation() : "",
                job.getSalaryMin() != null ? job.getSalaryMin() : "面议",
                job.getSalaryMax() != null ? job.getSalaryMax() : "面议"
        );

        String response = aiClient.call(systemPrompt, userPrompt, 512);
        return response != null ? response.trim() : "";
    }
}
