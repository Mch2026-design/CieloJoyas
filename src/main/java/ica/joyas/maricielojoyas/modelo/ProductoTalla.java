package ica.joyas.maricielojoyas.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "producto_tallas")
public class ProductoTalla {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "talla", nullable = false, length = 10)
    private String talla;

    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    public ProductoTalla() {}

    public ProductoTalla(Producto producto, String talla, Integer stock) {
        this.producto = producto;
        this.talla = talla;
        this.stock = stock;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
