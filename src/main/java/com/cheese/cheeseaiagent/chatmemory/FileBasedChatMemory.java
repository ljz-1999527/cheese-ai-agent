package com.cheese.cheeseaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件持久化的对话记忆
 * <p>
 * 实现 Spring AI 的 ChatMemory 接口，将每个会话（conversationId）的对话消息
 * 序列化为独立的 .kryo 文件保存到磁盘，实现跨重启的对话记忆持久化。
 * 使用 Kryo 高性能序列化框架（而非 JDK 序列化），提升读写效率。
 */
public class FileBasedChatMemory implements ChatMemory {

    /**
     * 记忆文件的保存根目录
     */
    private final String BASE_DIR;
    /**
     * Kryo 序列化器（线程安全，可全局复用），用于对象与字节流的互相转换
     */
    private static final Kryo kryo = new Kryo();

    // 静态初始化块：配置 Kryo 序列化策略
    static {
        // 关闭注册要求：允许序列化任意类（不需要提前注册），提高易用性
        kryo.setRegistrationRequired(false);
        // 设置实例化策略：使用 StdInstantiatorStrategy，可绕过无参构造器创建对象（兼容更多类型）
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    /**
     * 构造对象时，指定文件保存目录（目录不存在会自动创建）
     *
     * @param dir 记忆文件的保存目录
     */
    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    /**
     * 向指定会话追加消息并持久化
     *
     * @param conversationId 会话 ID
     * @param messages       要追加的消息列表
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        // 读取该会话已有消息，追加新消息后整体写回
        List<Message> conversationMessages = getOrCreateConversation(conversationId);
        conversationMessages.addAll(messages);
        saveConversation(conversationId, conversationMessages);
    }
    /**
     * 获取指定会话的全部消息
     *
     * @param conversationId 会话 ID
     * @return 该会话的历史消息列表（文件不存在时返回空列表）
     */
    @Override
    public List<Message> get(String conversationId) {
        return getOrCreateConversation(conversationId);
    }
/*
    public List<Message> get(String conversationId, int lastN) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        return messageList.stream().skip(Math.max(0, messageList.size() - lastN)).toList();
    }
*/

    /**
     * 清空指定会话的记忆（删除对应的记忆文件）
     *
     * @param conversationId 会话 ID
     */
    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 获取指定会话的消息列表：文件存在则反序列化读取，否则返回空列表
     *
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            // 从文件反序列化消息列表（Kryo 读取）
            try (Input input = new Input(new FileInputStream(file))) {
                messages = kryo.readObject(input, ArrayList.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }

    /**
     * 将消息列表序列化保存到记忆文件
     *
     * @param conversationId 会话 ID
     * @param messages       要保存的消息列表
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        // 使用 Kryo 将消息列表序列化写入文件
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据会话 ID 定位记忆文件（文件名 = 会话ID + .kryo）
     *
     * @param conversationId 会话 ID
     * @return 对应的记忆文件对象
     */
    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
