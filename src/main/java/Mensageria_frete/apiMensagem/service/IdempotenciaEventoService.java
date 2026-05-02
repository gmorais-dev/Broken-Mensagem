package Mensageria_frete.apiMensagem.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotenciaEventoService {

	private final Set<String> chavesProcessadas = ConcurrentHashMap.newKeySet();

	public boolean registrarSeNovo(String chaveIdempotencia) {
		return chavesProcessadas.add(chaveIdempotencia);
	}
}
