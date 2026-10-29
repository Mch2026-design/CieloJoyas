package ica.joyas.maricielojoyas.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "rol", nullable = false, length = 20)
    private String rol = "CLIENTE";

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "pedido_recordado_id")
    private Long pedidoRecordadoId;

    @ElementCollection
    @CollectionTable(name = "usuario_entrega", joinColumns = @JoinColumn(name = "usuario_id"))
    @MapKeyColumn(name = "campo", length = 30)
    @Column(name = "valor", length = 254)
    private java.util.Map<String, String> entregaGuardada = new java.util.HashMap<>();

    public java.util.Map<String, String> getEntregaGuardada() { return entregaGuardada; }

    public Usuario() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public Long getPedidoRecordadoId() { return pedidoRecordadoId; }
    public void setPedidoRecordadoId(Long id) { this.pedidoRecordadoId = id; }
}
