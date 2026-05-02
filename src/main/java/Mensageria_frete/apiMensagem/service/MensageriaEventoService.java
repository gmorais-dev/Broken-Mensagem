package Mensageria_frete.apiMensagem.service;

import Mensageria_frete.apiMensagem.dto.EventoFreteRequest;
import Mensageria_frete.apiMensagem.dto.EventoFreteResponse;
import Mensageria_frete.apiMensagem.dto.LoteEventoFreteRequest;
import Mensageria_frete.apiMensagem.dto.LoteEventoFreteResponse;
import Mensageria_frete.apiMensagem.exception.EventoDuplicadoException;
import Mensageria_frete.apiMensagem.exception.EventoInvalidoException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MensageriaEventoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MensageriaEventoService.class);

	private final IdempotenciaEventoService idempotenciaEventoService;
	private final RabbitPublisherService rabbitPublisherService;
	private final DashboardNotifierService dashboardNotifierService;

	public EventoFreteResponse processar(EventoFreteRequest evento) {
		validar(evento);

		String chaveIdempotencia = evento.chaveIdempotencia();
		if (!idempotenciaEventoService.registrarSeNovo(chaveIdempotencia)) {
			throw new EventoDuplicadoException("Evento ja processado: " + chaveIdempotencia);
		}

		LOGGER.info(
				"Evento de frete recebido. evento={}, freteNumero={}, freteStatus={}",
				evento.evento(),
				evento.frete().numero(),
				evento.frete().status()
		);

		rabbitPublisherService.publicar(evento);
		dashboardNotifierService.notificar(evento);

		return EventoFreteResponse.aceito(chaveIdempotencia);
	}

	public LoteEventoFreteResponse processarLote(LoteEventoFreteRequest lote) {
		List<EventoFreteResponse> respostas = lote.eventos()
				.stream()
				.map(this::processar)
				.toList();

		return LoteEventoFreteResponse.aceito(respostas);
	}

	private void validar(EventoFreteRequest evento) {
		if (!evento.evento().startsWith("FRETE_")) {
			throw new EventoInvalidoException("Tipo de evento invalido para frete: " + evento.evento());
		}
	}
}
