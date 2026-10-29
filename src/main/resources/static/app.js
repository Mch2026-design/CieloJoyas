'use strict';
const {api, esc, money, busy} = Cielo;
const $ = selector => document.querySelector(selector);
const cartAnnouncement=document.createElement('span');cartAnnouncement.className='sr-only';cartAnnouncement.setAttribute('role','status');cartAnnouncement.setAttribute('aria-live','polite');document.body.append(cartAnnouncement);
let announcedCartCount=null;
let products = [], categories = [], coupon = null, locations = [];
let requestId = crypto.randomUUID();
let submittingOrder=false;
let locationsReadyResolve;const locationsReady=new Promise(resolve=>locationsReadyResolve=resolve);
function readCart() {
  try {
    const currentKey = 'cielo-cart-v2';
    let raw = localStorage.getItem(currentKey);
    let previousKey;
    // Conservar la selección al actualizar la identidad de la misma tienda.
    if (raw === null) {
      const previous = Object.keys(localStorage).filter(key => key.endsWith('-cart-v2') && key !== currentKey);
      if (previous.length === 1) { previousKey = previous[0]; raw = localStorage.getItem(previousKey); }
    }
    const rows = JSON.parse(raw || '[]');
    const valid = Array.isArray(rows) ? rows.filter(x => Number.isSafeInteger(x.productId) && Number.isSafeInteger(x.quantity) && x.quantity > 0 && x.quantity <= 100 && typeof x.key === 'string' && typeof x.engraving === 'string' && typeof x.size === 'string').slice(0,50) : [];
    if (previousKey && valid.length) { localStorage.setItem(currentKey, JSON.stringify(valid)); localStorage.removeItem(previousKey); }
    return valid;
  } catch { return []; }
}
let cart = readCart();
const params = new URLSearchParams(location.search);
const page = document.body.dataset.page || 'inicio';
const onlyCustom = page === 'personaliza';
const onlyOffers = page === 'ofertas';
const state = {category:params.get('categoria') || 'Todo', query:params.get('q') || '', min:String(Math.max(0,Math.min(250,Number(params.get('min'))||0))), max:String(params.has('max')?Math.max(0,Math.min(250,Number(params.get('max'))||0)):250), sort:params.get('orden') || 'relevant', customizable:onlyCustom || params.get('personalizable')==='1'};
const layers = [];
let lastFocus;
const focusable = el => [...el.querySelectorAll('button:not([disabled]),a[href],input:not([disabled]),select:not([disabled]),textarea,[tabindex="0"]')].filter(x => !x.hidden && x.getClientRects().length);
function syncLayers() {
  const top = layers.at(-1);
  for (const el of document.querySelectorAll('body > header, body > main, body > footer, #cart-panel, #filter-drawer, [role="dialog"]')) {
    el.inert = !!top && el !== top;
    if ((el.id === 'cart-panel' || el.id === 'filter-drawer') && !layers.includes(el)) el.inert = true;
  }
  document.body.style.overflow = top ? 'hidden' : '';
}
function show(el) {
  if (!layers.length) lastFocus = document.activeElement;
  if (layers.includes(el)) return;
  layers.push(el); el.hidden = false; el.classList.add('open'); el.setAttribute('aria-hidden','false');
  if (el.id === 'cart-panel') $('#overlay').classList.add('show');
  if (el.id === 'filter-drawer') $('#filter-overlay').classList.add('show');
  syncLayers(); (focusable(el)[0] || el).focus();
}
function hide(el = layers.at(-1)) {
  if (!el) return;
  const index = layers.indexOf(el); if (index < 0) return;
  layers.splice(index,1); el.classList.remove('open'); el.setAttribute('aria-hidden','true');
  if (el.id === 'cart-panel') $('#overlay').classList.remove('show');
  else if (el.id === 'filter-drawer') $('#filter-overlay').classList.remove('show');
  else el.hidden = true;
  syncLayers(); if (layers.length) (focusable(layers.at(-1))[0] || layers.at(-1)).focus(); else lastFocus?.focus();
}
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && submittingOrder) { e.preventDefault(); return; }
  if (e.key === 'Escape') { hide(); $('#nav').classList.remove('open'); $('#menu-toggle').setAttribute('aria-expanded','false'); }
  if (e.key !== 'Tab' || !layers.length) return;
  const nodes = focusable(layers.at(-1)); if (!nodes.length) { e.preventDefault(); return; }
  if (e.shiftKey && document.activeElement === nodes[0]) { e.preventDefault(); nodes.at(-1).focus(); }
  else if (!e.shiftKey && document.activeElement === nodes.at(-1)) { e.preventDefault(); nodes[0].focus(); }
});
let noticeTimer;
function notify(message) { clearTimeout(noticeTimer); $('#live-message').textContent = message; noticeTimer = setTimeout(() => { $('#live-message').textContent = ''; }, 5000); }
function save() {
  requestId = crypto.randomUUID(); coupon = null;
  try { sessionStorage.removeItem('cielo-coupon'); } catch {}
  $('#coupon-message').textContent=''; $('#checkout-coupon-message').textContent='';
  try { localStorage.setItem('cielo-cart-v2', JSON.stringify(cart)); } catch { notify('Tu navegador no permite guardar el carrito. Puedes continuar en esta página.'); }
  renderCart();
}
function card(p, headingLevel = 3) {
  return `<article class="product-card"><div class="product-image-wrap"><button class="product-image-button" data-view="${p.id}" aria-label="Ver ${esc(p.name)}"><img class="product-image" src="${esc(p.src)}" alt="${esc(p.alt)}" width="760" height="900" loading="lazy"></button>${p.tag ? `<span class="product-tag">${esc(p.tag)}</span>` : ''}<button class="quick-add" data-add="${p.id}" ${p.stock <= 0 ? 'disabled' : ''}>${p.stock <= 0 ? 'AGOTADO' : 'AÑADIR AL CARRITO +'}</button></div><div class="product-info"><span class="product-category">${esc(p.category)}</span><h${headingLevel}><button class="product-name-link" data-view="${p.id}">${esc(p.name)}</button></h${headingLevel}><div class="product-prices"><b class="price">${money(p.price)}</b>${p.compareAtPrice ? `<del>${money(p.compareAtPrice)}</del>` : ''}</div></div></article>`;
}
function renderFilters() { $('#filters').innerHTML = ['Todo',...categories.map(c=>c.name)].map(c => `<button class="filter-btn ${state.category===c?'active':''}" aria-pressed="${state.category===c}" data-category="${esc(c)}">${esc(c)}</button>`).join(''); }
function renderProducts() {
  if (!$('#product-grid')) return;
  const url = new URL(location.href);
  for (const [key,value] of Object.entries({categoria:state.category==='Todo'?'':state.category,q:state.query,min:state.min==='0'?'':state.min,max:state.max==='250'?'':state.max,orden:state.sort==='relevant'?'':state.sort})) { if(value)url.searchParams.set(key,value);else url.searchParams.delete(key); }
  if(state.customizable&&!onlyCustom)url.searchParams.set('personalizable','1');else url.searchParams.delete('personalizable');
  history.replaceState(null,'',url);
  let rows = products.filter(p => (!onlyOffers || Number(p.compareAtPrice) > Number(p.price)) && (state.category==='Todo' || p.category===state.category) && (!state.customizable || p.customizable) && Number(p.price) >= Number(state.min || 0) && Number(p.price) <= (state.max === '' ? Infinity : Number(state.max)) && `${p.name} ${p.category}`.toLocaleLowerCase('es').includes(state.query.toLocaleLowerCase('es')));
  const sorters = {'bestselling':(a,b)=>b.salesCount-a.salesCount,'name-asc':(a,b)=>a.name.localeCompare(b.name,'es'),'name-desc':(a,b)=>b.name.localeCompare(a.name,'es'),'price-asc':(a,b)=>a.price-b.price,'price-desc':(a,b)=>b.price-a.price,'date-asc':(a,b)=>new Date(a.createdAt)-new Date(b.createdAt)||a.id-b.id,'date-desc':(a,b)=>new Date(b.createdAt)-new Date(a.createdAt)||b.id-a.id};
  if (sorters[state.sort]) rows.sort(sorters[state.sort]);
  $('#product-grid').innerHTML = rows.map(p=>card(p,page==='personaliza'?3:2)).join(''); $('#result-count').textContent = `${rows.length} ${rows.length===1?'pieza':'piezas'}`; $('#show-filter-results').textContent = `Ver ${rows.length} ${rows.length===1?'artículo':'artículos'}`; $('#empty-state').hidden = rows.length > 0;
}
function renderHome() {
  const newest = $('#new-products');
  if(newest)newest.innerHTML = [...products].sort((a,b)=>new Date(b.createdAt)-new Date(a.createdAt)||b.id-a.id).slice(0,page==='novedades'?8:4).map(p=>card(p,page==='novedades'?2:3)).join('');
  const favorite = $('#favorite-products');
  if(favorite)favorite.innerHTML = [...products].sort((a,b)=>b.salesCount-a.salesCount||a.id-b.id).slice(0,4).map(p=>card(p)).join('');
  const sale = $('#season-products');
  if(sale){const offers=products.filter(p=>Number(p.compareAtPrice)>Number(p.price));sale.innerHTML=offers.map(p=>card(p)).join('');sale.closest('section').hidden=offers.length===0;}
}
const productOf = line => products.find(p=>p.id===line.productId);
const subtotal = () => cart.reduce((sum,line)=>sum + Number(productOf(line)?.price || 0)*line.quantity,0);
const discount = () => Math.round(subtotal() * (coupon?.discount || 0)) / 100;
function renderCart() {
  const count = cart.reduce((n,line)=>n+line.quantity,0);
  if(announcedCartCount!==count){cartAnnouncement.textContent=`Tu carrito tiene ${count} ${count===1?'unidad':'unidades'}.`;announcedCartCount=count;}
  $('#bag-count').textContent=count; $('#open-cart').setAttribute('aria-label',`Abrir carrito, ${count} unidades`); $('#cart-items-label').textContent=count?`(${count})`:'';
  $('#cart-empty').hidden=count>0; $('#cart-bottom').hidden=count===0;
  $('#cart-items').innerHTML=cart.map(line=>{
    const p=productOf(line); if(!p)return `<article class="cart-item"><p>Esta pieza ya no está disponible.</p><button data-remove="${esc(line.key)}">Eliminar</button></article>`;
    const used=cart.filter(x=>x.productId===p.id).reduce((n,x)=>n+x.quantity,0);
    return `<article class="cart-item"><img src="${esc(p.src)}" alt="${esc(p.alt)}" width="80" height="100"><div class="cart-item-info"><h3>${esc(p.name)}</h3><small>${esc(p.material)}${line.engraving?`<br>Grabado: ${esc(line.engraving)}`:''}${line.size?`<br>Talla: ${esc(line.size)}`:''}</small>${used>p.stock?'<p class="field-error">Revisa la cantidad: el stock disponible cambió.</p>':''}<div class="qty"><button data-qty="${esc(line.key)}" data-step="-1" aria-label="Quitar una unidad de ${esc(p.name)}">−</button><span>${line.quantity}</span><button data-qty="${esc(line.key)}" data-step="1" aria-label="Añadir una unidad de ${esc(p.name)}" ${used>=p.stock?'disabled':''}>+</button><button class="remove-item" data-remove="${esc(line.key)}">Eliminar</button></div></div><b class="price">${money(p.price*line.quantity)}</b></article>`;
  }).join('');
  $('#subtotal').textContent=money(subtotal()); $('#discount-row').hidden=!coupon; $('#discount-row span').textContent=coupon?`Descuento ${coupon.code}`:'Descuento'; $('#discount').textContent=money(discount());
  updateSummary();
}
function add(id, engraving='', size='') {
  const p=products.find(x=>x.id===id); if(!p)return;
  const used=cart.filter(x=>x.productId===id).reduce((n,x)=>n+x.quantity,0);
  if(used>=p.stock){notify('No quedan más unidades de esa pieza.');return;}
  const key=JSON.stringify([id,engraving.trim(),size.trim()]);
  const wasEmpty=cart.length===0;
  const line=cart.find(x=>x.key===key);
  if(line){if(line.quantity>=100)return;line.quantity++;}else if(cart.length>=50){notify('Tu carrito admite hasta 50 selecciones distintas.');return;}else cart.push({key,productId:id,quantity:1,engraving:engraving.trim(),size:size.trim()});
  save();if(wasEmpty)notify('Pieza añadida al carrito. Puedes seguir eligiendo.');
}
function openProduct(id) {
  const p=products.find(x=>x.id===id);if(!p)return;
  const el=$('#product-modal');el.dataset.productId=p.id;
  $('#product-modal-image').src=p.src;$('#product-modal-image').alt=p.alt;$('#product-modal-category').textContent=p.category;$('#product-modal-title').textContent=p.name;$('#product-modal-price').textContent=money(p.price);
  $('#product-modal-material').textContent=`${p.material}. ${p.stock>0?`${p.stock} unidades disponibles.`:'Pieza agotada.'}`;
  $('#product-modal-certificate').hidden=!p.certificateUrl;if(p.certificateUrl)$('#product-modal-certificate').href=p.certificateUrl;
  $('#product-modal-customization').innerHTML=p.customizable?'<label>Texto para grabado (opcional)<input id="product-engraving" maxlength="20" autocomplete="off"></label><label>Talla o medida (opcional)<input id="product-size" maxlength="30" autocomplete="off"></label>':'';
  $('#product-modal-add').disabled=p.stock<=0;show(el);
}
$('#product-modal-add').addEventListener('click',()=>{const id=Number($('#product-modal').dataset.productId),engraving=$('#product-engraving')?.value||'',size=$('#product-size')?.value||'';hide($('#product-modal'));add(id,engraving,size);});
document.addEventListener('click',e=>{
  const view=e.target.closest('[data-view]'),button=e.target.closest('[data-add]'),cat=e.target.closest('[data-shop-category]'),custom=e.target.closest('[data-shop-customizable]');
  if(view)openProduct(Number(view.dataset.view));
  if(button)add(Number(button.dataset.add));
});
$('#cart-items').addEventListener('click',e=>{
  const q=e.target.closest('[data-qty]'),r=e.target.closest('[data-remove]');
  if(q){const line=cart.find(x=>x.key===q.dataset.qty);if(!line)return;const p=productOf(line),step=Number(q.dataset.step),used=cart.filter(x=>x.productId===line.productId).reduce((n,x)=>n+x.quantity,0);if(step>0&&(!p||used>=p.stock))return;line.quantity+=step;cart=cart.filter(x=>x.quantity>0);save();}
  if(r){cart=cart.filter(x=>x.key!==r.dataset.remove);save();}
});
$('#forget-cart').addEventListener('click',()=>{cart=[];$('#gift-message').value='';save();notify('Selección y grabados borrados de este navegador.');});
for (const [button,panel] of [['open-cart','cart-panel'],['open-filters','filter-drawer']]) $('#'+button)?.addEventListener('click',()=>show($('#'+panel)));
for (const [button,panel] of [['close-cart','cart-panel'],['continue-shopping','cart-panel'],['overlay','cart-panel'],['close-filters','filter-drawer'],['show-filter-results','filter-drawer'],['filter-overlay','filter-drawer'],['close-product-modal','product-modal'],['close-checkout','checkout-modal'],['close-modal','order-modal'],['done-order','order-modal'],['close-policies','policy-modal']]) $('#'+button)?.addEventListener('click',()=>hide($('#'+panel)));
const menuPreview=document.createElement('a');
menuPreview.className='nav-preview';
menuPreview.href='catalogo.html';
menuPreview.innerHTML='<img src="https://images.unsplash.com/photo-1617038220319-276d3cfab638?auto=format&fit=crop&w=500&q=85" alt="Argollas doradas de la colección Cielo" width="240" height="360" loading="lazy"><span>Un detalle muy tú<small>Explora la colección <svg class="cielo-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M7 17 17 7M7 7h10v10"/></svg></small></span>';
$('#nav').append(menuPreview);
$('#menu-toggle').addEventListener('click',()=>{const open=$('#nav').classList.toggle('open');$('#menu-toggle').setAttribute('aria-expanded',String(open));});
$('#nav').addEventListener('click',()=>{$('#nav').classList.remove('open');$('#menu-toggle').setAttribute('aria-expanded','false');});
if ($('#product-grid')) {
$('#filters').addEventListener('click',e=>{const b=e.target.closest('[data-category]');if(b){state.category=b.dataset.category;state.customizable=onlyCustom;renderFilters();renderProducts();}});
$('#search').addEventListener('input',e=>{state.query=e.target.value;renderProducts();});
$('#sort-products').addEventListener('change',e=>{state.sort=e.target.value;renderProducts();});
$('#search-clear').addEventListener('click',()=>{state.query='';$('#search').value='';renderProducts();$('#search').focus();});
function syncPrice(source){
 let low=Math.max(0,Math.min(250,Number($('#price-min').value)||0));
 let high=Math.max(0,Math.min(250,$('#price-max').value===''?250:Number($('#price-max').value)||0));
 if(low>high){if(source==='min')high=low;else low=high;}
 state.min=String(low);state.max=String(high);paintPrice();renderProducts();
}
function paintPrice(){for(const kind of ['min','max']){$('#price-'+kind).value=state[kind];$('#price-range-'+kind).value=state[kind];}$('#price-range-label').textContent=`${money(state.min)} a ${money(state.max)}`;}
for(const kind of ['min','max']){
 $('#price-'+kind).addEventListener('change',()=>syncPrice(kind));
 $('#price-range-'+kind).addEventListener('input',()=>{$('#price-'+kind).value=$('#price-range-'+kind).value;syncPrice(kind);});
}
$('#apply-price').addEventListener('click',()=>syncPrice('max'));
$('#clear-filters').addEventListener('click',()=>{Object.assign(state,{category:'Todo',query:'',min:'0',max:'250',customizable:onlyCustom});$('#search').value='';paintPrice();renderFilters();renderProducts();});
if(Number(state.min)>Number(state.max))state.min=state.max;
paintPrice();
}
async function applyCoupon(input,message,button){await busy(button,async()=>{try{coupon=await api('/coupons/validate',{method:'POST',body:{code:input.value,subtotal:Math.round(subtotal()*100)/100}});message.textContent=`Cupón ${coupon.code}: ${coupon.discount}% de descuento.`;}catch(e){coupon=null;message.textContent=e.message;}try{if(coupon)sessionStorage.setItem('cielo-coupon',coupon.code);else sessionStorage.removeItem('cielo-coupon');}catch{}renderCart();});}
async function restoreCoupon(){
  let code;try{code=sessionStorage.getItem('cielo-coupon');}catch{return;}
  if(!code||!cart.length)return;
  $('#coupon').value=code;
  await applyCoupon($('#coupon'),$('#coupon-message'),$('#apply-coupon'));
}
$('#apply-coupon').addEventListener('click',e=>applyCoupon($('#coupon'),$('#coupon-message'),e.currentTarget));
$('#checkout-apply-coupon').addEventListener('click',e=>applyCoupon($('#checkout-coupon'),$('#checkout-coupon-message'),e.currentTarget));
function updateSummary(){
  $('#checkout-summary-items').innerHTML=cart.map(line=>{const p=productOf(line);return p?`<div class="checkout-summary-item"><img src="${esc(p.src)}" alt="${esc(p.alt)}" width="64" height="80"><span>${esc(p.name)}<small>Cantidad: ${line.quantity}${line.engraving?` · Grabado: ${esc(line.engraving)}`:''}${line.size?` · Talla: ${esc(line.size)}`:''}</small></span><b>${money(p.price*line.quantity)}</b></div>`:'';}).join('');
  const method=$('#checkout-form').elements.paymentMethod.value;
  $('#checkout-selected-payment').textContent=({yape:'Yape',plin:'Plin',transferencia:'Transferencia bancaria',tarjeta:'Tarjeta'})[method]||'Selecciona un medio';
  $('#checkout-subtotal').textContent=money(subtotal());$('#checkout-discount').textContent=money(discount());$('#checkout-total').textContent=money(subtotal()-discount());$('#checkout-button-total').textContent=money(subtotal()-discount());
}
$('#checkout').addEventListener('click',async()=>{if(!cart.length)return;hide($('#cart-panel'));$('#checkout-error').textContent='';$('#checkout-coupon-message').textContent='';$('#checkout-coupon').value=coupon?.code||'';updateSummary();await restoreDelivery();show($('#checkout-modal'));});
$('#checkout-form').addEventListener('change',e=>{if(e.target.name==='paymentMethod')updateSummary();});
$('#checkout-form').addEventListener('submit',async e=>{
  e.preventDefault();if(submittingOrder)return;
  const form=e.currentTarget;if(!CieloControls.validate(form))return;
  const d=Object.fromEntries(new FormData(form)),button=form.querySelector('[type="submit"]');
  $('#checkout-error').textContent='';submittingOrder=true;
  const fields=[...form.elements,...document.querySelectorAll('#close-checkout,#checkout-apply-coupon')].filter(el=>el!==button);
  const states=fields.map(el=>[el,el.disabled]);
  const payload={...d,rememberDelivery:form.elements.rememberDelivery.checked,requestId,phone:d.phone.replace(/\s/g,''),addressLine:d.address,ubigeo:d.district,coupon:coupon?.code||'',giftMessage:$('#gift-message').value,items:cart.map(line=>({productId:line.productId,quantity:line.quantity,engraving:line.engraving,size:line.size}))};
  await busy(button,async()=>{
   states.forEach(([el])=>el.disabled=true);button.querySelector('.checkout-submit-label').hidden=true;button.querySelector('.checkout-processing').hidden=false;
   try{
    const result=await api('/orders',{method:'POST',body:payload});
    hide($('#checkout-modal'));await CieloPedidos.open(result,{received:true,remembered:d.rememberDelivery==='on'});cart=[];save();form.reset();$('#gift-message').value='';syncDocument();syncDepartment();
    try{await loadProducts();}catch{notify('Tu pedido fue registrado. El catálogo se actualizará al recargar.');}
   }catch(error){$('#checkout-error').textContent=error.message;$('#checkout-error').focus();}
   finally{states.forEach(([el,disabled])=>el.disabled=disabled);button.querySelector('.checkout-submit-label').hidden=false;button.querySelector('.checkout-processing').hidden=true;submittingOrder=false;CieloControls.refresh();}
  });
});
const form=$('#checkout-form');
function syncDocument(){const type=form.elements.documentType.value,field=form.elements.documentId;const rules={dni:['[0-9]{8}',8,8],ce:['[A-Za-z0-9]{9,12}',9,12],ruc:['(?:10|15|17|20)[0-9]{9}',11,11]};[field.pattern,field.minLength,field.maxLength]=rules[type];field.title=`Revisa tu ${type.toUpperCase()}.`;}
$('#document-type').addEventListener('change',syncDocument);syncDocument();
const options=(el,label,values)=>el.replaceChildren(new Option(label,''),...values.map(([name,value])=>new Option(name,value)));
function syncDepartment(){const dep=$('#department'),prov=$('#province'),dist=$('#district');options(prov,'Selecciona provincia',[...new Set(locations.filter(x=>x.department===dep.value).map(x=>x.province))].sort().map(x=>[x,x]));prov.disabled=!dep.value;options(dist,'Selecciona distrito',[]);dist.disabled=true;}
$('#department').addEventListener('change',syncDepartment);$('#province').addEventListener('change',()=>{options($('#district'),'Selecciona distrito',locations.filter(x=>x.department===$('#department').value&&x.province===$('#province').value).sort((a,b)=>a.district.localeCompare(b.district,'es')).map(x=>[x.district,x.ubigeo]));$('#district').disabled=!$('#province').value;});
async function restoreDelivery(){
 const field=form.elements.rememberDelivery, help=$('#remember-delivery-help');
 field.checked=false;$('#forget-delivery').hidden=true;$('#remember-delivery-status').textContent='';
 // Clear any previous account's data before checking the current session.
 for(const key of ['email','firstName','lastName','documentId','address','reference','postal','phone'])form.elements[key].value='';
 form.elements.documentType.value='dni';form.elements.email.readOnly=false;syncDocument();
 $('#department').value='';syncDepartment();
 try{
  const {user}=await Cielo.session(true);field.disabled=!user;
  if(!user){help.textContent='Inicia sesión para guardar tus datos de entrega en tu cuenta.';return;}
  for(const [key,value]of Object.entries({email:user.email,firstName:user.firstName,lastName:user.lastName,phone:user.phone}))if(value)form.elements[key].value=value;
  form.elements.email.readOnly=true;
  const saved=await api('/account/delivery');
  await locationsReady;
  help.textContent=saved.saved?'Usamos los datos que guardaste. Márcalo para actualizar los cambios al comprar.':'Guardaremos estos datos de entrega para tus próximas compras al registrar el pedido.';
  if(!saved.saved)return;
  for(const key of ['firstName','lastName','documentType','documentId','address','reference','postal','phone'])form.elements[key].value=saved[key]||'';
  syncDocument();$('#department').value=saved.department;syncDepartment();$('#province').value=saved.province;$('#province').dispatchEvent(new Event('change'));$('#district').value=saved.district;
  $('#forget-delivery').hidden=false;
 }catch(error){field.disabled=true;help.textContent=error.message;}
}
$('#forget-delivery').addEventListener('click',e=>busy(e.currentTarget,async()=>{try{await api('/account/delivery',{method:'DELETE'});form.elements.rememberDelivery.checked=false;e.target.hidden=true;$('#remember-delivery-help').textContent='Puedes marcar Recuérdame para guardar estos datos al registrar tu próxima compra.';$('#remember-delivery-status').textContent='Datos guardados eliminados. Puedes continuar con esta compra.';}catch(error){$('#remember-delivery-status').textContent=error.message;}}));
form.addEventListener('invalid',e=>{e.target.closest('label')?.classList.add('has-error');},true);form.addEventListener('input',e=>{if(e.target.checkValidity())e.target.closest('label')?.classList.remove('has-error');});
for(const id of ['guarantee-form','newsletter-form']){const publicForm=$('#'+id);if(!publicForm)continue;publicForm.addEventListener('submit',async e=>{
  e.preventDefault();const form=e.currentTarget,body=Object.fromEntries(new FormData(form)),message=$('#'+({'guarantee-form':'guarantee-message','newsletter-form':'newsletter-message'}[id]));
  await busy(form.querySelector('button[type="submit"],button.button'),async()=>{try{const result=await api({'guarantee-form':'/guarantees','newsletter-form':'/newsletter'}[id],{method:'POST',body});message.textContent=result.message+(result.caseId?` Caso #${result.caseId}.`:'');form.reset();}catch(error){message.textContent=error.message;}});
});
}
function renderPolicy(){$('#policy-content').innerHTML=$('#policy-select').value==='returns'?'<h3>Cambios</h3><p>Contacta a la tienda con tu número de pedido y comprobante para consultar las condiciones de cambio antes de enviar una pieza.</p>':'<h3>Garantías y devoluciones</h3><p>Usa el formulario de garantía para describir un defecto e incluir evidencia. La tienda evaluará tu solicitud y coordinará contigo los siguientes pasos.</p><p>Los pedidos se registran con pago pendiente. El envío se cotiza por separado; el total mostrado corresponde a las piezas y al descuento aplicado.</p>';}
for(const id of ['open-policies','open-policies-checkout'])$('#'+id).addEventListener('click',e=>{e.preventDefault();renderPolicy();show($('#policy-modal'));});$('#policy-select').addEventListener('change',renderPolicy);
async function loadProducts(){
  products=(await api('/products')).map(p=>({...p,price:Number(p.price)}));
  renderProducts();renderHome();renderCart();
}
async function init(){
  if($('.drawer-price') && $('.price-filter-card'))$('.drawer-price').appendChild($('.price-filter-card'));
  syncLayers();
  if($('#product-grid')){ $('#product-grid').textContent='Cargando colección…';$('#search').value=state.query;$('#sort-products').value=state.sort;$('#price-min').value=state.min;$('#price-max').value=state.max; }
  try{categories=await api('/categories');renderFilters();await loadProducts();const linkedProduct=Number(new URLSearchParams(location.search).get('producto'));if(document.body.dataset.page==='catalogo' && linkedProduct)openProduct(linkedProduct);await restoreCoupon();$('#service-notice').hidden=true;}
  catch(e){$('#service-notice').textContent=e.message;$('#service-notice').hidden=false;if($('#product-grid')){$('#product-grid').innerHTML=`<p role="alert">${esc(e.message)} <button id="retry-catalog" type="button">Reintentar</button></p>`;$('#retry-catalog').addEventListener('click',()=>loadProducts().then(()=>$('#service-notice').hidden=true).catch(error=>notify(error.message)));}}
  try{const r=await fetch('/data/ubigeos-inei.json');if(!r.ok)throw Error();locations=await r.json();options($('#department'),'Selecciona departamento',[...new Set(locations.map(x=>x.department))].sort().map(x=>[x,x]));}
  catch{$('#checkout-error').textContent='No se pudieron cargar las ubicaciones. Recarga antes de comprar.';}finally{locationsReadyResolve();}
  try{const c=await api('/config');if(c.whatsapp){$('#whatsapp-contact').href='https://wa.me/'+c.whatsapp;$('#whatsapp-contact').hidden=false;}}catch{}
}
init();
