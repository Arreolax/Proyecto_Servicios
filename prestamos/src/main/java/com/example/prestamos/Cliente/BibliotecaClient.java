package com.example.prestamos.cliente;

import com.example.prestamos.modelo.LibroRespuesta;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BibliotecaClient {

    private final RestClient cliente;

    public BibliotecaClient(
            @Value("${biblioteca.url}") String bibliotecaUrl) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(5));

        cliente = RestClient.builder()
                .baseUrl(bibliotecaUrl)
                .requestFactory(factory)
                .build();
    }

    public LibroRespuesta consultar(int id) {
        try {
            LibroRespuesta libro = cliente.get()
                    .uri("/api/libros/{id}", id)
                    .retrieve()
                    .body(LibroRespuesta.class);

            return verificarRespuesta(libro, id);

        } catch (RestClientResponseException error) {
            throw traducirErrorHttp(error);

        } catch (RestClientException error) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No fue posible consultar el servicio de biblioteca"
            );
        }
    }

    public LibroRespuesta reservar(int id) {
        try {
            LibroRespuesta libro = cliente.post()
                    .uri("/api/libros/{id}/reserva", id)
                    .retrieve()
                    .body(LibroRespuesta.class);

            libro = verificarRespuesta(libro, id);

            if (libro.disponible()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Biblioteca no confirmó la reserva"
                );
            }

            return libro;

        } catch (RestClientResponseException error) {
            throw traducirErrorHttp(error);

        } catch (RestClientException error) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo confirmar la reserva. "
                    + "Verifica la disponibilidad antes de intentar nuevamente"
            );
        }
    }

    private LibroRespuesta verificarRespuesta(
            LibroRespuesta libro, int idEsperado) {

        if (libro == null
                || libro.id() != idEsperado
                || libro.titulo() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Biblioteca devolvió una respuesta inesperada"
            );
        }

        return libro;
    }

    private ResponseStatusException traducirErrorHttp(
            RestClientResponseException error) {

        return switch (error.getStatusCode().value()) {
            case 404 -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El libro solicitado no existe"
            );

            case 409 -> new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El libro ya está prestado o reservado"
            );

            default -> new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El servicio de biblioteca respondió con un error"
            );
        };
    }
}
