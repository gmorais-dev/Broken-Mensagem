package Mensageria_frete.apiMensagem.service;

import Mensageria_frete.apiMensagem.dto.EventoFreteRequest;
import Mensageria_frete.apiMensagem.dto.FretePayloadRequest;
import Mensageria_frete.apiMensagem.entity.EventoProcessado;
import Mensageria_frete.apiMensagem.entity.EventoProcessadoStatus;
import Mensageria_frete.apiMensagem.repository.EventoProcessadoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
class IdempotenciaEventoServiceTest {

	@Autowired
	private EventoProcessadoRepository eventoProcessadoRepository;

	@Test
	void deveReativarRetentativaApenasUmaVezQuandoEventoEstiverComErro() {
		EventoFreteRequest evento = evento("FRETE_CRIADO");
		EventoProcessado eventoProcessado = eventoProcessadoRepository.saveAndFlush(
				EventoProcessado.iniciar(evento, evento.chaveIdempotencia())
		);

		eventoProcessado.registrarErro("rabbit offline");
		eventoProcessadoRepository.saveAndFlush(eventoProcessado);

		int primeiraAtualizacao = eventoProcessadoRepository.atualizarStatusSeCorresponder(
				evento.chaveIdempotencia(),
				EventoProcessadoStatus.ERRO,
				EventoProcessadoStatus.PROCESSANDO
		);
		int segundaAtualizacao = eventoProcessadoRepository.atualizarStatusSeCorresponder(
				evento.chaveIdempotencia(),
				EventoProcessadoStatus.ERRO,
				EventoProcessadoStatus.PROCESSANDO
		);

		EventoProcessado atualizado = eventoProcessadoRepository.findByChaveIdempotencia(evento.chaveIdempotencia())
				.orElseThrow();

		assertEquals(1, primeiraAtualizacao);
		assertEquals(0, segundaAtualizacao);
		assertEquals(EventoProcessadoStatus.PROCESSANDO, atualizado.getStatus());
		assertNull(atualizado.getDataProcessamento());
		assertNull(atualizado.getMensagemErro());
	}

	private EventoFreteRequest evento(String tipoEvento) {
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
				)
		);
	}
}
