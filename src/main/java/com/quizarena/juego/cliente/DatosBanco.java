package com.quizarena.juego.cliente;

import com.quizarena.juego.modelo.Pregunta;
import java.util.List;

/** Datos de un banco traidos de Identidad, listos para crear una sala. */
public record DatosBanco(String idBanco, String materia, List<Pregunta> preguntas) {}
