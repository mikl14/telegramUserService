package org.telegram.client.controllers;

import it.tdlight.jni.TdApi;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.telegram.client.service.TgClientService;

import java.util.HashSet;
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


    @PostMapping("/joinChat")
    public String joinToChat(@RequestBody String chatName) {
        try {
            TdApi.Chat chat = telegramClient.findAChatByName(chatName);
            if(telegramClient.joinChat(chat.id))
            {
                return chat.title +";"+ chat.id;
            }

        } catch (ExecutionException | TimeoutException e) {
            return "no_chat";
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "no_chat";
    }


}
