package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.List;

public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío: no hay ninguna persona.");
        }
        String[] lineas = texto.split("\r?\n");
        List<Persona> personas = new ArrayList<>();
        List<Integer> numeros = new ArrayList<>();

        for (int i = 0; i < lineas.length; i++) {
            if (lineas[i].isBlank()) {
                continue;
            }
            int numero = i + 1;
            Persona persona = leerPersona(lineas[i], numero);
            for (Persona anterior : personas) {
                if (anterior.getPosicion().equals(persona.getPosicion())) {
                    throw new ArbolInvalidoException("Línea " + numero + ": la posición "
                            + persona.getPosicion() + " está repetida.");
                }
            }
            personas.add(persona);
            numeros.add(numero);
        }

        int indiceSujeto = -1;
        for (int i = 0; i < personas.size(); i++) {
            if (Posicion.esSujeto(personas.get(i).getPosicion())) {
                indiceSujeto = i;
            }
        }
        if (indiceSujeto == -1) {
            throw new ArbolInvalidoException("Falta el sujeto: debe existir la persona con la posición 1.");
        }

        ArbolGenealogico arbol = new ArbolGenealogico(personas.get(indiceSujeto));

        // Ascendientes: por número Ahnentafel ascendente.
        for (int n = 2; n <= Posicion.MAX_AHNENTAFEL; n++) {
            String pos = String.valueOf(n);
            for (int i = 0; i < personas.size(); i++) {
                if (personas.get(i).getPosicion().equals(pos)) {
                    agregar(arbol, personas.get(i), numeros.get(i));
                }
            }
        }

        // Descendientes: por generación.
        for (int g = 1; g <= Posicion.MAX_LONGITUD; g++) {
            for (int i = 0; i < personas.size(); i++) {
                String pos = personas.get(i).getPosicion();
                if (Posicion.esDescendiente(pos) && Posicion.generacion(pos) == g) {
                    agregar(arbol, personas.get(i), numeros.get(i));
                }
            }
        }
        return arbol;
    }

    private static Persona leerPersona(String linea, int numero) {
        String[] campos = linea.split(";", -1);
        if (campos.length < 3 || campos.length > 4) {
            throw new ArbolInvalidoException("Línea " + numero
                    + ": debe tener 3 o 4 campos separados por ';'.");
        }
        String posicion = campos[0].trim();
        String nombre = campos[1].trim();

        int nacimiento;
        try {
            nacimiento = Integer.parseInt(campos[2].trim());
        } catch (NumberFormatException e) {
            throw new ArbolInvalidoException("Línea " + numero
                    + ": el año de nacimiento debe ser un número entero.");
        }

        Integer fallecimiento = null;
        if (campos.length == 4 && !campos[3].trim().isEmpty()) {
            try {
                fallecimiento = Integer.parseInt(campos[3].trim());
            } catch (NumberFormatException e) {
                throw new ArbolInvalidoException("Línea " + numero
                        + ": el año de fallecimiento debe ser un número entero.");
            }
        }

        Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
        try {
            persona.validar();
        } catch (IllegalArgumentException e) {
            throw new ArbolInvalidoException("Línea " + numero + ": " + e.getMessage());
        }
        return persona;
    }

    private static void agregar(ArbolGenealogico arbol, Persona persona, int numero) {
        try {
            arbol.agregar(persona);
        } catch (ArbolInvalidoException e) {
            throw new ArbolInvalidoException("Línea " + numero + ": " + e.getMessage());
        }
    }
}