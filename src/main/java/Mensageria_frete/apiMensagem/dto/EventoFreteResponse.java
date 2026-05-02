package Mensageria_frete.apiMensagem.dto;

import java.time.LocalDateTime;

public record EventoFreteResponse(
		String status,
		String mensagem,
		String chaveIdempotencia,
		LocalDateTime dataRecebimento
) {
	public static EventoFreteResponse aceito(String chaveIdempotencia) {
		return new EventoFreteResponse(
				"ACEITO",
				"Evento aceito para processamento",
				chaveIdempotencia,
				LocalDateTime.now()
		);
	}
}
