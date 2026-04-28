
package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;

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

import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;
import com.uma.example.springuma.integration.base.AbstractIntegration;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.BodyInserters;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ImagenControllerWebTestClientIT extends AbstractIntegration {

    @LocalServerPort
    private Integer port;

    private WebTestClient testClient;

    private Paciente paciente;
    private Medico medico;

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
    }

    private void subirImagen(String nombreArchivo) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("image", new FileSystemResource(Paths.get("src/test/resources/" + nombreArchivo).toFile()));
            builder.part("paciente", paciente);

            testClient.post().uri("/imagen")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .exchange()
                    .expectStatus().isOk();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Subir imagen healthy y verificar que se sube correctamente")
    void subirImagen_Healthy_DevuelveOk() {
        subirImagen("healthy.png");
    }

    @Test
    @DisplayName("Subir imagen y recuperar sus metadatos por ID")
    void subirImagen_RecuperaInfoPorId() {
        subirImagen("healthy.png");

        testClient.get().uri("/imagen/info/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.nombre").isEqualTo("healthy.png");
    }

    @Test
    @DisplayName("Subir imagen y descargarla como bytes PNG")
    void subirImagen_DescargaImagen() {
        subirImagen("healthy.png");

        testClient.get().uri("/imagen/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG);
    }

    @Test
    @DisplayName("Obtener lista de imágenes de un paciente")
    void subirImagen_ListaImagenesPaciente() {
        subirImagen("healthy.png");

        testClient.get().uri("/imagen/paciente/" + paciente.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    @DisplayName("Realizar predicción sobre imagen subida — devuelve resultado aleatorio")
    void subirImagen_RealizaPrediccion() {
        subirImagen("healthy.png");

        String resultado = testClient.get().uri("/imagen/predict/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertTrue(resultado != null && !resultado.isEmpty(),
                "La predicción debe devolver un resultado no vacío");
        assertTrue(resultado.contains("Not cancer") || resultado.contains("Cancer"),
                "La predicción debe indicar 'Not cancer' o 'Cancer'");
    }

    @Test
    @DisplayName("Eliminar imagen subida")
    void subirImagen_EliminaImagen() {
        subirImagen("healthy.png");

        testClient.delete().uri("/imagen/1")
                .exchange()
                .expectStatus().isNoContent();
    }
}
