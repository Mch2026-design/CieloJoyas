'use strict';
window.CieloPedidos=(()=>{
  const {api,esc,money,busy}=Cielo;
  const statuses={pendiente:'Pendiente de pago',pagado:'Pago confirmado',preparando:'En preparación',enviado:'En camino',entregado:'Entregado',cancelado:'Cancelado'};
  let detailView='items',itemPage=0;
  const desktop=()=>matchMedia('(min-width:901px)').matches;
  let order=null,received=false,previousFocus=null,previousOverflow='',timer=null,refreshing=false,configuration;
  let payment={qrUrl:'assets/pago-yape-plin.png',cci:'00000000000000000000',holder:'MARICIELO GIRAO CHACALIAZA',demonstration:true,confirmationBusinessDays:5};
  const dialog=document.createElement('dialog');dialog.id='pedido-dialog';dialog.className='pedido-dialog';dialog.setAttribute('aria-labelledby','pedido-title');
  dialog.innerHTML='<div class="pedido-dialog-header"><div><span class="eyebrow">TU PEDIDO CIELO</span><h2 id="pedido-title"></h2><p id="pedido-reference"></p><p id="pedido-email-note" class="pedido-email-note" role="status" hidden></p></div><button class="icon-btn" id="pedido-close" type="button" aria-label="Cerrar detalle del pedido"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18"/></svg></button></div><div id="pedido-content" class="pedido-dialog-body"></div><div class="pedido-dialog-footer"><p id="pedido-live" role="status" aria-live="polite"></p><button class="export-button" id="pedido-refresh" type="button">Actualizar estado</button><button class="button" id="pedido-done" type="button">Seguir comprando</button></div>';
  document.body.append(dialog);
  const el=id=>dialog.querySelector('#'+id);
  function paymentHTML(){
    const paid=order.certificateAvailable,cancelled=order.status==='cancelado';
    if(cancelled)return '<section class="pedido-payment"><h3>Pedido cancelado</h3><p>Este pedido ya no requiere un pago.</p></section>';
    const note=paid?'<p class="payment-confirmed-note">Tu pago está confirmado. No necesitas volver a realizarlo.</p>':`<p class="payment-deadline">La confirmación de tu pago puede tardar hasta ${payment.confirmationBusinessDays} días hábiles después de realizarlo. Cuando validemos el abono, actualizaremos el estado y habilitaremos tu certificado.</p>`;
    if(order.paymentMethod==='transferencia'){
      const cci=payment.cci.match(/.{1,4}/g)?.join(' ')||payment.cci;
      return `<section class="pedido-payment bank-payment"><span class="payment-method-label">Transferencia bancaria</span><h3>Datos para tu transferencia</h3><p class="payment-amount">${money(order.total)}</p><div class="bank-account"><span>Titular de la cuenta</span><strong>${esc(payment.holder)}</strong><span>${payment.demonstration?'CCI de demostración':'Código de cuenta interbancario (CCI)'}</span><p class="bank-cci">${esc(cci)}</p><button class="copy-cci" type="button">Copiar CCI</button><p id="copy-cci-status" role="status"></p></div>${payment.demonstration?'<p class="payment-demo-note">Datos de demostración. Solicita a la tienda el CCI real antes de transferir.</p>':''}${note}</section>`;
    }
    const method=order.paymentMethod==='plin'?'Plin':'Yape';
    return `<section class="pedido-payment qr-payment"><span class="payment-method-label">Pago por ${method}</span><h3>${paid?'Tu medio de pago':'Tu elección está a un paso'}</h3><p class="payment-amount">${money(order.total)}</p><p class="qr-instruction">${paid?'Este es el QR asociado al medio de pago de tu pedido.':`Escanea el QR desde tu app para pagar por ${method}. Verifica el destinatario antes de confirmar.`}</p><div class="payment-qr-frame"><img src="${esc(payment.qrUrl)}" alt="QR proporcionado por la tienda para el pago con Yape o Plin" width="443" height="450"></div><a class="download-qr" href="${esc(payment.qrUrl)}" download="QR-pago-Cielo.png">Guardar QR</a>${note}</section>`;
  }
  function render(){
    el('pedido-title').textContent=received?'¡Pedido confirmado!':'Detalle de tu pedido';
    el('pedido-reference').textContent=`Pedido #${order.id} · ${new Intl.DateTimeFormat('es-PE',{dateStyle:'long',timeZone:'America/Lima'}).format(new Date(order.createdAt))}`;
    el('pedido-email-note').hidden=!received;
    el('pedido-email-note').textContent=order.emailSent?'Enviamos el aviso de tu pedido a '+order.email+'.':order.emailStatus==='failed'?'Tu pedido está registrado. No pudimos enviar el correo; conserva tu número de pedido.':'Tu pedido está registrado. Conserva tu número de pedido y consulta aquí su estado.';
    el('pedido-content').innerHTML=paymentHTML()+`<section class="pedido-summary"><span class="pedido-status ${order.certificateAvailable?'is-paid':''}">${esc(statuses[order.status]||order.status)}</span><div class="pedido-total"><span>Total de productos</span><strong>${money(order.total)}</strong><small>Envío por cotizar con la tienda.</small></div><div class="pedido-details-tabs" role="tablist" aria-label="Información del pedido"><button id="pedido-items-tab" role="tab" type="button" data-detail-view="items" aria-controls="pedido-items-panel">Tu selección</button><button id="pedido-delivery-tab" role="tab" type="button" data-detail-view="delivery" aria-controls="pedido-delivery-panel">Entrega y documentos</button></div><div id="pedido-items-panel" class="pedido-detail-panel" data-panel="items" role="tabpanel" aria-labelledby="pedido-items-tab"><h3 class="pedido-mobile-heading">Tu selección</h3><ul class="pedido-items">${order.items.map((item,index)=>`<li data-item-index="${index}"><div><b>${esc(item.name)}</b><span>${item.quantity} ${item.quantity===1?'unidad':'unidades'}${item.engraving?` · Grabado: ${esc(item.engraving)}`:''}${item.size?` · Talla: ${esc(item.size)}`:''}</span></div><strong>${money(Number(item.price)*item.quantity)}</strong></li>`).join('')}</ul><div class="pedido-item-pagination"><button type="button" id="pedido-items-prev" aria-label="Productos anteriores"><svg class="cielo-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M20 12H4m6-6-6 6 6 6"/></svg></button><span id="pedido-items-page" role="status"></span><button type="button" id="pedido-items-next" aria-label="Productos siguientes"><svg class="cielo-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M4 12h16m-6-6 6 6-6 6"/></svg></button></div></div><div id="pedido-delivery-panel" class="pedido-detail-panel" data-panel="delivery" role="tabpanel" aria-labelledby="pedido-delivery-tab"><dl class="pedido-delivery"><dt>Entrega para</dt><dd>${esc(order.customer)}</dd><dt>Dirección</dt><dd>${esc([order.address,order.district,order.province,order.department].filter(Boolean).join(', '))}</dd></dl><div class="pedido-documents"><button class="export-button" type="button" data-receipt>Descargar pedido (PDF)</button>${order.certificateAvailable?`<a class="button certificate-download" href="${esc(order.certificateUrl)}" download>Descargar certificado (PDF)</a>`:'<p class="certificate-locked">El certificado estará disponible cuando confirmemos tu pago.</p>'}<p class="form-error" id="pedido-download-error" role="alert"></p></div></div></section>`;
    dialog.querySelectorAll('[data-detail-view]').forEach(button=>button.addEventListener('click',()=>{detailView=button.dataset.detailView;arrange();}));
    el('pedido-items-prev').addEventListener('click',()=>{itemPage--;arrange();});
    el('pedido-items-next').addEventListener('click',()=>{itemPage++;arrange();});
    arrange();
    dialog.querySelector('[data-receipt]').addEventListener('click',e=>CieloPDF.downloadOrder(order,e.currentTarget,el('pedido-download-error')));
    dialog.querySelector('.copy-cci')?.addEventListener('click',async()=>{try{await navigator.clipboard.writeText(payment.cci);el('copy-cci-status').textContent='CCI copiado.';}catch{el('copy-cci-status').textContent='Selecciona el número para copiarlo.';}});
    el('pedido-done').textContent=received?'Seguir comprando':'Cerrar detalle';
  }
  function arrange(){
    if(!order)return;
    const compact=desktop(),perPage=innerHeight<740?2:3,pages=Math.ceil(order.items.length/perPage);
    itemPage=Math.max(0,Math.min(itemPage,pages-1));
    dialog.querySelectorAll('[data-detail-view]').forEach(b=>{const active=b.dataset.detailView===detailView;b.setAttribute('aria-selected',String(active));});
    dialog.querySelectorAll('[data-panel]').forEach(p=>p.classList.toggle('is-inactive',compact&&p.dataset.panel!==detailView));
    dialog.querySelectorAll('[data-item-index]').forEach(li=>li.hidden=compact&&Math.floor(Number(li.dataset.itemIndex)/perPage)!==itemPage);
    dialog.querySelector('.pedido-item-pagination').hidden=!compact||pages<=1;
    el('pedido-items-page').textContent=`${itemPage+1} de ${pages}`;
    el('pedido-items-prev').disabled=itemPage===0;el('pedido-items-next').disabled=itemPage===pages-1;
  }
  window.addEventListener('resize',arrange);
  async function refresh(manual=false){
    if(!order||!dialog.open||refreshing)return;
    refreshing=true;const button=el('pedido-refresh');button.disabled=true;
    try{const latest=await api('/orders/'+order.id);const changed=JSON.stringify(latest)!==JSON.stringify(order);order=latest;
      if(changed){render();el('pedido-live').textContent=order.certificateAvailable?'Pago confirmado. Ya puedes descargar tu certificado.':'El estado de tu pedido se actualizó.';window.dispatchEvent(new CustomEvent('cielo:order-updated',{detail:order}));}
      else if(manual)el('pedido-live').textContent='Estado actualizado: '+(statuses[order.status]||order.status)+'.';
      if(order.status!=='pendiente'){clearInterval(timer);timer=null;}
    }catch(error){if(manual)el('pedido-live').textContent=error.message;}finally{button.disabled=false;refreshing=false;}
  }
  function open(value,options={}){
    order=value;detailView='items';itemPage=0;received=!!options.received;el('pedido-live').textContent=options.remembered?'Guardamos tus datos de entrega para la próxima compra.':'';render();
    if(!dialog.open){previousFocus=document.activeElement;previousOverflow=document.body.style.overflow;dialog.showModal();document.body.style.overflow='hidden';}
    el('pedido-content').scrollTop=0;el('pedido-close').focus({preventScroll:true});
    clearInterval(timer);if(order.status==='pendiente')timer=setInterval(()=>{if(!document.hidden)refresh();},15000);
    if(!configuration)configuration=api('/config').then(c=>{if(c.payment)payment=c.payment;}).catch(()=>{});
    configuration.then(()=>{if(dialog.open&&order.id===value.id)render();});
  }
  function close(){dialog.close();}
  el('pedido-close').addEventListener('click',close);el('pedido-done').addEventListener('click',close);
  el('pedido-refresh').addEventListener('click',()=>refresh(true));
  dialog.addEventListener('close',()=>{clearInterval(timer);timer=null;document.body.style.overflow=previousOverflow;const target=previousFocus?.isConnected&&previousFocus.getClientRects().length?previousFocus:document.querySelector(`[data-order-detail="${order?.id}"]`)||document.querySelector('#open-cart');target?.focus({preventScroll:true});});
  window.addEventListener('focus',()=>refresh());document.addEventListener('visibilitychange',()=>{if(!document.hidden)refresh();});
  return{open,refresh};
})();
