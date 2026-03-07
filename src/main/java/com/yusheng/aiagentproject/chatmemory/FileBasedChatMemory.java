package com.yusheng.aiagentproject.chatmemory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.ModelOptionsUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileBasedChatMemory implements ChatMemory {

    private static final ObjectMapper OBJECT_MAPPER = ModelOptionsUtils.OBJECT_MAPPER;
    private static final TypeReference<List<StoredMessage>> LIST_TYPE = new TypeReference<>() {};

    private final String baseDir;
    private final int lastN;

    public FileBasedChatMemory(String dir) {
        this(dir, Integer.MAX_VALUE);
    }

    public FileBasedChatMemory(String dir, int lastN) {
        this.baseDir = Objects.requireNonNull(dir, "dir");
        this.lastN = lastN;
        File base = new File(dir);
        if (!base.exists()) {
            base.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, Message message) {
        add(conversationId, List.of(message));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        synchronized (this) {
            List<Message> conversationMessages = getOrCreateConversation(conversationId);
            conversationMessages.addAll(messages);
            saveConversation(conversationId, conversationMessages);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        List<Message> allMessages = getOrCreateConversation(conversationId);
        if (lastN <= 0 || allMessages.isEmpty()) {
            return List.of();
        }
        if (lastN >= allMessages.size()) {
            return allMessages;
        }
        return allMessages.subList(allMessages.size() - lastN, allMessages.size());
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (FileInputStream in = new FileInputStream(file)) {
            List<StoredMessage> stored = OBJECT_MAPPER.readValue(in, LIST_TYPE);
            return toMessages(stored);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read chat memory file: " + file.getAbsolutePath(), e);
        }
    }

    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        List<StoredMessage> stored = toStored(messages);
        try (FileOutputStream out = new FileOutputStream(file)) {
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(out, stored);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write chat memory file: " + file.getAbsolutePath(), e);
        }
    }

    private File getConversationFile(String conversationId) {
        return new File(baseDir, conversationId + ".json");
    }

    private static List<StoredMessage> toStored(List<Message> messages) {
        List<StoredMessage> stored = new ArrayList<>(messages.size());
        for (Message message : messages) {
            stored.add(new StoredMessage(message.getMessageType().getValue(), message.getText()));
        }
        return stored;
    }

    private static List<Message> toMessages(List<StoredMessage> stored) {
        List<Message> messages = new ArrayList<>(stored.size());
        for (StoredMessage item : stored) {
            MessageType type = MessageType.fromValue(item.type());
            String text = item.text();
            if (type == MessageType.USER) {
                messages.add(new UserMessage(text));
            } else if (type == MessageType.SYSTEM) {
                messages.add(new SystemMessage(text));
            } else {
                messages.add(new AssistantMessage(text));
            }
        }
        return messages;
    }

    private record StoredMessage(String type, String text) {}
}

