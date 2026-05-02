package Mensageria_frete.apiMensagem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LoteEventoFreteRequest(
		@NotEmpty List<@Valid EventoFreteRequest> eventos
) {
}
