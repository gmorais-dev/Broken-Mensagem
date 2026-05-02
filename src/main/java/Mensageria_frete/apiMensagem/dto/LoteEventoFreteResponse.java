package Mensageria_frete.apiMensagem.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LoteEventoFreteResponse(
		String status,
		int totalRecebido,
		List<EventoFreteResponse> eventos,
		LocalDateTime dataRecebimento
) {
	public static LoteEventoFreteResponse aceito(List<EventoFreteResponse> eventos) {
		return new LoteEventoFreteResponse("ACEITO", eventos.size(), eventos, LocalDateTime.now());
	}
}
