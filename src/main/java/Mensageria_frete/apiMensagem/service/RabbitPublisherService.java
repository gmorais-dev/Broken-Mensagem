package Mensageria_frete.apiMensagem.service;

import Mensageria_frete.apiMensagem.config.MensageriaProperties;
import Mensageria_frete.apiMensagem.dto.EventoFreteRequest;
import Mensageria_frete.apiMensagem.exception.PublicacaoMensageriaException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitPublisherService {

	private final RabbitTemplate rabbitTemplate;
	private final MensageriaProperties mensageriaProperties;

	public void publicar(EventoFreteRequest evento) {
		try {
			rabbitTemplate.convertAndSend(
					mensageriaProperties.rabbit().exchange(),
					mensageriaProperties.rabbit().routingKey(),
					evento
			);
		} catch (AmqpException ex) {
			throw new PublicacaoMensageriaException("Falha ao publicar evento no RabbitMQ", ex);
		}
	}
}
