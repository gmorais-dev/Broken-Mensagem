package Mensageria_frete.apiMensagem.service;

import Mensageria_frete.apiMensagem.config.MensageriaProperties;
import Mensageria_frete.apiMensagem.dto.EventoFreteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardNotifierService {

	private final SimpMessagingTemplate messagingTemplate;
	private final MensageriaProperties mensageriaProperties;

	public void notificar(EventoFreteRequest evento) {
		messagingTemplate.convertAndSend(mensageriaProperties.websocket().topicFretes(), evento);
	}
}
