package Mensageria_frete.apiMensagem.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(
		int status,
		String erro,
		String mensagem,
		List<String> detalhes,
		LocalDateTime dataHora
) {
	public static ErroResponse of(int status, String erro, String mensagem, List<String> detalhes) {
		return new ErroResponse(status, erro, mensagem, detalhes, LocalDateTime.now());
	}
}
