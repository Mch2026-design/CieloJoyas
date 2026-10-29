'use strict';
(() => {
  const {api,session,esc,busy}=Cielo, $=s=>document.querySelector(s);
  const grid=$('#reviews-grid'), filter=$('#review-filter'), state=$('#reviews-state'), dialog=$('#review-dialog'), form=$('#compose-review');
  const verifiedIcon='<svg aria-hidden="true" width="14" height="14" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="10" cy="10" r="8"/><path d="m6 10 3 3 5-6"/></svg>';
  let reviews=[], purchases=[], previewUrl, sending=false;
  const date=value=>new Intl.DateTimeFormat('es-PE',{day:'numeric',month:'short',year:'numeric'}).format(new Date(value));
  for(const modal of [dialog,$('#review-photo-dialog')])modal.addEventListener('keydown',e=>{
    if(e.key!=='Tab')return;
    const controls=[...modal.querySelectorAll('button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),a[href]')].filter(el=>el.getClientRects().length);
    const first=controls[0],last=controls.at(-1);
    if(e.shiftKey&&document.activeElement===first){e.preventDefault();last.focus();}
    else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus();}
  });
  function render() {
    const rows=reviews.filter(r=>!filter.value || String(r.product.id)===filter.value);
    const verified=rows.filter(r=>r.verified).length;
    $('#reviews-count').textContent=`${rows.length} ${rows.length===1?'historia':'historias'} · ${verified} ${verified===1?'compra verificada':'compras verificadas'}`;
    $('#reviews-demo-note').hidden=!rows.some(r=>r.demonstration);
    state.hidden=rows.length>0;
    if(!rows.length)state.textContent='Esta pieza aún no tiene reseñas publicadas. Tu experiencia puede ser la primera.';
    grid.innerHTML=rows.map(r=>`<article class="review-card"><button class="review-card-photo" type="button" data-photo="${r.id}" aria-label="Ampliar foto de ${esc(r.product.name)} en la reseña de ${esc(r.name)}"><img src="${esc(r.photo)}" alt="${esc(r.demonstration?'Foto ilustrativa':'Foto compartida por '+r.name)} de ${esc(r.product.name)}" loading="lazy" decoding="async" width="600" height="526"><span class="review-photo-hint" aria-hidden="true">Ver foto <svg class="cielo-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M7 17 17 7M7 7h10v10"/></svg></span></button><div class="review-card-copy"><div class="review-author"><b>${esc(r.name)}</b><time datetime="${esc(r.createdAt)}">${esc(date(r.createdAt))}</time></div><span class="review-badge${r.demonstration?' demo':''}">${r.verified?verifiedIcon+'Compra verificada':'Reseña de demostración'}</span><blockquote>${esc(r.body)}</blockquote><div class="reviewed-product"><img src="${esc(r.product.src)}" alt="" loading="lazy" width="54" height="60"><div><small>La pieza de esta historia</small>${r.product.active?`<a href="catalogo.html?producto=${r.product.id}">${esc(r.product.name)} <svg class="cielo-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M7 17 17 7M7 7h10v10"/></svg></a>`:`<b>${esc(r.product.name)}</b><small>Fuera de colección</small>`}</div></div></div></article>`).join('');
  }
  async function load() {
    grid.setAttribute('aria-busy','true');state.hidden=true;
    try {
      reviews=await api('/reviews');
      filter.replaceChildren(new Option('Todas las piezas',''),...[...new Map(reviews.map(r=>[r.product.id,r.product])).values()].sort((a,b)=>a.name.localeCompare(b.name,'es')).map(p=>new Option(p.name,p.id)));
      filter.disabled=false;render();
    } catch(error) {
      $('#reviews-count').textContent='Las historias estarán aquí.';
      state.hidden=false;state.replaceChildren(document.createTextNode(error.message));
      const retry=document.createElement('button');retry.className='review-secondary';retry.textContent='Volver a intentar';retry.addEventListener('click',load);state.append(retry);
    } finally {grid.setAttribute('aria-busy','false');}
  }
  filter.addEventListener('change',render);
  grid.addEventListener('click',e=>{
    const button=e.target.closest('[data-photo]');if(!button)return;
    const r=reviews.find(r=>r.id===Number(button.dataset.photo));if(!r)return;
    $('#review-full-photo').src=r.photo;$('#review-full-photo').alt=`${r.demonstration?'Foto ilustrativa de':'Foto de '+r.name+' con'} ${r.product.name}`;
    $('#review-photo-title').textContent=r.product.name;
    $('#review-photo-caption').textContent=`${r.name} · ${r.demonstration?'Fotografía ilustrativa de demostración':'Compra verificada'}`;
    $('#review-photo-dialog').showModal();
  });
  $('#close-review-photo').addEventListener('click',()=>$('#review-photo-dialog').close());
  function selectedProduct() {
    const p=($('#review-purchase').value===''?null:purchases[Number($('#review-purchase').value)]);
    $('#review-selected-product').innerHTML=p?`<img src="${esc(p.src)}" alt=""><div>${esc(p.name)}<small>Pedido #${p.orderId} · ${esc(date(p.purchasedAt))}</small></div>`:'';
  }
  async function eligibility() {
    const {user}=await session();
    if(!user){$('#review-login').hidden=false;$('#review-eligibility').textContent='Necesitas una compra con pago confirmado para publicar.';return;}
    if(user.role!=='CLIENTE'){$('#review-eligibility').textContent='Las clientas comparten sus experiencias desde su cuenta. Puedes revisarlas en Posventa.';return;}
    purchases=await api('/account/reviewable-products');
    $('#write-review').hidden=false;
    $('#review-eligibility').textContent=purchases.length?`${purchases.length} ${purchases.length===1?'pieza disponible':'piezas disponibles'} para contar tu experiencia.`:'Cuando confirmemos el pago de una pieza que aún no has reseñado, podrás compartirla aquí.';
    $('#review-purchase').replaceChildren(new Option('Selecciona una pieza',''),...purchases.map((p,i)=>new Option(`${p.name} · Pedido #${p.orderId}`,i)));
  }
  $('#write-review').addEventListener('click',async()=>{
    try {
      await busy($('#write-review'),eligibility);
      if(!purchases.length){$('#review-eligibility').textContent='No tienes piezas pendientes de reseñar con pago confirmado. Revisa el estado de tus pedidos en tu cuenta.';return;}
      $('#review-submit-error').textContent='';dialog.showModal();
    } catch(error){$('#review-eligibility').textContent=error.message;}
  });
  function close(){if(!sending)dialog.close();}
  $('#close-review').addEventListener('click',close);$('#cancel-review').addEventListener('click',close);
  dialog.addEventListener('cancel',e=>{if(sending)e.preventDefault();});
  $('#review-purchase').addEventListener('change',selectedProduct);
  $('#review-body').addEventListener('input',()=>$('#review-counter').textContent=`${$('#review-body').value.length} / 3000 caracteres · Mínimo 10`);
  $('#review-photo').addEventListener('change',()=>{
    if(previewUrl)URL.revokeObjectURL(previewUrl);
    const input=$('#review-photo'),file=input.files[0],preview=$('#review-photo-preview');preview.hidden=true;preview.removeAttribute('src');input.setCustomValidity('');$('#review-submit-error').textContent='';
    if(!file)return;
    if(!['image/jpeg','image/png'].includes(file.type)||file.size>5*1024*1024){input.setCustomValidity('Selecciona una foto JPG o PNG de hasta 5 MB.');CieloControls.validateField(input);return;}
    previewUrl=URL.createObjectURL(file);preview.src=previewUrl;preview.hidden=false;
  });
  form.addEventListener('submit',async e=>{
    e.preventDefault();if(sending||!CieloControls.validate(form))return;
    const value=$('#review-purchase').value,p=value===''?null:purchases[Number(value)];if(!p)return;
    await busy($('#submit-review'),async()=>{
      sending=true;$('#review-submit-error').textContent='';$('#close-review').disabled=true;$('#cancel-review').disabled=true;
      try {
        const body=new FormData();body.set('productId',p.productId);body.set('orderId',p.orderId);body.set('body',$('#review-body').value.trim());body.set('photo',$('#review-photo').files[0]);
        const result=await api('/account/reviews',{method:'POST',body});
        dialog.close();form.reset();$('#review-photo-preview').hidden=true;$('#review-counter').textContent='0 / 3000 caracteres · Mínimo 10';selectedProduct();
        if(previewUrl)URL.revokeObjectURL(previewUrl);
        $('#review-eligibility').textContent=result.message;
        purchases=purchases.filter(x=>x.orderId!==p.orderId||x.productId!==p.productId);
        const message=document.createElement('p');message.className='review-submitted';message.textContent=result.message;message.setAttribute('role','status');$('#reviews-state').before(message);message.tabIndex=-1;message.focus({preventScroll:true});
      }catch(error){$('#review-submit-error').textContent=error.message;$('#review-submit-error').focus();}
      finally{sending=false;$('#close-review').disabled=false;$('#cancel-review').disabled=false;}
    });
  });
  load();eligibility().catch(error=>{$('#review-eligibility').textContent=error.message;});
})();
