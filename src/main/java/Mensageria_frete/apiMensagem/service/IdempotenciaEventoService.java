package Mensageria_frete.apiMensagem.service;

import Mensageria_frete.apiMensagem.dto.EventoFreteRequest;
import Mensageria_frete.apiMensagem.entity.EventoProcessado;
import Mensageria_frete.apiMensagem.entity.EventoProcessadoStatus;
import Mensageria_frete.apiMensagem.repository.EventoProcessadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotenciaEventoService {

	private final EventoProcessadoRepository eventoProcessadoRepository;

	@Transactional
	public boolean iniciarProcessamento(EventoFreteRequest evento, String chaveIdempotencia) {
		Optional<EventoProcessado> eventoExistente = eventoProcessadoRepository.findByChaveIdempotencia(chaveIdempotencia);
		if (eventoExistente.isPresent()) {
			return prepararRetentativaSePossivel(eventoExistente.get());
		}

		try {
			eventoProcessadoRepository.saveAndFlush(EventoProcessado.iniciar(evento, chaveIdempotencia));
			return true;
		} catch (DataIntegrityViolationException ex) {
			return false;
		}
	}

	@Transactional
	public void concluirProcessamento(String chaveIdempotencia) {
		eventoProcessadoRepository.findByChaveIdempotencia(chaveIdempotencia)
				.ifPresent(EventoProcessado::concluir);
	}

	@Transactional
	public void registrarErro(String chaveIdempotencia, Exception ex) {
		eventoProcessadoRepository.findByChaveIdempotencia(chaveIdempotencia)
				.ifPresent(evento -> evento.registrarErro(ex.getMessage()));
	}

	private boolean prepararRetentativaSePossivel(EventoProcessado eventoProcessado) {
		if (EventoProcessadoStatus.ERRO.equals(eventoProcessado.getStatus())) {
			eventoProcessado.reprocessar();
			return true;
		}

		return false;
	}
}
