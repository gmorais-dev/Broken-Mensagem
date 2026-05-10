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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MensageriaEventoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MensageriaEventoService.class);
	private static final Set<String> EVENTOS_SUPORTADOS = Set.of(
			"FRETE_CRIADO",
			"FRETE_SAIDA_CONFIRMADA",
			"FRETE_EM_TRANSITO",
			"FRETE_ENTREGUE",
			"FRETE_NAO_ENTREGUE",
			"FRETE_CANCELADO",
			"OCORRENCIA_FRETE_REGISTRADA"
	);

	private final IdempotenciaEventoService idempotenciaEventoService;
	private final RabbitPublisherService rabbitPublisherService;
	private final DashboardNotifierService dashboardNotifierService;

	public EventoFreteResponse processar(EventoFreteRequest evento) {
		validar(evento);

		String chaveIdempotencia = evento.chaveIdempotencia();
		if (!idempotenciaEventoService.iniciarProcessamento(evento, chaveIdempotencia)) {
			throw new EventoDuplicadoException("Evento ja processado: " + chaveIdempotencia);
		}

		try {
			LOGGER.info(
					"Evento de frete recebido. evento={}, freteNumero={}, freteStatus={}",
					evento.evento(),
					evento.frete().numero(),
					evento.frete().status()
			);

			rabbitPublisherService.publicar(evento);
			dashboardNotifierService.notificar(evento);
			idempotenciaEventoService.concluirProcessamento(chaveIdempotencia);

			return EventoFreteResponse.aceito(chaveIdempotencia);
		} catch (RuntimeException ex) {
			idempotenciaEventoService.registrarErro(chaveIdempotencia, ex);
			throw ex;
		}
	}

	public LoteEventoFreteResponse processarLote(LoteEventoFreteRequest lote) {
		List<EventoFreteResponse> respostas = lote.eventos()
				.stream()
				.map(this::processar)
				.toList();

		return LoteEventoFreteResponse.aceito(respostas);
	}

	private void validar(EventoFreteRequest evento) {
		if (!EVENTOS_SUPORTADOS.contains(evento.evento())) {
			throw new EventoInvalidoException("Tipo de evento invalido para frete: " + evento.evento());
		}
	}
}
