# Hospital Spring Boot – Gestión de Mamografías

Sistema de gestión hospitalaria con análisis de imágenes de mamografías mediante IA.

## Estado del pipeline

![CI](https://github.com/laiadiaz/Practica6Infraestructuras/actions/workflows/ci.yml/badge.svg?branch=main)
![Coverage](.github/badges/jacoco.svg)

## Descripción

El sistema permite a los médicos gestionar sus pacientes, subir imágenes de mamografías y obtener predicciones automáticas sobre la probabilidad de cáncer.

## Cómo ejecutar los tests

```bash
# Dar permisos al wrapper
chmod +x ./mvnw

# Compilar
./mvnw compile --no-transfer-progress

# Ejecutar todos los tests de integración + informe JaCoCo
./mvnw verify --no-transfer-progress
```

El informe de cobertura se genera en `target/site/jacoco/index.html`.
