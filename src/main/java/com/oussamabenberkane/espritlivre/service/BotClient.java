package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.config.ApplicationProperties;
import com.oussamabenberkane.espritlivre.domain.enumeration.Channel;
import com.oussamabenberkane.espritlivre.service.dto.BotSendResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Client that delivers an admin reply through the WhatsApp bot's per-channel send layer.
 * The API never talks to Meta directly — all tokens stay in the bot.
 * <p>
 * Calls the bot over the docker network ({@code application.bot.internal-url}) authenticated
 * with a shared secret header. The endpoint is not exposed publicly via nginx.
 */
@Service
public class BotClient {

    private static final Logger LOG = LoggerFactory.getLogger(BotClient.class);

    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final ApplicationProperties applicationProperties;
    private final RestTemplate restTemplate;

    public BotClient(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Send a free-text message to the customer through the bot.
     *
     * @return the Meta message id of the delivered message, or {@code null} if the bot didn't return one.
     * @throws ResponseStatusException 502 if the bot is unreachable or rejects the send.
     */
    public String sendText(Channel channel, String senderId, String text) {
        ApplicationProperties.Bot bot = applicationProperties.getBot();
        String url = bot.getInternalUrl() + "/internal/send";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_TOKEN_HEADER, bot.getSharedSecret());

        Map<String, Object> body = new HashMap<>();
        body.put("channel", toBotChannel(channel));
        body.put("senderId", senderId);
        body.put("text", text);

        try {
            BotSendResponse response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), BotSendResponse.class);
            return response != null ? response.messageId() : null;
        } catch (RestClientException e) {
            LOG.error("Bot send failed for {}:{} : {}", channel, senderId, e.getMessage());
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "L'envoi du message a échoué : le service de messagerie est indisponible."
            );
        }
    }

    /** Map the API channel enum to the bot's per-channel client key. */
    private static String toBotChannel(Channel channel) {
        return switch (channel) {
            case WHATSAPP -> "wa";
            case MESSENGER -> "messenger";
            case INSTAGRAM -> "instagram";
        };
    }
}
