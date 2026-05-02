package Mensageria_frete.apiMensagem.handler;

import Mensageria_frete.apiMensagem.dto.ErroResponse;
import Mensageria_frete.apiMensagem.exception.EventoDuplicadoException;
import Mensageria_frete.apiMensagem.exception.EventoInvalidoException;
import Mensageria_frete.apiMensagem.exception.PublicacaoMensageriaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
		List<String> detalhes = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(this::formatarErroCampo)
				.toList();

		return erro(HttpStatus.BAD_REQUEST, "Payload invalido", "Revise os campos obrigatorios do evento", detalhes);
	}

	@ExceptionHandler(EventoInvalidoException.class)
	public ResponseEntity<ErroResponse> tratarEventoInvalido(EventoInvalidoException ex) {
		return erro(HttpStatus.BAD_REQUEST, "Evento invalido", ex.getMessage(), List.of());
	}

	@ExceptionHandler(EventoDuplicadoException.class)
	public ResponseEntity<ErroResponse> tratarEventoDuplicado(EventoDuplicadoException ex) {
		return erro(HttpStatus.CONFLICT, "Evento duplicado", ex.getMessage(), List.of());
	}

	@ExceptionHandler(PublicacaoMensageriaException.class)
	public ResponseEntity<ErroResponse> tratarFalhaPublicacao(PublicacaoMensageriaException ex) {
		return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Falha de publicacao", ex.getMessage(), List.of());
	}

	private ResponseEntity<ErroResponse> erro(
			HttpStatus status,
			String erro,
			String mensagem,
			List<String> detalhes
	) {
		return ResponseEntity
				.status(status)
				.body(ErroResponse.of(status.value(), erro, mensagem, detalhes));
	}

	private String formatarErroCampo(FieldError fieldError) {
		return fieldError.getField() + ": " + fieldError.getDefaultMessage();
	}
}
