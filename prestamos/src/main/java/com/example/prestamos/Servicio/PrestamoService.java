package com.example.prestamos.servicio;

import com.example.prestamos.cliente.BibliotecaClient;
import com.example.prestamos.modelo.LibroRespuesta;
import com.example.prestamos.modelo.OperacionRespuesta;
import com.example.prestamos.modelo.Prestamo;
import com.example.prestamos.modelo.SolicitudPrestamo;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrestamoService {

    private final BibliotecaClient bibliotecaClient;
    private final OperacionLocalService operaciones;

    public PrestamoService(
            BibliotecaClient bibliotecaClient,
            OperacionLocalService operaciones) {

        this.bibliotecaClient = bibliotecaClient;
        this.operaciones = operaciones;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Prestamo registrar(SolicitudPrestamo solicitud) {

        // Esta llamada termina y confirma su transacción
        // antes de continuar con la comunicación HTTP.
        OperacionRespuesta operacion =
                operaciones.iniciar(solicitud);

        if (!"PENDIENTE".equals(operacion.estado())) {
            return resolverResultado(operacion);
        }

        try {
            bibliotecaClient.reservarOperacion(
                    operacion.operacionId(),
                    operacion.libroId()
            );

        } catch (ResponseStatusException error) {

            int codigo = error.getStatusCode().value();

            if (codigo == 404 || codigo == 409) {

                OperacionRespuesta resultado =
                        operaciones.rechazar(
                                operacion.operacionId(),
                                codigo,
                                error.getReason()
                        );

                return resolverResultado(resultado);
            }

            // No marcar como rechazada una operación incierta.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Resultado no confirmado. Reintenta con el mismo UUID: "
                            + operacion.operacionId()
            );
        }

        // La reserva ya está confirmada.
        // Consultar el libro permite obtener su título.
        // No rechazamos disponible=false: es lo esperado.
        LibroRespuesta libro =
                bibliotecaClient.consultar(operacion.libroId());

        // Nueva transacción local.
        return operaciones.confirmar(
                operacion.operacionId(),
                libro
        );
    }

    public List<Prestamo> obtenerTodos() {
        return operaciones.obtenerTodos();
    }

    public Prestamo buscarPorId(int id) {
        return operaciones.buscarPorId(id);
    }

    public OperacionRespuesta consultarOperacion(UUID operacionId) {
        return operaciones.consultar(operacionId);
    }

    private Prestamo resolverResultado(
            OperacionRespuesta operacion) {

        if ("CONFIRMADA".equals(operacion.estado())) {
            return operaciones.buscarPorId(
                    operacion.prestamoId()
            );
        }

        if ("RECHAZADA".equals(operacion.estado())) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(operacion.codigoError()),
                    operacion.mensaje()
            );
        }

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "La operación continúa pendiente"
        );
    }
}
