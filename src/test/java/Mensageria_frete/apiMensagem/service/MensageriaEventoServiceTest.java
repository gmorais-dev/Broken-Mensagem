package Mensageria_frete.apiMensagem.service;

import Mensageria_frete.apiMensagem.dto.EventoFreteRequest;
import Mensageria_frete.apiMensagem.dto.EventoFreteResponse;
import Mensageria_frete.apiMensagem.dto.FretePayloadRequest;
import Mensageria_frete.apiMensagem.dto.OcorrenciaPayloadRequest;
import Mensageria_frete.apiMensagem.entity.EventoProcessadoStatus;
import Mensageria_frete.apiMensagem.exception.EventoDuplicadoException;
import Mensageria_frete.apiMensagem.exception.EventoInvalidoException;
import Mensageria_frete.apiMensagem.exception.PublicacaoMensageriaException;
import Mensageria_frete.apiMensagem.repository.EventoProcessadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class MensageriaEventoServiceTest {

	@Autowired
	private EventoProcessadoRepository eventoProcessadoRepository;

	private RabbitPublisherServiceFake rabbitPublisherService;
	private DashboardNotifierServiceFake dashboardNotifierService;
	private MensageriaEventoService mensageriaEventoService;

	@BeforeEach
	void setUp() {
		rabbitPublisherService = new RabbitPublisherServiceFake();
		dashboardNotifierService = new DashboardNotifierServiceFake();
		mensageriaEventoService = new MensageriaEventoService(
				new IdempotenciaEventoService(eventoProcessadoRepository),
				rabbitPublisherService,
				dashboardNotifierService
		);
	}

	@Test
	void deveProcessarEventoValido() {
		EventoFreteRequest evento = evento("FRETE_CRIADO");

		EventoFreteResponse response = mensageriaEventoService.processar(evento);

		assertEquals("ACEITO", response.status());
		assertEquals(evento.chaveIdempotencia(), response.chaveIdempotencia());
		assertEquals(1, rabbitPublisherService.totalPublicacoes);
		assertEquals(1, dashboardNotifierService.totalNotificacoes);
		assertEquals(
				EventoProcessadoStatus.PROCESSADO,
				eventoProcessadoRepository.findByChaveIdempotencia(evento.chaveIdempotencia()).orElseThrow().getStatus()
		);
	}

	@Test
	void deveProcessarEventoDeOcorrenciaRegistrada() {
		EventoFreteRequest evento = evento("OCORRENCIA_FRETE_REGISTRADA", ocorrencia());

		EventoFreteResponse response = mensageriaEventoService.processar(evento);

		assertEquals("ACEITO", response.status());
		assertEquals(evento.chaveIdempotencia(), response.chaveIdempotencia());
		assertEquals(1, rabbitPublisherService.totalPublicacoes);
		assertEquals(1, dashboardNotifierService.totalNotificacoes);
		assertEquals(
				EventoProcessadoStatus.PROCESSADO,
				eventoProcessadoRepository.findByChaveIdempotencia(evento.chaveIdempotencia()).orElseThrow().getStatus()
		);
	}

	@Test
	void deveProcessarEventoDeFreteComBlocoOpcionalDeOcorrencia() {
		EventoFreteRequest evento = evento("FRETE_ENTREGUE", ocorrencia());

		EventoFreteResponse response = mensageriaEventoService.processar(evento);

		assertEquals("ACEITO", response.status());
		assertEquals(evento.chaveIdempotencia(), response.chaveIdempotencia());
		assertEquals(1, rabbitPublisherService.totalPublicacoes);
		assertEquals(1, dashboardNotifierService.totalNotificacoes);
		assertEquals(
				EventoProcessadoStatus.PROCESSADO,
				eventoProcessadoRepository.findByChaveIdempotencia(evento.chaveIdempotencia()).orElseThrow().getStatus()
		);
	}

	@Test
	void deveBloquearEventoDuplicadoDepoisDoSucesso() {
		EventoFreteRequest evento = evento("FRETE_CRIADO");

		mensageriaEventoService.processar(evento);

		assertThrows(EventoDuplicadoException.class, () -> mensageriaEventoService.processar(evento));
		assertEquals(1, rabbitPublisherService.totalPublicacoes);
		assertEquals(1, dashboardNotifierService.totalNotificacoes);
	}

	@Test
	void devePermitirRetentativaQuandoPublicacaoFalhar() {
		EventoFreteRequest evento = evento("FRETE_CRIADO");
		rabbitPublisherService.falharProximaPublicacao = true;

		assertThrows(PublicacaoMensageriaException.class, () -> mensageriaEventoService.processar(evento));
		assertEquals(
				EventoProcessadoStatus.ERRO,
				eventoProcessadoRepository.findByChaveIdempotencia(evento.chaveIdempotencia()).orElseThrow().getStatus()
		);

		EventoFreteResponse response = mensageriaEventoService.processar(evento);

		assertEquals("ACEITO", response.status());
		assertEquals(2, rabbitPublisherService.totalPublicacoes);
		assertEquals(1, dashboardNotifierService.totalNotificacoes);
		assertEquals(
				EventoProcessadoStatus.PROCESSADO,
				eventoProcessadoRepository.findByChaveIdempotencia(evento.chaveIdempotencia()).orElseThrow().getStatus()
		);
	}

	@Test
	void deveRejeitarEventoForaDoContratoDoLegado() {
		EventoFreteRequest evento = evento("FRETE_DESCONHECIDO");

		assertThrows(EventoInvalidoException.class, () -> mensageriaEventoService.processar(evento));
		assertTrue(eventoProcessadoRepository.findAll().isEmpty());
	}

	private EventoFreteRequest evento(String tipoEvento) {
		return evento(tipoEvento, null);
	}

	private EventoFreteRequest evento(String tipoEvento, OcorrenciaPayloadRequest ocorrencia) {
		return new EventoFreteRequest(
				"1.0",
				tipoEvento,
				"SISTEMA_FRETES_WEB",
				"http://localhost:8082/api/mensageria/eventos",
				true,
				LocalDateTime.of(2026, 5, 2, 10, 30),
				new FretePayloadRequest(
						12L,
						"FRT-2026-00012",
						"EMITIDO",
						1L,
						2L,
						3L,
						4L,
						"Campinas/SP",
						"Curitiba/PR",
						new BigDecimal("9900.0"),
						new BigDecimal("9900.0")
				),
				ocorrencia
		);
	}

	private OcorrenciaPayloadRequest ocorrencia() {
		return new OcorrenciaPayloadRequest(
				"AVARIA",
				"Avaria",
				LocalDateTime.of(2026, 5, 10, 14, 35),
				"Fortaleza",
				"CE",
				"Embalagem lateral danificada.",
				"",
				""
		);
	}

	private static class RabbitPublisherServiceFake extends RabbitPublisherService {
		private int totalPublicacoes;
		private boolean falharProximaPublicacao;

		private RabbitPublisherServiceFake() {
			super(null, null);
		}

		@Override
		public void publicar(EventoFreteRequest evento) {
			totalPublicacoes++;

			if (falharProximaPublicacao) {
				falharProximaPublicacao = false;
				throw new PublicacaoMensageriaException("falha", new RuntimeException("rabbit offline"));
			}
		}
	}

	private static class DashboardNotifierServiceFake extends DashboardNotifierService {
		private int totalNotificacoes;

		private DashboardNotifierServiceFake() {
			super(null, null);
		}

		@Override
		public void notificar(EventoFreteRequest evento) {
			totalNotificacoes++;
		}
	}
}
