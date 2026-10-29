package ica.joyas.maricielojoyas.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cupones")
public class Cupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "descuento", nullable = false)
    private Integer descuento;

    @Column(name = "minimo", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimo = BigDecimal.ZERO;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "vence_en", nullable = true)
    private OffsetDateTime venceEn;

    public Cupon() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Integer getDescuento() { return descuento; }
    public void setDescuento(Integer descuento) { this.descuento = descuento; }
    public BigDecimal getMinimo() { return minimo; }
    public void setMinimo(BigDecimal minimo) { this.minimo = minimo; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public OffsetDateTime getVenceEn() { return venceEn; }
    public void setVenceEn(OffsetDateTime venceEn) { this.venceEn = venceEn; }
}
