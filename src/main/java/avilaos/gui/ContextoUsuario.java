package avilaos.gui;

import avilaos.modelo.ModoUsuario;

/** Modo de uso activo (administrador / usuario) y usuario actual; lo comparten todas las pestañas. */
public class ContextoUsuario {
    private volatile ModoUsuario modo = ModoUsuario.ADMINISTRADOR;
    private volatile String usuario = "user1";

    public ModoUsuario getModo() { return modo; }
    public void setModo(ModoUsuario m) { this.modo = m; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String u) { this.usuario = u; }
    /** Identidad con la que se crean los procesos de E/S. */
    public String identidad() { return modo == ModoUsuario.ADMINISTRADOR ? "admin" : usuario; }
}
