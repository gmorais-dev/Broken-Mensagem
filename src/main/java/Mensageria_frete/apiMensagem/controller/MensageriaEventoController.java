package Mensageria_frete.apiMensagem.controller;

import Mensageria_frete.apiMensagem.dto.EventoFreteRequest;
import Mensageria_frete.apiMensagem.dto.EventoFreteResponse;
import Mensageria_frete.apiMensagem.service.MensageriaEventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mensageria/eventos")
@RequiredArgsConstructor
public class MensageriaEventoController {

	private final MensageriaEventoService mensageriaEventoService;

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public EventoFreteResponse receberEvento(@Valid @RequestBody EventoFreteRequest request) {
		return mensageriaEventoService.processar(request);
	}
}
