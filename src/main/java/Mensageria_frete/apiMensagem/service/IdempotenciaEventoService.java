package Mensageria_frete.apiMensagem.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotenciaEventoService {

	private final Map<String, EstadoProcessamento> eventos = new ConcurrentHashMap<>();

	public boolean iniciarProcessamento(String chaveIdempotencia) {
		return eventos.putIfAbsent(chaveIdempotencia, EstadoProcessamento.PROCESSANDO) == null;
	}

	public void concluirProcessamento(String chaveIdempotencia) {
		eventos.replace(chaveIdempotencia, EstadoProcessamento.PROCESSANDO, EstadoProcessamento.PROCESSADO);
	}

	public void liberarParaRetentativa(String chaveIdempotencia) {
		eventos.remove(chaveIdempotencia, EstadoProcessamento.PROCESSANDO);
	}

	private enum EstadoProcessamento {
		PROCESSANDO,
		PROCESSADO
	}
}
