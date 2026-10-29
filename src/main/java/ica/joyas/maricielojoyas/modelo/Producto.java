package ica.joyas.maricielojoyas.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "productos")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "proveedor_id", nullable = true)
    private Proveedor proveedor;

    @Column(name = "referencia", nullable = false, length = 30)
    private String referencia;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", nullable = true, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "precio_anterior", nullable = true, precision = 12, scale = 2)
    private BigDecimal precioAnterior;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "etiqueta", nullable = true, length = 60)
    private String etiqueta;

    @Column(name = "imagen_url", nullable = false, columnDefinition = "TEXT")
    private String imagenUrl;

    @Column(name = "imagen_alt", nullable = false, length = 254)
    private String imagenAlt;

    @Column(name = "material", nullable = false, length = 150)
    private String material = "Acero quirúrgico 316L";

    @Column(name = "certificado_url", nullable = true, columnDefinition = "TEXT")
    private String certificadoUrl;

    @Column(name = "personalizable", nullable = false)
    private Boolean personalizable = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public Producto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public BigDecimal getPrecioAnterior() { return precioAnterior; }
    public void setPrecioAnterior(BigDecimal precioAnterior) { this.precioAnterior = precioAnterior; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public String getImagenAlt() { return imagenAlt; }
    public void setImagenAlt(String imagenAlt) { this.imagenAlt = imagenAlt; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getCertificadoUrl() { return certificadoUrl; }
    public void setCertificadoUrl(String certificadoUrl) { this.certificadoUrl = certificadoUrl; }
    public Boolean getPersonalizable() { return personalizable; }
    public void setPersonalizable(Boolean personalizable) { this.personalizable = personalizable; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(OffsetDateTime creadoEn) { this.creadoEn = creadoEn; }
}
