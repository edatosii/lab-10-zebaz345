package co.umariana.modelo;

import java.util.ArrayList;
import java.util.List;

public class ArbolGenealogico {

    private static class Nodo {
        private final Persona persona;
        private Nodo padre;
        private Nodo madre;
        private final List<Nodo> hijos = new ArrayList<>();

        private Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    private final Nodo raiz;
    private int cantidad;

    public ArbolGenealogico(Persona sujeto) {
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El sujeto debe tener la posición 1.");
        }
        raiz = new Nodo(sujeto);
        cantidad = 1;
    }

    public Persona sujeto() {
        return raiz.persona;
    }

    public int tamano() {
        return cantidad;
    }

    public Persona buscar(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        return nodo == null ? null : nodo.persona;
    }

    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    public Persona agregar(Persona persona) {
        persona.validar();
        String pos = persona.getPosicion();
        if (Posicion.esSujeto(pos) || buscarNodo(pos) != null) {
            throw new ArbolInvalidoException("La posición " + pos + " está repetida.");
        }
        if (Posicion.esAscendiente(pos)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(pos);
            Nodo hijo = buscarNodo(posHijo);
            if (hijo == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + pos
                        + " porque no existe la posición " + posHijo + ".");
            }
            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La posición " + pos
                        + " debe haber nacido antes que la posición " + posHijo + ".");
            }
            Nodo nuevo = new Nodo(persona);
            if (Integer.parseInt(pos) % 2 == 0) {
                hijo.padre = nuevo;
            } else {
                hijo.madre = nuevo;
            }
            cantidad++;
            return persona;
        }
        String posProgenitor = Posicion.posicionDelProgenitor(pos);
        Nodo progenitor = buscarNodo(posProgenitor);
        if (progenitor == null) {
            throw new ArbolInvalidoException("No se puede agregar la posición " + pos
                    + " porque no existe la posición " + posProgenitor + ".");
        }
        if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {
            throw new ArbolInvalidoException("La posición " + pos
                    + " debe haber nacido después que la posición " + posProgenitor + ".");
        }
        Nodo nuevo = new Nodo(persona);
        int orden = Posicion.orden(pos);
        int i = 0;
        while (i < progenitor.hijos.size()
                && Posicion.orden(progenitor.hijos.get(i).persona.getPosicion()) < orden) {
            i++;
        }
        progenitor.hijos.add(i, nuevo);
        cantidad++;
        return persona;
    }

    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo nodo = nodoParaAscendiente(posicion);
        String posPadre = Posicion.posicionDelPadre(posicion);
        if (!Posicion.esAhnentafel(posPadre)) {
            throw new ArbolInvalidoException("No se pueden agregar más generaciones de ascendientes.");
        }
        if (nodo.padre != null) {
            throw new ArbolInvalidoException("El padre de la posición " + posicion + " ya existe.");
        }
        return agregar(new Persona(posPadre, nombre, nacimiento, fallecimiento));
    }

    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo nodo = nodoParaAscendiente(posicion);
        String posMadre = Posicion.posicionDeLaMadre(posicion);
        if (!Posicion.esAhnentafel(posMadre)) {
            throw new ArbolInvalidoException("No se pueden agregar más generaciones de ascendientes.");
        }
        if (nodo.madre != null) {
            throw new ArbolInvalidoException("La madre de la posición " + posicion + " ya existe.");
        }
        return agregar(new Persona(posMadre, nombre, nacimiento, fallecimiento));
    }

    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar hijos al sujeto y a sus descendientes.");
        }
        int mayor = 0;
        for (Nodo hijo : nodo.hijos) {
            mayor = Math.max(mayor, Posicion.orden(hijo.persona.getPosicion()));
        }
        String nueva = posicion + "." + (mayor + 1);
        if (!Posicion.esDAboville(nueva)) {
            throw new ArbolInvalidoException("No se puede agregar el hijo: la posición " + nueva
                    + " excede los límites permitidos.");
        }
        return agregar(new Persona(nueva, nombre, nacimiento, fallecimiento));
    }

    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto no se puede eliminar.");
        }
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        int eliminados;
        if (Posicion.esAscendiente(posicion)) {
            eliminados = contarAscendientes(nodo);
            Nodo hijo = buscarNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = null;
            } else {
                hijo.madre = null;
            }
        } else {
            eliminados = contarDescendientes(nodo);
            Nodo progenitor = buscarNodo(Posicion.posicionDelProgenitor(posicion));
            progenitor.hijos.remove(nodo);
        }
        cantidad -= eliminados;
        return eliminados;
    }

    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(raiz.persona);
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    public List<Persona> ascendientes() {
        List<Persona> resultado = new ArrayList<>();
        List<Nodo> cola = new ArrayList<>();
        cola.add(raiz);
        int i = 0;
        while (i < cola.size()) {
            Nodo actual = cola.get(i);
            i++;
            if (actual.padre != null) {
                cola.add(actual.padre);
                resultado.add(actual.padre.persona);
            }
            if (actual.madre != null) {
                cola.add(actual.madre);
                resultado.add(actual.madre.persona);
            }
        }
        return resultado;
    }

    public List<Persona> descendientes() {
        List<Persona> resultado = new ArrayList<>();
        preorden(raiz, resultado);
        return resultado;
    }

    // ---------- métodos auxiliares privados ----------

    private Nodo buscarNodo(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            return raiz;
        }
        if (Posicion.esAscendiente(posicion)) {
            String bits = Integer.toBinaryString(Integer.parseInt(posicion));
            Nodo actual = raiz;
            for (int i = 1; i < bits.length() && actual != null; i++) {
                actual = bits.charAt(i) == '0' ? actual.padre : actual.madre;
            }
            return actual;
        }
        if (Posicion.esDescendiente(posicion)) {
            String[] partes = posicion.split("\\.");
            Nodo actual = raiz;
            for (int i = 1; i < partes.length && actual != null; i++) {
                actual = hijoConOrden(actual, Integer.parseInt(partes[i]));
            }
            return actual;
        }
        return null;
    }

    private Nodo hijoConOrden(Nodo nodo, int orden) {
        for (Nodo hijo : nodo.hijos) {
            if (Posicion.orden(hijo.persona.getPosicion()) == orden) {
                return hijo;
            }
        }
        return null;
    }

    private Nodo nodoParaAscendiente(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe.");
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esAscendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar padres al sujeto y a sus ascendientes.");
        }
        return nodo;
    }

    private int contarAscendientes(Nodo nodo) {
        int total = 1;
        if (nodo.padre != null) {
            total += contarAscendientes(nodo.padre);
        }
        if (nodo.madre != null) {
            total += contarAscendientes(nodo.madre);
        }
        return total;
    }

    private int contarDescendientes(Nodo nodo) {
        int total = 1;
        for (Nodo hijo : nodo.hijos) {
            total += contarDescendientes(hijo);
        }
        return total;
    }

    private void preorden(Nodo nodo, List<Persona> lista) {
        for (Nodo hijo : nodo.hijos) {
            lista.add(hijo.persona);
            preorden(hijo, lista);
        }
    }
}