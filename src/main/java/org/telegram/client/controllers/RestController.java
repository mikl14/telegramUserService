package org.telegram.client.controllers;

import it.tdlight.jni.TdApi;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.telegram.client.service.TgClientService;

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
                return chat.title + ";" + chat.id + ";" + inviteLink;
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
            TdApi.Chat chat = telegramClient.findAChatByName(chatName);

            if (telegramClient.joinChat(chat.id)) {
                return chat.title + ";" + chat.id;
            }

        } catch (ExecutionException | TimeoutException e) {
            return "no_chat";
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "no_chat";
    }


}
