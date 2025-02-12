package org.telegram.client.controllers;

import it.tdlight.jni.TdApi;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.telegram.client.service.TgClientService;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/client/api")
public class RestController {

    private final TgClientService telegramClient;

    public RestController(TgClientService telegramClient) {
        this.telegramClient = telegramClient;
        this.telegramClient.Init();
    }

    /**
     * <b>joinToPrivateChat</b> - присоединяет к чату по post запросу, в теле которого написана ссылка на приватный чат
     * @param inviteLink
     * @return
     */
    @PostMapping("/joinPrivateChat")
    public String joinToPrivateChat(@RequestBody String inviteLink) {
        try {
            TdApi.Chat chat = telegramClient.joinPrivateChat(inviteLink);
            if (chat != null) {
                if(!chat.title.contains(";")) {
                    return chat.title + ";" + chat.id + ";" + inviteLink;
                }
                else
                {
                    return chat.title.replace(";","_") + ";" + chat.id + ";" + inviteLink;
                }

            }
        } catch (ExecutionException | TimeoutException e) {
            return "chat_is_already";
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "chat_is_already";
    }

    /**
     * <b>joinToChat</b> - присоединяет к чату по post запросу, в теле которого написан username публчиного чата
     * @param chatName
     * @return
     */
    @PostMapping("/joinChat")
    public String joinToChat(@RequestBody String chatName) {
        try {
            if(chatName.startsWith("https://t.me/"))
            {
                chatName = chatName.replace("https://t.me/","@");
            }
            TdApi.Chat chat = telegramClient.findAChatByName(chatName);

            if (telegramClient.joinChat(chat.id)) {
                if(!chat.title.contains(";")) {
                    return chat.title + ";" + chat.id;
                }
                else
                {
                    return chat.title.replace(";","_") + ";" + chat.id;
                }
            }

        } catch (ExecutionException | TimeoutException e) {
            return "no_chat";
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "no_chat";
    }

    /**
     * <b>leaveChat</b> - выходит из указанного чата
     * @param chatId
     * @return
     */
    @PostMapping("/leaveChat")
    public String leaveFromChat(@RequestBody String chatId) {
        try {
            long id = Long.parseLong(chatId);
            long[] chats = telegramClient.getChatList();
            if (Arrays.stream(chats).anyMatch(x -> x == id))
            {
                telegramClient.leaveChatByChatId(id);
            }

        } catch (ExecutionException | TimeoutException e) {
            return "no_chat";
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "no_chat";
    }

}
