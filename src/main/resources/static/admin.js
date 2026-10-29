'use strict';
const {api,esc,money,busy}=Cielo;
const $=s=>document.querySelector(s);
const adminPage=document.body.dataset.adminPage;
let currentReport,allProducts=[],allVendors=[],allCoupons=[],allReviews=[];
const date=v=>new Intl.DateTimeFormat('es-PE',{dateStyle:'short',timeStyle:'short',timeZone:'America/Lima'}).format(new Date(v));
const message=text=>{$('#admin-message').textContent=text;};
const formData=form=>Object.fromEntries(new FormData(form));
async function loadReport(){
 const r=await api('/admin/report?days='+$('#period').value);currentReport=r;
 $('#dashboard').hidden=false;
 if($('#status-report'))renderBreakdown(r);
 if($('#metric-revenue')){
 $('#metric-revenue').textContent=money(r.summary.revenue);$('#metric-orders').textContent=r.summary.orders;$('#metric-average').textContent=money(r.summary.average);$('#metric-customers').textContent=r.summary.customers;$('#metric-period').textContent=`Últimos ${r.days} días`;
 }
 if($('#daily-chart')){
 const max=Math.max(1,...r.daily.map(x=>Number(x.revenue)));
 $('#daily-chart').innerHTML=r.daily.length?r.daily.map(x=>`<div class="day-bar"><b>${money(x.revenue)}</b><i style="height:${Math.max(4,Number(x.revenue)/max*130)}px" aria-hidden="true"></i><small>${esc(x.day.slice(5))}</small></div>`).join(''):'<p class="daily-empty">Aún no hay pagos confirmados.</p>';
 $('#daily-data').innerHTML=r.daily.length?`<table><thead><tr><th>Fecha</th><th>Ingreso</th></tr></thead><tbody>${r.daily.map(x=>`<tr><td>${esc(x.day)}</td><td>${money(x.revenue)}</td></tr>`).join('')}</tbody></table>`:'<p>Sin datos en este periodo.</p>';
 $('#popular-list').innerHTML=r.popular.length?r.popular.map(x=>`<div class="popular-row"><span>${esc(x.name)}</span><b>${x.quantity} uds.</b><small>${money(x.revenue)}</small></div>`).join(''):'<p>Aún no hay productos vendidos.</p>';
 }
 const transitions={pendiente:['pagado','cancelado'],pagado:['preparando','cancelado'],preparando:['enviado','cancelado'],enviado:['entregado'],entregado:[],cancelado:[]};
 if($('#orders-table')){
 $('#orders-table').innerHTML=r.recent.map(o=>`<tr><td>#${o.id}</td><td>${esc(o.customer)}<br><small>${esc(o.email)}</small></td><td>${esc(date(o.createdAt))}</td><td><select class="order-status" data-order-state="${esc(o.status)}" data-status="${o.id}" aria-label="Estado del pedido ${o.id}">${[o.status,...transitions[o.status]].map(s=>`<option value="${s}">${s}</option>`).join('')}</select></td><td>${esc(o.paymentMethod)}<br>${esc(o.paymentStatus)}</td><td><b>${money(o.total)}</b></td><td><button class="row-action" data-order="${o.id}" type="button">Ver detalle</button></td></tr>`).join('');$('#no-orders').hidden=r.recent.length>0;
 }
}
function renderBreakdown(report){
 const names={pendiente:'Pendientes de pago',pagado:'Pago confirmado',preparando:'En preparación',enviado:'Enviados',entregado:'Entregados',cancelado:'Cancelados'};
 const max=Math.max(1,...report.byStatus.map(x=>Number(x.count)));
 $('#status-report').innerHTML=report.byStatus.map(x=>`<div class="report-stat"><div><span>${esc(names[x.status])}</span><b>${x.count}</b></div><div class="report-track" aria-hidden="true"><i style="width:${Number(x.count)/max*100}%"></i></div></div>`).join('');
 $('#payment-report').innerHTML=report.byPayment.length?report.byPayment.map(x=>`<div class="payment-stat"><span>${esc({yape:'Yape',plin:'Plin',transferencia:'Transferencia',tarjeta:'Tarjeta'}[x.method]||x.method)}</span><b>${money(x.revenue)}</b></div>`).join(''):'<p>No hay pagos confirmados en este periodo.</p>';
}
function productOptions(){
 if($('#purchase-form'))$('#purchase-form [name=productId]').innerHTML='<option value="">Selecciona producto</option>'+allProducts.filter(p=>p.active).map(p=>`<option value="${p.id}">${esc(p.name)}</option>`).join('');
 const options=allVendors.filter(v=>v.activo).map(v=>`<option value="${v.id}">${esc(v.nombre)}</option>`).join('');
 if($('#purchase-form'))$('#purchase-form [name=vendorId]').innerHTML='<option value="">Selecciona proveedor</option>'+options;
 if($('#product-form'))$('#product-form [name=vendorId]').innerHTML='<option value="">Sin proveedor asignado</option>'+options;
}
async function loadProducts(){allProducts=await api('/admin/products');window.ProductosAdmin?.setProducts(allProducts);productOptions();}
async function loadVendors(){allVendors=await api('/admin/vendors');if($('#vendor-list'))$('#vendor-list').innerHTML=allVendors.map(v=>`<div class="management-row"><div><b>${esc(v.nombre)}</b><small>${esc(v.contacto)} · ${esc(v.material)} · ${v.activo?'Activo':'Inactivo'}</small><p>${esc(v.notas)}</p></div><button class="row-action" data-vendor="${v.id}" type="button">Editar</button></div>`).join('')||'<p>Agrega tu primer proveedor.</p>';productOptions();}
async function loadPurchases(){const rows=await api('/admin/purchases');$('#purchase-list').innerHTML=rows.map(x=>`<div class="management-row"><div><b>Compra #${x.id} · ${esc(x.product)}</b><small>${esc(x.vendor)} · ${x.quantity} unidades · ${esc(date(x.createdAt))} · ${esc(x.reference||'Sin comprobante')}</small></div><b>${money(x.total)}</b></div>`).join('')||'<p>Aún no hay compras registradas.</p>';}
async function loadCoupons(){allCoupons=await api('/admin/coupons');$('#coupon-list').innerHTML=allCoupons.map(c=>`<div class="management-row"><div><b>${esc(c.code)} · ${c.discount}%</b><small>Mínimo ${money(c.minimum)} · ${c.active?'Activo':'Inactivo'}${c.expiresAt?` · Vence ${esc(date(c.expiresAt))}`:''}</small></div><button type="button" class="row-action" data-coupon="${c.id}">Editar</button></div>`).join('')||'<p>No hay cupones.</p>';}
async function loadGuarantees(){const rows=await api('/admin/guarantees');$('#guarantee-list').innerHTML=rows.map(g=>`<article class="management-row"><div><b>Caso #${g.id} · ${esc(g.product)}</b><small>${esc(g.name)} · ${esc(g.email)} · ${esc(g.status)}</small><p>${esc(g.description)}</p>${g.evidence?`<a href="${esc(g.evidence)}" target="_blank" rel="noopener">Ver evidencia</a>`:''}<form class="claim-form" data-claim="${g.id}"><label>Estado<select name="status">${['pendiente','aprobada','rechazada','resuelta'].map(s=>`<option ${g.status===s?'selected':''}>${s}</option>`).join('')}</select></label><label>Nota de administración<textarea name="note" maxlength="3000">${esc(g.note||'')}</textarea></label><button class="button" type="submit">Guardar seguimiento</button><p role="status"></p></form></div></article>`).join('')||'<p>No hay solicitudes de garantía.</p>';}
async function loadReviews(){const rows=await api('/admin/reviews');allReviews=rows;$('#review-list').innerHTML=rows.map(r=>`<article class="management-row"><div><b>${esc(r.name)} · ${esc(r.product)}</b><p>${esc(r.body)}</p>${r.photo?`<a href="${esc(r.photo)}" target="_blank" rel="noopener" class="moderation-photo"><img src="${esc(r.photo)}" alt="Foto de la reseña de ${esc(r.name)}" loading="lazy" width="96" height="96">Ver fotografía <svg class="cielo-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M7 17 17 7M7 7h10v10"/></svg></a>`:''}<small>${r.demonstration?'Demostración · ':r.verified?'Compra verificada · ':''}${r.approved?'Publicada':'Pendiente de publicación'}</small></div><div class="review-row-actions"><button class="row-action" data-edit-review="${r.id}" type="button">Editar</button><button class="row-action" data-review="${r.id}" data-approved="${!r.approved}" type="button">${r.approved?'Ocultar':'Publicar'}</button></div></article>`).join('')||'<p>No hay reseñas para revisar.</p>';}
async function loadSegments(){if(!$('#customer-segments'))return;const rows=await api('/admin/segments');$('#customer-segments').innerHTML=rows.map(c=>`<div class="management-row"><div><b>${esc(c.email)}</b><small>${esc(c.segment)} · ${c.orders} pedidos confirmados · ${money(c.spent)}</small></div></div>`).join('')||'<p>No hay suscripciones registradas.</p>';}
async function loadAll(){
 if(['pedidos','reportes'].includes(adminPage))await loadReport();
 if(['productos','compras'].includes(adminPage)){
  if(adminPage==='productos'){
   const categories=await api('/categories');
   $('#product-form [name=categoryId]').innerHTML=categories.map(c=>`<option value="${c.id}">${esc(c.name)}</option>`).join('');
  }
  await Promise.all([loadVendors(),loadProducts()]);
  if(adminPage==='productos')$('#add-product').disabled=false;
  if(adminPage==='compras')await loadPurchases();
 }
 if(adminPage==='posventa')await Promise.all([loadGuarantees(),loadReviews()]);
 if(adminPage==='promociones')await Promise.all([loadCoupons(),loadSegments()]);
}
if($('#period')){
 try{const saved=sessionStorage.getItem('cielo-admin-period');if([...$('#period').options].some(o=>o.value===saved))$('#period').value=saved;}catch{}
}
const activeLink=$('.admin-nav [aria-current=page]'),nav=$('.admin-nav');
if(activeLink)nav.scrollLeft=activeLink.offsetLeft-nav.offsetLeft-(nav.clientWidth-activeLink.clientWidth)/2;
$('#period')?.addEventListener('change',()=>{try{sessionStorage.setItem('cielo-admin-period',$('#period').value);}catch{}loadReport().catch(handleError);});
window.CieloAdminReviews={reload:loadReviews,find:id=>allReviews.find(r=>r.id===Number(id)),message};
function handleError(error){if(error.status===401){location.replace('login.html?next=admin.html');return;}if(error.status===403){location.replace('index.html');return;}$('#report-error').textContent=error.message;}
$('#orders-table')?.addEventListener('change',async e=>{const select=e.target.closest('[data-status]');if(!select)return;const old=currentReport.recent.find(o=>o.id===Number(select.dataset.status)).status;if(['pagado','cancelado'].includes(select.value)&&!(await CieloControls.confirm(select.value==='pagado'?'¿Confirmas que verificaste el pago? Se descontarán las existencias.':'¿Cancelar este pedido? Si estaba pagado se repondrá el stock; coordina el reembolso por separado.'))){select.value=old;return;}select.disabled=true;try{await api('/admin/orders/'+select.dataset.status+'/status',{method:'PATCH',body:{status:select.value}});await loadReport();message('Estado del pedido actualizado.');}catch(error){select.value=old;handleError(error);}finally{select.disabled=false;}});
function fillForm(form,values){for(const [key,value]of Object.entries(values)){const el=form.elements.namedItem(key);if(!el)continue;if(el.type==='checkbox')el.checked=!!value;else el.value=value??'';}}
$('#vendor-list')?.addEventListener('click',e=>{const b=e.target.closest('[data-vendor]');if(!b)return;const v=allVendors.find(x=>x.id===Number(b.dataset.vendor));fillForm($('#vendor-form'),{id:v.id,name:v.nombre,contact:v.contacto,material:v.material,notes:v.notas,active:v.activo});$('#vendor-form').scrollIntoView();$('#vendor-form [name=name]').focus();});
$('#reset-vendor')?.addEventListener('click',()=>{$('#vendor-form').reset();$('#vendor-form [name=id]').value='';});
$('#coupon-list')?.addEventListener('click',e=>{const b=e.target.closest('[data-coupon]');if(!b)return;const c=allCoupons.find(x=>x.id===Number(b.dataset.coupon));const local=c.expiresAt?new Date(new Date(c.expiresAt).getTime()-new Date(c.expiresAt).getTimezoneOffset()*60000).toISOString().slice(0,16):'';fillForm($('#coupon-form'),{...c,expiresAt:local});$('#coupon-form').scrollIntoView();$('#coupon-form [name=code]').focus();});
for(const kind of ['vendor','purchase','coupon'])$('#'+kind+'-form')?.addEventListener('submit',async e=>{
 e.preventDefault();const form=e.currentTarget,body=formData(form),error=$('#'+kind+'-error');error.textContent='';
 if(kind==='product'){body.customizable=form.elements.customizable.checked;body.active=form.elements.active.checked;}
 if(kind==='vendor'||kind==='coupon')body.active=form.elements.active.checked;
 if(kind==='coupon'&&body.expiresAt)body.expiresAt=new Date(body.expiresAt).toISOString();
 await busy(form.querySelector('[type=submit]'),async()=>{try{await api('/admin/'+({product:'products',vendor:'vendors',purchase:'purchases',coupon:'coupons'}[kind]),{method:'POST',body});form.reset();if(form.elements.namedItem('id'))form.elements.id.value='';if(kind==='product')await loadProducts();if(kind==='vendor')await loadVendors();if(kind==='purchase')await Promise.all([loadPurchases(),loadProducts()]);if(kind==='coupon')await loadCoupons();message(kind==='purchase'?'Compra guardada y stock actualizado.':'Cambios guardados.');}catch(err){error.textContent=err.message;}});
});
$('#guarantee-list')?.addEventListener('submit',async e=>{const form=e.target.closest('[data-claim]');if(!form)return;e.preventDefault();await busy(form.querySelector('button'),async()=>{try{await api('/admin/guarantees/'+form.dataset.claim,{method:'PATCH',body:formData(form)});form.querySelector('[role=status]').textContent='Seguimiento guardado.';}catch(error){form.querySelector('[role=status]').textContent=error.message;}});});
$('#review-list')?.addEventListener('click',async e=>{const b=e.target.closest('[data-review]');if(!b)return;await busy(b,async()=>{try{await api('/admin/reviews/'+b.dataset.review,{method:'PATCH',body:{approved:b.dataset.approved==='true'}});await loadReviews();message('Publicación de reseña actualizada.');}catch(error){handleError(error);}});});
$('#orders-table')?.addEventListener('click',async e=>{const b=e.target.closest('[data-order]');if(!b)return;try{const o=await api('/admin/orders/'+b.dataset.order);$('#order-title').textContent=`Pedido #${o.id}`;$('#order-detail').innerHTML=`<p>${esc(o.customer)}<br>${esc(o.email)} · ${esc(o.phone)}</p><p>${esc(o.address)}, ${esc(o.district)}, ${esc(o.province)}, ${esc(o.department)}</p><p>Pago: ${esc(o.paymentStatus)} · ${esc(o.paymentMethod)}<br>Descuento: ${money(o.discount)}<br>Total de productos: ${money(o.total)}<br>Envío por cotizar.</p><ul>${o.items.map(x=>`<li>${x.quantity} × ${esc(x.name)}${x.engraving?` · Grabado: ${esc(x.engraving)}`:''}${x.size?` · Talla: ${esc(x.size)}`:''}</li>`).join('')}</ul>${o.giftMessage?`<p>Mensaje de regalo: ${esc(o.giftMessage)}</p>`:''}`;fillForm($('#edit-order-form'),o);$('#edit-order-form').hidden=['enviado','entregado','cancelado'].includes(o.status);$('#edit-order-error').textContent='';$('#order-dialog').showModal();}catch(error){handleError(error);}});
$('#close-order')?.addEventListener('click',()=>$('#order-dialog').close());
$('#edit-order-form')?.addEventListener('submit',async e=>{e.preventDefault();const form=e.currentTarget;await busy(form.querySelector('button'),async()=>{try{await api('/admin/orders/'+form.elements.id.value,{method:'PUT',body:formData(form)});$('#order-dialog').close();await loadReport();message('Datos de entrega actualizados.');}catch(error){$('#edit-order-error').textContent=error.message;}});});
$('#export-csv')?.addEventListener('click',()=>{if(!currentReport)return;const cell=v=>'"'+String(v??'').replace(/^[=+@-]/,"'$&").replace(/"/g,'""')+'"';const rows=[['Pedido','Cliente','Correo','Fecha','Estado','Pago','Total'],...currentReport.recent.map(o=>[o.id,o.customer,o.email,o.createdAt,o.status,o.paymentStatus,o.total])];const url=URL.createObjectURL(new Blob(['\ufeff'+rows.map(row=>row.map(cell).join(';')).join('\r\n')],{type:'text/csv;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download='cielo-pedidos.csv';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);});
(async()=>{const {user}=await Cielo.session();if(!user){location.replace('login.html?next=admin.html');return;}if(user.role!=='ADMIN'){location.replace('index.html');return;}$('#dashboard').hidden=false;$('#dashboard').setAttribute('aria-busy','true');try{await loadAll();}finally{$('#dashboard').removeAttribute('aria-busy');}})().catch(handleError);
