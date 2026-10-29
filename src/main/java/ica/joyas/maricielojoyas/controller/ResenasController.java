package ica.joyas.maricielojoyas.controller;

import ica.joyas.maricielojoyas.modelo.*;
import ica.joyas.maricielojoyas.repositorio.*;
import ica.joyas.maricielojoyas.validacion.AccesoService;
import ica.joyas.maricielojoyas.validacion.FotosResenas;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static ica.joyas.maricielojoyas.validacion.Reglas.*;

@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class ResenasController {
    private final ResenaRepository resenas;
    private final PedidoRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final PagoRepository pagos;
    private final AccesoService acceso;
    private final FotosResenas fotos;
    public ResenasController(ResenaRepository resenas, PedidoRepository pedidos, DetallePedidoRepository detalles,
            PagoRepository pagos, AccesoService acceso, FotosResenas fotos) {
        this.resenas=resenas; this.pedidos=pedidos; this.detalles=detalles; this.pagos=pagos; this.acceso=acceso; this.fotos=fotos;
    }
    private boolean confirmado(Pedido p) {
        return p != null && Set.of("pagado", "preparando", "enviado", "entregado").contains(p.getEstado())
            && pagos.findByPedidoId(p.getId()).map(x -> x.getEstado().equals("confirmado")).orElse(false);
    }
    private boolean publica(Resena r) {
        return r.getAprobada() && (r.getDemostracion() || (r.getVerificada() && confirmado(r.getPedido())));
    }
    private Usuario cliente(HttpServletRequest request) {
        Usuario u = acceso.requerido(request.getSession(false));
        if (!u.getRol().equals("CLIENTE")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Las reseñas se comparten desde una cuenta de cliente.");
        return u;
    }
    @GetMapping("/reviews")
    public List<Map<String, Object>> publicadas(@RequestParam(required=false) Long productId) {
        return resenas.findByAprobadaTrueOrderByIdDesc().stream()
            .filter(r -> (productId == null || productId.equals(r.getProducto().getId())) && publica(r)).map(r -> {
                Producto p=r.getProducto();
                Map<String, Object> d=new LinkedHashMap<>();
                d.put("id", r.getId()); d.put("name", r.getNombreCliente()); d.put("body", r.getContenido()); d.put("photo", r.getFotoUrl());
                d.put("verified", !r.getDemostracion() && r.getVerificada()); d.put("demonstration", r.getDemostracion()); d.put("createdAt", r.getCreadoEn());
                d.put("product", Map.of("id", p.getId(), "name", p.getNombre(), "src", p.getImagenUrl(), "category", p.getCategoria().getNombre(), "active", p.getActivo()));
                return d;
            }).toList();
    }
    @GetMapping("/account/reviewable-products")
    public List<Map<String, Object>> piezas(HttpServletRequest request) {
        Usuario u=cliente(request);
        List<Map<String, Object>> resultado=new ArrayList<>();
        for (Pedido p:pedidos.findByUsuarioIdOrderByIdDesc(u.getId())) {
            if (!confirmado(p)) continue;
            Set<Long> vistos=new HashSet<>();
            for (DetallePedido d:detalles.findByPedidoIdOrderById(p.getId())) {
                Producto producto=d.getProducto();
                if (!vistos.add(producto.getId()) || resenas.existsByProductoIdAndPedidoId(producto.getId(),p.getId())) continue;
                resultado.add(Map.of("orderId",p.getId(),"productId",producto.getId(),"name",d.getNombreProducto(),"src",producto.getImagenUrl(),"purchasedAt",p.getCreadoEn()));
            }
        }
        return resultado;
    }
    @PostMapping(value="/account/reviews", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String,Object> enviar(@RequestParam Long productId, @RequestParam Long orderId,
            @RequestParam String body, @RequestParam MultipartFile photo, HttpServletRequest request) {
        Usuario u=cliente(request);
        // El bloqueo también serializa dos envíos simultáneos para la misma compra.
        Pedido p=pedidos.bloquearPorId(orderId).orElseThrow(() -> error("No encontramos una compra válida para esta pieza."));
        if (p.getUsuario()==null || !p.getUsuario().getId().equals(u.getId()) || !confirmado(p))
            throw error("Selecciona una pieza de tus pedidos con pago confirmado.");
        var detalle=detalles.findByPedidoIdOrderById(orderId).stream().filter(d -> d.getProducto().getId().equals(productId)).findFirst()
            .orElseThrow(() -> error("La pieza no pertenece a este pedido."));
        if (resenas.existsByProductoIdAndPedidoId(productId,orderId)) throw error("Ya enviaste una reseña para esta pieza y pedido.");
        String contenido=texto(Map.of("reseña",body),"reseña",10,3000);
        String archivo=fotos.guardar(photo);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) fotos.eliminar(archivo); }
        });
        Resena r=new Resena(); r.setProducto(detalle.getProducto()); r.setPedido(p); r.setNombreCliente(u.getNombre()); r.setEmail(u.getEmail());
        r.setContenido(contenido); r.setFotoUrl("/api/review-photos/"+archivo); r.setVerificada(true); r.setDemostracion(false); r.setAprobada(false);
        resenas.saveAndFlush(r);
        return Map.of("id",r.getId(),"message","Gracias por compartir tu historia. Verificamos tu compra; tu reseña y foto se publicarán después de revisarlas.");
    }
    @PutMapping(value="/admin/reviews/{id}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String,Object> editar(@PathVariable Long id, @RequestParam String body,
            @RequestParam(required=false) MultipartFile photo, @RequestParam(required=false) String photoUrl, HttpServletRequest request) {
        Usuario u=acceso.requerido(request.getSession(false));
        if (!u.getRol().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta edición corresponde a administración.");
        Resena r=resenas.bloquearPorId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Reseña no encontrada."));
        String contenido=texto(Map.of("reseña",body),"reseña",10,3000);
        if (photo!=null && photoUrl!=null && !photoUrl.isBlank()) throw error("Elige un archivo o una URL de imagen.");
        if (photoUrl!=null && !photoUrl.isBlank()) {
            String nueva=url(Map.of("photoUrl",photoUrl),"photoUrl",true), anterior=r.getFotoUrl();
            r.setFotoUrl(nueva);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    if(anterior!=null && anterior.startsWith("/api/review-photos/")) fotos.eliminar(anterior.substring("/api/review-photos/".length()));
                }
            });
        }
        if (photo!=null) {
            String anterior=r.getFotoUrl(), archivo=fotos.guardar(photo);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if(status!=STATUS_COMMITTED) fotos.eliminar(archivo);
                    else if(anterior!=null && anterior.startsWith("/api/review-photos/"))
                        fotos.eliminar(anterior.substring("/api/review-photos/".length()));
                }
            });
            r.setFotoUrl("/api/review-photos/"+archivo);
        }
        r.setContenido(contenido); resenas.saveAndFlush(r);
        return Map.of("id",r.getId(),"message","Reseña actualizada.");
    }
    @GetMapping("/review-photos/{nombre}")
    public ResponseEntity<?> fotografia(@PathVariable String nombre, HttpServletRequest request) {
        var ruta=fotos.ruta(nombre);
        Resena r=resenas.findFirstByFotoUrl("/api/review-photos/"+nombre).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Usuario u=acceso.actual(request.getSession(false));
        boolean propio=u!=null && (u.getRol().equals("ADMIN") || (r.getPedido()!=null && r.getPedido().getUsuario()!=null && r.getPedido().getUsuario().getId().equals(u.getId())));
        var archivo=new FileSystemResource(ruta);
        if ((!publica(r) && !propio) || !archivo.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Fotografía no encontrada.");
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore()).body(archivo);
    }
}
