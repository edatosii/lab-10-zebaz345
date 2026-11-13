package co.umariana.dao;

import co.umariana.conexion.ConexionBD;
import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.Persona;
import co.umariana.modelo.ResumenArbol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ArbolDAO {

    public long guardar(String nombre, ArbolGenealogico arbol) {
        if (nombre == null || nombre.isBlank() || nombre.length() > 100 || arbol == null) {
            return -1;
        }
        try (Connection con = ConexionBD.conectar()) {
            try {
                con.setAutoCommit(false);
                long idArbol;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO arboles (nombre) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("No se obtuvo el id del árbol");
                        }
                        idArbol = rs.getLong(1);
                    }
                }
                String sqlMiembro = "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) "
                        + "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pm = con.prepareStatement(sqlMiembro)) {
                    for (Persona p : arbol.miembros()) {
                        pm.setLong(1, idArbol);
                        pm.setString(2, p.getPosicion());
                        pm.setString(3, p.getNombre());
                        pm.setInt(4, p.getNacimiento());
                        if (p.getFallecimiento() == null) {
                            pm.setNull(5, java.sql.Types.INTEGER);
                        } else {
                            pm.setInt(5, p.getFallecimiento());
                        }
                        pm.executeUpdate();
                    }
                }
                con.commit();
                return idArbol;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return -1;
        }
    }

    public List<ResumenArbol> listar() {
        String sql = "SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total "
                + "FROM arboles a LEFT JOIN miembros m ON m.arbol_id = a.id "
                + "GROUP BY a.id, a.nombre, a.creado ORDER BY a.id";
        List<ResumenArbol> resumenes = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resumenes.add(new ResumenArbol(rs.getLong("id"), rs.getString("nombre"),
                        rs.getInt("total"), rs.getTimestamp("creado").toLocalDateTime()));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
        return resumenes;
    }

    public ArbolGenealogico cargar(long id) {
        String sql = "SELECT posicion, nombre, nacimiento, fallecimiento FROM miembros "
                + "WHERE arbol_id = ? ORDER BY id";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                ArbolGenealogico arbol = null;
                while (rs.next()) {
                    int f = rs.getInt("fallecimiento");
                    Integer fallecimiento = rs.wasNull() ? null : f;
                    Persona persona = new Persona(rs.getString("posicion"), rs.getString("nombre"),
                            rs.getInt("nacimiento"), fallecimiento);
                    if (arbol == null) {
                        arbol = new ArbolGenealogico(persona);
                    } else {
                        arbol.agregar(persona);
                    }
                }
                return arbol;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public boolean actualizar(long id, ArbolGenealogico arbol) {
        if (arbol == null) {
            return false;
        }
        try (Connection con = ConexionBD.conectar()) {
            try {
                con.setAutoCommit(false);
                try (PreparedStatement ps = con.prepareStatement("SELECT id FROM arboles WHERE id = ?")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return false;
                        }
                    }
                }
                try (PreparedStatement pd = con.prepareStatement("DELETE FROM miembros WHERE arbol_id = ?")) {
                    pd.setLong(1, id);
                    pd.executeUpdate();
                }
                String sqlMiembro = "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) "
                        + "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pm = con.prepareStatement(sqlMiembro)) {
                    for (Persona p : arbol.miembros()) {
                        pm.setLong(1, id);
                        pm.setString(2, p.getPosicion());
                        pm.setString(3, p.getNombre());
                        pm.setInt(4, p.getNacimiento());
                        if (p.getFallecimiento() == null) {
                            pm.setNull(5, java.sql.Types.INTEGER);
                        } else {
                            pm.setInt(5, p.getFallecimiento());
                        }
                        pm.executeUpdate();
                    }
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean eliminar(long id) {
        String sql = "DELETE FROM arboles WHERE id = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}