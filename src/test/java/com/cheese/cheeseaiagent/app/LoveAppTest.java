package com.cheese.cheeseaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

/**
 * LoveApp 核心应用测试类
 * <p>
 * 覆盖 LoveApp 提供的多种对话模式：
 * 普通对话、带报告对话、RAG 增强对话、工具调用对话与 MCP 对话。
 */
@SpringBootTest
class LoveAppTest {

    /** 注入 LoveApp 核心应用 Bean */
    @Resource
    private LoveApp loveApp;

    /**
     * 测试普通多轮对话：验证聊天记忆功能
     * 前两轮提供背景信息，第三轮验证 AI 能否回忆起之前的内容。
     */
    @Test
    void testChat() {
        // 生成唯一的会话 ID，模拟一次新会话
        String chatId = UUID.randomUUID().toString();
        // 第一轮：打招呼并自我介绍
        String message = "你好，我是程序员鱼皮";
        String answer = loveApp.doChat(message, chatId);
        // 第二轮：提出诉求
        message = "我想让另一半（编程导航）更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮：验证记忆——AI 应能回忆第一轮提到的称呼
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 测试带报告输出的对话模式
     */
    @Test
    void doChatWithReport() {
        // 生成会话 ID 并发送消息
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员鱼皮，我想让另一半（编程导航）更爱我，但我不知道该怎么做";
        // 调用带报告模式的对话接口，返回结构化报告
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        // 断言报告不为空
        Assertions.assertNotNull(loveReport);
    }

    /**
     * 测试 RAG 增强对话模式
     */
    @Test
    void doChatWithRag() {
        // 生成会话 ID 并发送需要知识库支撑的问题
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        // 调用 RAG 对话接口
        String answer = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 测试工具调用对话模式
     * 依次测试：联网搜索、网页抓取、资源下载、终端操作、文件操作、PDF 生成六类工具。
     */
    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");

        // 测试网页抓取：恋爱案例分析
        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
        testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");
    }

    /**
     * 私有辅助方法：发送一条消息并断言返回不为空
     *
     * @param message 要发送的消息内容
     */
    private void testMessage(String message) {
        // 每次调用使用新的会话 ID，模拟独立会话
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 测试 MCP（模型上下文协议）对话模式
     * 当前启用图片搜索 MCP 的测试；地图 MCP 测试被注释保留。
     */
    @Test
    void doChatWithMcp() {
        // 生成会话 ID
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
//        String message = "我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点";
//        String answer =  loveApp.doChatWithMcp(message, chatId);
//        Assertions.assertNotNull(answer);
        // 测试图片搜索 MCP
        String message = "帮我搜索一些哄另一半开心的图片";
        String answer =  loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
