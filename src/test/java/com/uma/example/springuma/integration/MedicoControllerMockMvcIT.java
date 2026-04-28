package com.uma.example.springuma.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;

public class MedicoControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Medico medico;

    @BeforeEach
    void setUp() {
        medico = new Medico();
        medico.setId(1L);
        medico.setDni("835");
        medico.setNombre("Miguel");
        medico.setEspecialidad("Ginecologia");
    }

    private void crearMedico(Medico medico) throws Exception {
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());
    }

    
    @Test
    @DisplayName("Crear un médico y recuperarlo por ID")
    void crearMedico_RecuperaPorId() throws Exception {
        crearMedico(medico);

        mockMvc.perform(get("/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.nombre").value("Miguel"))
                .andExpect(jsonPath("$.especialidad").value("Ginecologia"));
    }

    @Test
    @DisplayName("Crear un médico y recuperarlo por DNI")
    void crearMedico_RecuperaPorDni() throws Exception {
        crearMedico(medico);

        mockMvc.perform(get("/medico/dni/" + medico.getDni()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.dni").value("835"));
    }

    @Test
    @DisplayName("Actualizar un médico existente")
    void actualizarMedico_CambiaEspecialidad() throws Exception {
        crearMedico(medico);

        medico.setEspecialidad("Radiologia");

        mockMvc.perform(put("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/medico/" + medico.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especialidad").value("Radiologia"));
    }

    @Test
    @DisplayName("Eliminar un médico existente")
    void eliminarMedico_DevuelveOk() throws Exception {
        crearMedico(medico);

        mockMvc.perform(delete("/medico/" + medico.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Buscar médico por DNI inexistente devuelve 404")
    void buscarMedicoDniInexistente_DevuelveNotFound() throws Exception {
        mockMvc.perform(get("/medico/dni/DNI_NO_EXISTE"))
                .andExpect(status().isNotFound());
    }
}
