'use strict';
(async()=>{
 const {api,session,esc,money}=Cielo;
 const user=(await session()).user;
 if(!user){location.replace('login.html?next=cuenta.html');return;}
 if(user.role==='ADMIN'){location.replace('productos.html');return;}
 document.querySelector('#account-title').textContent='Hola, '+(user.firstName||user.name.split(' ')[0])+'.';
 document.querySelector('#account-email').textContent=user.email;
 const container=document.querySelector('#account-orders');let orders=[],loading=false;
 const statuses={pendiente:'Pendiente de pago',pagado:'Pago confirmado',preparando:'En preparación',enviado:'En camino',entregado:'Entregado',cancelado:'Cancelado'};
 function render(){
  container.innerHTML=orders.length?orders.map(order=>`<article class="account-order"><div class="account-order-heading"><div><h3>Pedido #${order.id}</h3><p>${new Intl.DateTimeFormat('es-PE',{dateStyle:'long',timeZone:'America/Lima'}).format(new Date(order.createdAt))}</p></div><span class="account-order-status">${esc(statuses[order.status]||order.status)}</span></div><div class="account-order-total"><span>Total de productos</span><b>${money(order.total)}</b></div><button class="account-order-detail-button" type="button" data-order-detail="${order.id}">Ver detalle del pedido <span aria-hidden="true"><svg class="cielo-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M7 17 17 7M7 7h10v10"/></svg></span></button>${order.certificateAvailable?`<a class="account-order-detail-button account-certificate-link" href="${esc(order.certificateUrl)}" download>Descargar certificado (PDF)</a>`:'<p class="account-certificate-pending">El certificado estará disponible al confirmar tu pago.</p>'}</article>`).join(''):'<div class="account-empty"><h3>Tu primera historia está por empezar.</h3><p>Cuando compres con tu cuenta, podrás consultar aquí tus pedidos.</p><a class="button" href="catalogo.html">Descubrir la colección <span aria-hidden="true"><svg class="cielo-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M7 17 17 7M7 7h10v10"/></svg></span></a></div>';
 }
 async function load(){if(loading)return;loading=true;try{orders=await api('/account/orders');render();}catch(error){document.querySelector('[data-session-error]').textContent=error.message;}finally{loading=false;}}
 container.addEventListener('click',event=>{const button=event.target.closest('[data-order-detail]');if(button)CieloPedidos.open(orders.find(o=>o.id===Number(button.dataset.orderDetail)));});
 window.addEventListener('cielo:order-updated',event=>{orders=orders.map(o=>o.id===event.detail.id?event.detail:o);render();});
 window.addEventListener('focus',()=>{if(!document.querySelector('#pedido-dialog')?.open)load();});
 document.addEventListener('visibilitychange',()=>{if(!document.hidden&&!document.querySelector('#pedido-dialog')?.open)load();});
 async function information(){
  const form=document.querySelector('#account-information-form'),status=document.querySelector('#account-information-status');
  const response=await fetch('/data/ubigeos-inei.json');if(!response.ok)throw Error('No se pudieron cargar las ubicaciones.');
  const locations=await response.json(),saved=await api('/account/delivery'),fields=form.elements;
  const options=(field,label,values)=>field.replaceChildren(new Option(label,''),...values.map(([name,value])=>new Option(name,value)));
  function provinces(){options(fields.province,'Selecciona provincia',[...new Set(locations.filter(x=>x.department===fields.department.value).map(x=>x.province))].sort().map(x=>[x,x]));fields.province.disabled=!fields.department.value;districts();}
  function districts(){options(fields.district,'Selecciona distrito',locations.filter(x=>x.department===fields.department.value&&x.province===fields.province.value).map(x=>[x.district,x.ubigeo]));fields.district.disabled=!fields.province.value;}
  function documentRules(){const rules={dni:['[0-9]{8}',8,8],ce:['[A-Za-z0-9]{9,12}',9,12],ruc:['(?:10|15|17|20)[0-9]{9}',11,11]};[fields.documentId.pattern,fields.documentId.minLength,fields.documentId.maxLength]=rules[fields.documentType.value];}
  options(fields.department,'Selecciona departamento',[...new Set(locations.map(x=>x.department))].sort().map(x=>[x,x]));
  fields.department.addEventListener('change',provinces);fields.province.addEventListener('change',districts);fields.documentType.addEventListener('change',documentRules);
  for(const key of ['firstName','lastName','phone'])fields[key].value=user[key]||'';
  if(saved.saved){for(const key of ['firstName','lastName','documentType','documentId','address','reference','postal','phone'])fields[key].value=saved[key]||'';fields.department.value=saved.department;}
  provinces();if(saved.saved){fields.province.value=saved.province;districts();fields.district.value=saved.district;}documentRules();
  form.hidden=false;status.textContent=saved.saved?'Estos son tus datos guardados.':'Completa tu información para guardarla en tu cuenta.';CieloControls.refresh();
  form.addEventListener('submit',async event=>{
   event.preventDefault();if(!CieloControls.validate(form))return;
   const body=Object.fromEntries(new FormData(form));
   await Cielo.busy(form.querySelector('[type=submit]'),async()=>{try{await api('/account/delivery',{method:'PUT',body});status.textContent='Tu información se guardó. Aparecerá en tu próxima compra.';}catch(error){status.textContent=error.message;}});
  });
 }
 await Promise.all([load(),information().catch(error=>{document.querySelector('#account-information-status').textContent=error.message;})]);
})().catch(error=>{document.querySelector('[data-session-error]').textContent=error.message;});
