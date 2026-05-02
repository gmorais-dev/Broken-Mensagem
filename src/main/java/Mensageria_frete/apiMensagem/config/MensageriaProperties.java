package Mensageria_frete.apiMensagem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mensageria")
public record MensageriaProperties(
		Rabbit rabbit,
		Websocket websocket
) {
	public record Rabbit(
			String exchange,
			String queue,
			String routingKey
	) {
	}

	public record Websocket(
			String topicFretes
	) {
	}
}
