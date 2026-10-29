package ica.joyas.maricielojoyas.controller;

import ica.joyas.maricielojoyas.modelo.*;
import ica.joyas.maricielojoyas.repositorio.*;
import ica.joyas.maricielojoyas.validacion.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.*;
import java.time.*;
import java.util.*;
import static ica.joyas.maricielojoyas.validacion.Reglas.*;

/** API única, con modelos y repositorios como en BiblioTrack. */
@RestController
@RequestMapping("/api")
public class AppController {
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ClienteRepository clientes;
    @Autowired private CategoriaRepository categorias;
    @Autowired private ProductoRepository productos;
    @Autowired private CuponRepository cupones;
    @Autowired private PedidoRepository pedidos;
    @Autowired private DetallePedidoRepository detalles;
    @Autowired private PagoRepository pagos;
    @Autowired private ProveedorRepository proveedores;
    @Autowired private CompraRepository compras;
    @Autowired private GarantiaRepository garantias;
    @Autowired private ResenaRepository resenas;
    @Autowired private PedidoService pedidosService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AccesoService acceso;
    @Autowired private Ubigeos ubigeos;
    @Autowired private AvisosPedido avisosPedido;
    @Value("${cielo.whatsapp:}") private String whatsapp;
    @Value("${cielo.payment.cci:00000000000000000000}") private String paymentCci;
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private static Map<String, Object> vista(Object... pares) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) resultado.put((String) pares[i], pares[i + 1]);
        return resultado;
    }
    private boolean confirmado(Pedido pedido) {
        return Set.of("pagado", "preparando", "enviado", "entregado").contains(pedido.getEstado());
    }
    private Map<String, Object> producto(Producto p) {
        return vista("id", p.getId(), "reference", p.getReferencia(), "name", p.getNombre(),
            "description", p.getDescripcion(), "category", p.getCategoria().getNombre(), "categoryId", p.getCategoria().getId(),
            "price", p.getPrecio(), "compareAtPrice", p.getPrecioAnterior(), "stock", p.getStock(), "tag", p.getEtiqueta(),
            "src", p.getImagenUrl(), "alt", p.getImagenAlt(), "material", p.getMaterial(), "certificateUrl", null,
            "customizable", p.getPersonalizable(), "active", p.getActivo(), "createdAt", p.getCreadoEn(),
            "vendorId", p.getProveedor() == null ? null : p.getProveedor().getId());
    }
    private Map<String, Object> pedido(Pedido p) {
        Pago pago = pagos.findByPedidoId(p.getId()).orElseThrow();
        return vista("id", p.getId(), "customer", p.getNombres() + " " + p.getApellidos(), "firstName", p.getNombres(),
            "lastName", p.getApellidos(), "email", p.getEmail(), "phone", p.getTelefono(), "address", p.getDireccion(),
            "district", p.getDistrito(), "province", p.getProvincia(), "department", p.getDepartamento(),
            "status", p.getEstado(), "createdAt", p.getCreadoEn(), "paidAt", p.getPagadoEn(), "subtotal", p.getSubtotal(),
            "discount", p.getDescuento(), "shipping", p.getCostoEnvio(), "total", p.getTotal(), "giftMessage", p.getMensajeRegalo(),
            "paymentMethod", pago.getMetodo(), "paymentStatus", pago.getEstado(), "emailSent", p.getCorreoEnviadoEn()!=null,
            "certificateAvailable", confirmado(p) && pago.getEstado().equals("confirmado"),
            "certificateUrl", confirmado(p) && pago.getEstado().equals("confirmado") ? "/api/orders/" + p.getId() + "/certificate" : null,
            "items", detalles.findByPedidoIdOrderById(p.getId()).stream().map(d -> vista("name", d.getNombreProducto(),
                "quantity", d.getCantidad(), "price", d.getPrecioUnitario(), "engraving", d.getGrabado(), "size", d.getTalla())).toList());
    }
    @GetMapping("/auth/me") public Map<String, Object> miSesion(HttpServletRequest request) { return acceso.sesion(request); }
    @PostMapping("/auth/login") public Map<String, Object> entrar(@RequestBody Map<String, Object> datos, HttpServletRequest request) {
        return acceso.entrar(acceso.autenticar(datos, request.getSession()), request);
    }
    @PostMapping("/auth/register") public Map<String, Object> registrar(@RequestBody Map<String, Object> datos, HttpServletRequest request) {
        return acceso.entrar(acceso.registrar(datos, false), request);
    }
    @PostMapping("/auth/setup") public Map<String, Object> configurar(@RequestBody Map<String, Object> datos, HttpServletRequest request) {
        if (!acceso.esLocal(request)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Configura la administración desde el equipo de la tienda.");
        return acceso.entrar(acceso.registrar(datos, true), request);
    }
    @PostMapping("/auth/logout") public Map<String, Object> salir(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false); if (sesion != null) sesion.invalidate();
        return Map.of("ok", true);
    }
    @GetMapping("/account/orders") public List<Map<String, Object>> misPedidos(HttpServletRequest request) {
        Usuario usuario = acceso.requerido(request.getSession(false));
        return pedidos.findByUsuarioIdOrderByIdDesc(usuario.getId()).stream().map(this::pedido).toList();
    }
    @GetMapping("/account/delivery") @Transactional(readOnly=true) public Map<String, Object> entregaRecordada(HttpServletRequest request) {
        Usuario u = acceso.requerido(request.getSession(false));
        if (!u.getEntregaGuardada().isEmpty()) { Map<String,Object> resultado=new LinkedHashMap<>(u.getEntregaGuardada()); resultado.put("saved",true); return resultado; }
        Pedido p = u.getPedidoRecordadoId() == null ? null : pedidos.findById(u.getPedidoRecordadoId()).orElse(null);
        if (p == null || p.getUsuario() == null || !p.getUsuario().getId().equals(u.getId())) return Map.of("saved", false);
        return vista("saved", true, "firstName", p.getNombres(), "lastName", p.getApellidos(), "documentType", p.getTipoDocumento(),
            "documentId", p.getNumeroDocumento(), "address", p.getDireccion(), "reference", p.getReferencia(), "department", p.getDepartamento(),
            "province", p.getProvincia(), "district", p.getUbigeoCodigo(), "postal", p.getCodigoPostal(), "phone", p.getTelefono());
    }
    @PutMapping("/account/delivery") @Transactional
    public Map<String,Object> guardarEntrega(@RequestBody Map<String,Object> datos, HttpServletRequest request) {
        Usuario u=acceso.requerido(request.getSession(false));
        Map<String,String> entrega=new LinkedHashMap<>();
        entrega.put("firstName",texto(datos,"firstName",2,50)); entrega.put("lastName",texto(datos,"lastName",2,50));
        String tipo=opcion(datos,"documentType",Set.of("dni","ce","ruc")), documento=texto(datos,"documentId",8,12);
        String patron=switch(tipo) { case "dni" -> "[0-9]{8}"; case "ruc" -> "(?:10|15|17|20)[0-9]{9}"; default -> "[A-Za-z0-9]{9,12}"; };
        if(!documento.matches(patron)) throw error("Revisa el número de documento.");
        entrega.put("documentType",tipo); entrega.put("documentId",documento);
        entrega.put("address",texto(datos,"address",5,100)); entrega.put("reference",texto(datos,"reference",0,100));
        String[] localidad=ubigeos.buscar(texto(datos,"district",6,6));
        entrega.put("department",localidad[1]); entrega.put("province",localidad[2]); entrega.put("district",localidad[0]);
        String postal=texto(datos,"postal",0,5), telefono=texto(datos,"phone",9,9);
        if(!postal.isBlank()&&!postal.matches("[0-9]{5}")) throw error("El código postal debe tener 5 dígitos.");
        if(!telefono.matches("9[0-9]{8}")) throw error("Ingresa un celular de 9 dígitos que empiece por 9.");
        entrega.put("postal",postal); entrega.put("phone",telefono);
        u.getEntregaGuardada().clear(); u.getEntregaGuardada().putAll(entrega); usuarios.save(u);
        return Map.of("saved",true);
    }
    @DeleteMapping("/account/delivery") @Transactional public Map<String,Object> olvidarEntrega(HttpServletRequest request) {
        Usuario u = acceso.requerido(request.getSession(false)); u.setPedidoRecordadoId(null); u.getEntregaGuardada().clear(); usuarios.save(u); return Map.of("ok", true);
    }

    @GetMapping("/health") public Map<String, Object> salud() {
        return Map.of("ok", true, "database", jdbc.queryForObject("select current_database()", String.class));
    }
    @GetMapping("/config") public Map<String, Object> configuracion() {
        String cci = paymentCci.matches("[0-9]{20}") ? paymentCci : "00000000000000000000";
        return vista("whatsapp", whatsapp.replaceAll("[^0-9]", ""), "payment", vista(
            "qrUrl", "/assets/pago-yape-plin.png", "cci", cci, "holder", "MARICIELO GIRAO CHACALIAZA",
            "demonstration", cci.equals("00000000000000000000"), "confirmationBusinessDays", 5));
    }
    @GetMapping("/categories") public List<Map<String, Object>> categorias() {
        return categorias.findAll().stream().filter(Categoria::getActiva).map(c -> vista("id", c.getId(), "name", c.getNombre())).toList();
    }
    @GetMapping("/products") public List<Map<String, Object>> catalogo() {
        Map<Long, Integer> ventas = new HashMap<>();
        for (DetallePedido d : detalles.findAll()) if (confirmado(d.getPedido()))
            ventas.merge(d.getProducto().getId(), d.getCantidad(), Integer::sum);
        return productos.findByActivoTrueOrderByIdDesc().stream().map(p -> {
            var fila = producto(p); fila.put("salesCount", ventas.getOrDefault(p.getId(), 0)); return fila;
        }).toList();
    }
    @PostMapping("/coupons/validate") public Map<String, Object> validarCupon(@RequestBody Map<String, Object> datos) {
        Cupon c = pedidosService.validarCupon(texto(datos, "code", 3, 20), dinero(datos.get("subtotal")));
        return vista("code", c.getCodigo(), "discount", c.getDescuento(), "minimum", c.getMinimo());
    }
    @PostMapping("/orders") public Map<String, Object> comprar(@RequestBody Map<String, Object> datos, HttpServletRequest request) {
        Usuario usuario = acceso.actual(request.getSession(false));
        Map<String, Object> compra = new HashMap<>(datos);
        if (usuario != null) compra.put("email", usuario.getEmail());
        Map<String, Object> resultado = pedidosService.crear(compra, usuario);
        Long pedidoId = ((Number) resultado.get("id")).longValue();
        HttpSession sesion = request.getSession();
        synchronized (sesion) {
            Set<Long> permitidos = new LinkedHashSet<>();
            if (sesion.getAttribute("pedidosSesion") instanceof Set<?> previos)
                for (Object previo : previos) if (previo instanceof Long numero) permitidos.add(numero);
            permitidos.add(pedidoId);
            while (permitidos.size() > 100) permitidos.remove(permitidos.iterator().next());
            sesion.setAttribute("pedidosSesion", permitidos);
        }
        String correo=avisosPedido.enviar(pedidoId);
        Map<String,Object> respuesta=pedido(pedidos.findById(pedidoId).orElseThrow());
        respuesta.put("emailStatus",correo);
        return respuesta;
    }
    private Pedido pedidoPermitido(Long id, HttpServletRequest request) {
        Pedido p = pedidos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado."));
        HttpSession sesion = request.getSession(false);
        Usuario usuario = acceso.actual(sesion);
        boolean propietario = usuario != null && (usuario.getRol().equals("ADMIN") ||
            (p.getUsuario() != null && p.getUsuario().getId().equals(usuario.getId())));
        boolean compraDeSesion = sesion != null && sesion.getAttribute("pedidosSesion") instanceof Set<?> ids && ids.contains(id);
        if (!propietario && !compraDeSesion) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado.");
        return p;
    }
    @GetMapping("/orders/{id}") public Map<String, Object> consultarPedido(@PathVariable Long id, HttpServletRequest request) {
        return pedido(pedidoPermitido(id, request));
    }
    @GetMapping("/orders/{id}/certificate") public ResponseEntity<byte[]> certificado(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean preview, HttpServletRequest request) throws IOException {
        Pedido p = pedidoPermitido(id, request);
        if (!confirmado(p) || !pagos.findByPedidoId(id).orElseThrow().getEstado().equals("confirmado"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El certificado estará disponible cuando confirmemos tu pago.");
        String extension = preview ? "png" : "pdf";
        byte[] contenido;
        try (var stream = new ClassPathResource("static/assets/certificado-material." + extension).getInputStream()) { contenido = stream.readAllBytes(); }
        return ResponseEntity.ok().contentType(preview ? MediaType.IMAGE_PNG : MediaType.APPLICATION_PDF)
            .header("Content-Disposition", (preview ? "inline" : "attachment") + "; filename=Certificado-Cielo-pedido-" + id + "." + extension)
            .body(contenido);
    }
    @PostMapping("/newsletter") @Transactional public Map<String, Object> suscribir(@RequestBody Map<String, Object> datos) {
        clientes.registrarSiFalta("", "", email(datos), null, true);
        return Map.of("message", "Tu suscripción quedó registrada. Gracias por acompañarnos.");
    }
    @PostMapping("/guarantees") @Transactional public Map<String, Object> garantia(@RequestBody Map<String, Object> datos) {
        Garantia g = new Garantia(); g.setEmail(email(datos));
        g.setNombreCliente(texto(datos, "name", 2, 150)); g.setNombreProducto(texto(datos, "product", 2, 150));
        g.setDescripcion(texto(datos, "description", 10, 4000)); g.setEvidenciaUrl(url(datos, "evidence", false));
        if (datos.get("orderId") != null && !String.valueOf(datos.get("orderId")).isBlank()) {
            Pedido p = pedidos.findById(id(datos.get("orderId"))).orElseThrow(() -> error("Revisa el pedido y el correo de compra."));
            if (!p.getEmail().equals(g.getEmail())) throw error("Revisa el pedido y el correo de compra.");
            g.setPedido(p);
        }
        garantias.save(g); return Map.of("caseId", g.getId(), "message", "Recibimos tu solicitud de garantía.");
    }
    @GetMapping("/admin/products") public List<Map<String, Object>> inventario() { return productos.findAll().stream().map(this::producto).toList(); }
    @PostMapping("/admin/products") @Transactional public Map<String, Object> guardarProducto(@RequestBody Map<String, Object> datos) {
        Producto p = datos.get("id") == null || String.valueOf(datos.get("id")).isBlank() ? new Producto()
            : productos.bloquearPorId(id(datos.get("id"))).orElseThrow(() -> error("Producto no encontrado."));
        p.setReferencia(texto(datos, "reference", 2, 30)); p.setNombre(texto(datos, "name", 2, 150));
        p.setDescripcion(texto(datos, "description", 0, 3000));
        p.setCategoria(categorias.findById(id(datos.get("categoryId"))).orElseThrow(() -> error("Selecciona una categoría.")));
        p.setPrecio(dinero(datos.get("price"))); if (p.getPrecio().signum() == 0) throw error("El precio debe ser mayor que cero.");
        p.setPrecioAnterior(datos.get("compareAtPrice") == null || String.valueOf(datos.get("compareAtPrice")).isBlank() ? null : dinero(datos.get("compareAtPrice")));
        if (p.getPrecioAnterior() != null && p.getPrecioAnterior().compareTo(p.getPrecio()) <= 0) throw error("El precio anterior debe superar al actual.");
        p.setStock(entero(datos, "stock", 0, 1000000)); p.setEtiqueta(nulo(texto(datos, "tag", 0, 60)));
        p.setImagenUrl(url(datos, "src", true)); p.setImagenAlt(texto(datos, "alt", 2, 254));
        p.setMaterial(texto(datos, "material", 2, 150)); p.setCertificadoUrl("/assets/certificado-material.pdf");
        p.setPersonalizable(bandera(datos, "customizable"));
        if (datos.containsKey("active")) p.setActivo(bandera(datos, "active"));
        p.setProveedor(datos.get("vendorId") == null || String.valueOf(datos.get("vendorId")).isBlank() ? null
            : proveedores.findById(id(datos.get("vendorId"))).orElseThrow(() -> error("Proveedor no encontrado.")));
        return producto(productos.save(p));
    }
    @DeleteMapping("/admin/products/{id}") @Transactional public Map<String, Object> bajaProducto(@PathVariable Long id) {
        Producto p = productos.bloquearPorId(id).orElseThrow(() -> error("Producto no encontrado."));
        p.setActivo(false); productos.save(p); return Map.of("ok", true);
    }
    @GetMapping("/admin/orders/{id}") public Map<String, Object> verPedido(@PathVariable Long id) { return pedido(pedidos.findById(id).orElseThrow(() -> error("Pedido no encontrado."))); }
    @PatchMapping("/admin/orders/{id}/status") public Map<String, Object> estado(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        return pedido(pedidosService.cambiarEstado(id, texto(datos, "status", 1, 20)));
    }
    @PutMapping("/admin/orders/{id}") public Map<String, Object> editarPedido(@PathVariable Long id, @RequestBody Map<String, Object> datos) { return pedido(pedidosService.editar(id, datos)); }
    @GetMapping("/admin/coupons") public List<Map<String, Object>> cupones() { return cupones.findAll().stream().map(c -> vista("id", c.getId(), "code", c.getCodigo(), "discount", c.getDescuento(), "minimum", c.getMinimo(), "active", c.getActivo(), "expiresAt", c.getVenceEn())).toList(); }
    @PostMapping("/admin/coupons") @Transactional public Map<String, Object> guardarCupon(@RequestBody Map<String, Object> datos) {
        String codigo = texto(datos, "code", 3, 20).toUpperCase(Locale.ROOT);
        if (!codigo.matches("[A-Z0-9_-]{3,20}")) throw error("Usa letras, números, guion o guion bajo en el cupón.");
        Cupon c = cupones.findByCodigo(codigo).orElseGet(Cupon::new); c.setCodigo(codigo);
        c.setDescuento(entero(datos, "discount", 1, 80)); c.setMinimo(dinero(datos.getOrDefault("minimum", 0)));
        c.setActivo(!datos.containsKey("active") || bandera(datos, "active"));
        String fecha = texto(datos, "expiresAt", 0, 40); c.setVenceEn(fecha.isBlank() ? null : OffsetDateTime.parse(fecha));
        cupones.save(c); return Map.of("id", c.getId());
    }
    @GetMapping("/admin/vendors") public List<Proveedor> proveedores() { return proveedores.findAll(); }
    @PostMapping("/admin/vendors") @Transactional public Proveedor guardarProveedor(@RequestBody Map<String, Object> datos) {
        Proveedor p = datos.get("id") == null || String.valueOf(datos.get("id")).isBlank() ? new Proveedor()
            : proveedores.findById(id(datos.get("id"))).orElseThrow(() -> error("Proveedor no encontrado."));
        p.setNombre(texto(datos, "name", 2, 150)); p.setContacto(texto(datos, "contact", 0, 254));
        p.setMaterial(texto(datos, "material", 0, 150)); p.setNotas(texto(datos, "notes", 0, 3000));
        p.setActivo(!datos.containsKey("active") || bandera(datos, "active")); return proveedores.save(p);
    }
    @GetMapping("/admin/purchases") public List<Map<String, Object>> compras() { return compras.findAllByOrderByIdDesc().stream().map(c -> vista("id", c.getId(), "vendor", c.getProveedor().getNombre(), "product", c.getProducto().getNombre(), "quantity", c.getCantidad(), "total", c.getTotal(), "reference", c.getComprobante(), "createdAt", c.getCreadoEn())).toList(); }
    @PostMapping("/admin/purchases") public Map<String, Object> compra(@RequestBody Map<String, Object> datos) { return Map.of("id", pedidosService.registrarCompra(datos).getId(), "message", "Compra registrada y existencias actualizadas."); }
    @GetMapping("/admin/guarantees") public List<Map<String, Object>> garantias() { return garantias.findAllByOrderByIdDesc().stream().map(g -> vista("id", g.getId(), "name", g.getNombreCliente(), "email", g.getEmail(), "product", g.getNombreProducto(), "description", g.getDescripcion(), "status", g.getEstado(), "note", g.getNotaAdmin(), "evidence", g.getEvidenciaUrl())).toList(); }
    @PatchMapping("/admin/guarantees/{id}") @Transactional public Map<String, Object> resolver(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        Garantia g = garantias.findById(id).orElseThrow(() -> error("Garantía no encontrada."));
        g.setEstado(opcion(datos, "status", Set.of("pendiente", "aprobada", "rechazada", "resuelta")));
        g.setNotaAdmin(texto(datos, "note", 0, 3000)); garantias.save(g); return Map.of("ok", true);
    }
    @GetMapping("/admin/reviews") public List<Map<String, Object>> moderacion() { return resenas.findAll().stream().map(r -> vista("id", r.getId(), "name", r.getNombreCliente(), "email", r.getEmail(), "product", r.getProducto().getNombre(), "body", r.getContenido(), "photo", r.getFotoUrl(), "approved", r.getAprobada(), "demonstration", r.getDemostracion(), "verified", !r.getDemostracion() && r.getVerificada())).toList(); }
    @PatchMapping("/admin/reviews/{id}") @Transactional public Map<String, Object> publicar(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
        Resena r = resenas.bloquearPorId(id).orElseThrow(() -> error("Reseña no encontrada."));
        boolean aprobada = bandera(datos, "approved");
        if (aprobada && !r.getDemostracion() && (!confirmado(r.getPedido()) || !pagos.findByPedidoId(r.getPedido().getId()).orElseThrow().getEstado().equals("confirmado"))) throw error("El pedido ya no tiene un pago confirmado.");
        r.setAprobada(aprobada); resenas.save(r); return Map.of("ok", true);
    }
    @GetMapping("/admin/segments") public List<Map<String, Object>> segmentos() {
        List<Pedido> ventas = pedidos.findAll().stream().filter(this::confirmado).toList();
        return clientes.findAll().stream().filter(Cliente::getSuscrito).map(c -> {
            List<Pedido> comprasCliente = ventas.stream().filter(p -> p.getCliente().getId().equals(c.getId())).toList();
            BigDecimal gastado = comprasCliente.stream().map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            return vista("email", c.getEmail(), "orders", comprasCliente.size(), "spent", gastado,
                "segment", comprasCliente.isEmpty() ? "Suscrita" : comprasCliente.size() >= 3 ? "Recurrente" : "Nueva compradora");
        }).toList();
    }
    @GetMapping("/admin/report") public Map<String, Object> reporte(@RequestParam(defaultValue = "30") int days) {
        if (days < 1 || days > 365) throw error("El periodo debe estar entre 1 y 365 días.");
        OffsetDateTime desde = LocalDate.now(LIMA).minusDays(days - 1L).atStartOfDay(LIMA).toOffsetDateTime();
        List<Pedido> recientes = pedidos.findByCreadoEnAfterOrderByIdDesc(desde);
        List<Pedido> ventas = pedidos.findAll().stream().filter(this::confirmado)
            .filter(p -> p.getPagadoEn() != null && !p.getPagadoEn().isBefore(desde)).toList();
        BigDecimal ingreso = ventas.stream().map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> diario = new TreeMap<>(); Map<Long, Map<String, Object>> popular = new HashMap<>();
        for (Pedido p : ventas) {
            String dia = p.getPagadoEn().atZoneSameInstant(LIMA).toLocalDate().toString(); diario.merge(dia, p.getTotal(), BigDecimal::add);
            for (DetallePedido d : detalles.findByPedidoIdOrderById(p.getId())) {
                var fila = popular.computeIfAbsent(d.getProducto().getId(), k -> vista("name", d.getNombreProducto(), "quantity", 0, "revenue", BigDecimal.ZERO));
                fila.put("quantity", (Integer) fila.get("quantity") + d.getCantidad());
                // Importe de productos antes de descuentos; ingreso usa total final confirmado.
                fila.put("revenue", ((BigDecimal) fila.get("revenue")).add(d.getImporte()));
            }
        }
        List<Map<String, Object>> estados = new ArrayList<>();
        for (String estado : List.of("pendiente", "pagado", "preparando", "enviado", "entregado", "cancelado"))
            estados.add(vista("status", estado, "count", recientes.stream().filter(p -> p.getEstado().equals(estado)).count()));
        Map<String, BigDecimal> medios = new TreeMap<>();
        for (Pedido venta : ventas) {
            Pago pago = pagos.findByPedidoId(venta.getId()).orElseThrow();
            medios.merge(pago.getMetodo(), venta.getTotal(), BigDecimal::add);
        }
        return vista("days", days, "byStatus", estados, "byPayment", medios.entrySet().stream().map(e -> vista("method", e.getKey(), "revenue", e.getValue())).toList(),
            "summary", vista("revenue", ingreso, "orders", recientes.size(), "customers", recientes.stream().map(Pedido::getEmail).distinct().count(),
            "average", ventas.isEmpty() ? BigDecimal.ZERO : ingreso.divide(BigDecimal.valueOf(ventas.size()), 2, RoundingMode.HALF_UP)),
            "daily", diario.entrySet().stream().map(e -> vista("day", e.getKey(), "revenue", e.getValue())).toList(),
            "popular", popular.values().stream().sorted(Comparator.comparingInt(m -> -(Integer) m.get("quantity"))).limit(8).toList(),
            "recent", recientes.stream().map(this::pedido).toList());
    }
}
