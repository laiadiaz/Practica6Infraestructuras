package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Informe;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InformeControllerWebTestClientIT extends AbstractIntegration {

    @LocalServerPort
    private Integer port;

    private WebTestClient testClient;

    private Medico medico;
    private Paciente paciente;
    private Imagen imagen;
    private Informe informe;

    @PostConstruct
    public void init() {
        testClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMillis(300000)).build();
    }

    @BeforeEach
    void setUp() {

        medico = new Medico();
        medico.setNombre("Miguel");
        medico.setId(1L);
        medico.setDni("835");
        medico.setEspecialidad("Ginecologo");

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombre("Maria");
        paciente.setDni("888");
        paciente.setEdad(20);
        paciente.setCita("Ginecologia");
        paciente.setMedico(medico);

        imagen = new Imagen();
        imagen.setId(1L);
        imagen.setPaciente(paciente);

        // Crea médico
        testClient.post().uri("/medico")
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated();

        // Crea paciente
        testClient.post().uri("/paciente")
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated();

        // Crea imagen
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(Paths.get("src/test/resources/healthy.png").toFile()));
        builder.part("paciente", paciente);

        testClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk();

    }

    private void crearInforme(String prediccion, String contenido) {
        informe = new Informe();
        informe.setPrediccion(prediccion);
        informe.setContenido(contenido);
        informe.setImagen(imagen);

        testClient.post().uri("/informe")
                .body(Mono.just(informe), Informe.class)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    @DisplayName("Crear informe asociado a una imagen y recuperarlo por ID")
    void crearInforme_RecuperaPorId() {
        crearInforme("Not cancer", "Imagen sin indicios de cancer");

        testClient.get().uri("/informe/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.prediccion").isEqualTo("Not cancer")
                .jsonPath("$.contenido").isEqualTo("Imagen sin indicios de cancer");
    }

    @Test
    @DisplayName("Obtener lista de informes asociados a una imagen")
    void crearInforme_ListaInformesDeImagen() {
        crearInforme("Not cancer", "Informe inicial");

        testClient.get().uri("/informe/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].contenido").isEqualTo("Informe inicial");
    }

    @Test
    @DisplayName("Eliminar un informe")
    void eliminarInforme_DevuelveNoContent() {
        crearInforme("Cancer", "Imagen con probabilidad de cancer alta");

        testClient.delete().uri("/informe/1")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Camino largo: crear médico, paciente, subir imagen, predecir y guardar informe")
    void caminoCompleto_MedicoPacienteImagenPrediccionInforme() {
        // La predicción
        String prediccion = testClient.get().uri("/imagen/predict/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertTrue(prediccion != null && (prediccion.contains("Not cancer") || prediccion.contains("Cancer")),
                "La predicción debe devolver estado válido");

        // Guardar el informe con el resultado de la predicción
        crearInforme(prediccion, "Informe generado desde predicción automática");

        // Verificar que el informe se puede recuperar
        testClient.get().uri("/informe/imagen/" + imagen.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1);
    }

}
