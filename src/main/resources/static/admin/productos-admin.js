'use strict';
(() => {
  const {api,esc,money,busy}=Cielo;
  const find=s=>document.querySelector(s),form=find('#product-form'),dialog=find('#product-dialog'),removeDialog=find('#delete-product-dialog');
  const sizesEditor=find('#product-sizes-editor'),sizeRowsList=find('#size-rows-list'),addSizeBtn=find('#add-size-row');
  let products=[],originalValues='',trigger=null,deleting=null,categoryInitialized=false;
  const params=new URLSearchParams(location.search);
  const search=find('#product-search'),category=find('#product-category-filter'),status=find('#product-status-filter');
  search.value=params.get('q')||'';status.value=params.get('estado')||'';
  const icon=kind=>`<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">${kind==='edit'?'<path d="m16 3 5 5-12 12-6 1 1-6L16 3ZM13 6l5 5"/>':'<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7"/>'}</svg>`;
  const isRingSelected=()=>form.elements.categoryId.selectedOptions[0]?.textContent?.trim().toLocaleLowerCase('es')==='anillos';
  function cleanSizeInput(value){
    let clean=String(value||'').replace(/[^0-9.]/g,'');
    const parts=clean.split('.');
    if(parts.length>2)clean=parts[0]+'.'+parts.slice(1).join('');
    return clean.slice(0,4);
  }
  function collectSizeRows(){
    return [...sizeRowsList.querySelectorAll('.size-stock-row')].map(row=>({
      size:cleanSizeInput(row.querySelector('.size-input').value).trim(),
      stock:row.querySelector('.size-stock-input').value.trim()
    }));
  }
  function syncTotalFromSizes(){
    if(!sizesEditor||sizesEditor.hidden)return;
    const sum=collectSizeRows().reduce((acc,r)=>acc+Math.max(0,parseInt(r.stock,10)||0),0);
    form.elements.stock.value=String(sum);
  }
  function addSizeRow(size='',stock='0'){
    const row=document.createElement('div');
    row.className='size-stock-row';
    row.innerHTML=`<input class="size-input" type="text" inputmode="decimal" maxlength="4" placeholder="Ej. 6 o 6.5" value="${esc(size)}" aria-label="Talla (US)"><input class="size-stock-input" type="number" min="0" max="1000000" inputmode="numeric" placeholder="Stock" value="${esc(stock)}" aria-label="Stock para talla"><button type="button" class="row-action danger remove-size-row" aria-label="Quitar talla">Quitar</button>`;
    row.querySelector('.size-input').addEventListener('input',e=>{e.target.value=cleanSizeInput(e.target.value);});
    row.querySelector('.size-stock-input').addEventListener('input',syncTotalFromSizes);
    row.querySelector('.remove-size-row').addEventListener('click',()=>{row.remove();syncTotalFromSizes();});
    sizeRowsList.appendChild(row);
  }
  function renderSizeRows(sizes){
    sizeRowsList.innerHTML='';
    const list=Array.isArray(sizes)&&sizes.length?sizes:[{size:'4',stock:0},{size:'5',stock:0},{size:'6',stock:0},{size:'7',stock:0},{size:'8',stock:0}];
    for(const item of list)addSizeRow(String(item.size??''),String(item.stock??0));
    syncTotalFromSizes();
  }
  function syncSizeMode(initialSizes=null){
    const isRing=isRingSelected(),box=form.elements.requiresSize,hint=find('#ring-size-hint');
    if(!box)return;
    if(isRing){box.checked=true;box.disabled=true;}
    else box.disabled=false;
    if(hint)hint.hidden=!isRing;
    const active=box.checked;
    sizesEditor.hidden=!active;
    form.elements.stock.readOnly=active;
    if(active){
      if(initialSizes!==null||!sizeRowsList.children.length)renderSizeRows(initialSizes);
      else syncTotalFromSizes();
    }
  }
  const snapshot=()=>JSON.stringify([...new FormData(form),['__sizes',sizesEditor.hidden?'':JSON.stringify(collectSizeRows())]]);
  const announce=text=>{find('#admin-message').textContent=text;};
  function render(){
    const term=search.value.trim().toLocaleLowerCase('es');
    const rows=products.filter(p=>(!term||`${p.name} ${p.reference}`.toLocaleLowerCase('es').includes(term))&&(!category.value||String(p.categoryId)===category.value)&&(!status.value||(status.value==='visible'&&p.active)||(status.value==='hidden'&&!p.active)||(status.value==='empty'&&p.stock===0)));
    find('#product-count').textContent=`${rows.length} de ${products.length} productos · ${products.filter(p=>p.active).length} visibles · ${products.filter(p=>p.active&&p.stock===0).length} sin stock`;
    find('#product-list').innerHTML=rows.length?`<table class="inventory-table" role="table"><thead><tr><th scope="col">Producto</th><th scope="col">Categoría</th><th scope="col">Precio</th><th scope="col">Stock</th><th scope="col">Estado</th><th scope="col"><span class="sr-only">Acciones</span></th></tr></thead><tbody>${rows.map(p=>`<tr><th scope="row"><div class="inventory-product"><img src="${esc(p.src)}" alt="${esc(p.alt)}" width="64" height="76" loading="lazy"><div><b>${esc(p.name)}</b><small>${esc(p.reference)}</small></div></div></th><td data-label="Categoría">${esc(p.category)}</td><td data-label="Precio" class="inventory-price">${money(p.price)}</td><td data-label="Stock"><span class="stock-label ${p.stock===0?'stock-empty':''}">${p.stock} ${p.stock===1?'unidad':'unidades'}</span></td><td data-label="Estado"><span class="visibility-label ${p.active?'':'is-hidden'}">${p.active?'Visible':'Oculto'}</span></td><td class="inventory-row-actions"><button class="row-action" data-product="${p.id}" type="button" aria-label="Editar ${esc(p.name)}">${icon('edit')}Editar</button>${p.active?`<button class="row-action danger" data-hide="${p.id}" type="button" aria-label="Eliminar ${esc(p.name)}">${icon('delete')}Eliminar</button>`:''}</td></tr>`).join('')}</tbody></table>`:'<div class="inventory-empty"><h2>No hay productos para mostrar</h2><p>Prueba otra búsqueda o agrega una nueva pieza a la colección.</p><button class="export-button" id="clear-product-filters" type="button">Limpiar filtros</button></div>';
    find('#clear-product-filters')?.addEventListener('click',()=>{search.value='';category.value='';status.value='';updateFilters();search.focus();});
  }
  function updateFilters(){
    const url=new URL(location.href);
    for(const [key,value]of [['q',search.value.trim()],['categoria',category.value],['estado',status.value]]){if(value)url.searchParams.set(key,value);else url.searchParams.delete(key);}
    history.replaceState(null,'',url);render();
  }
  function setProducts(rows){
    products=rows;const selected=categoryInitialized?category.value:(params.get('categoria')||'');categoryInitialized=true;
    const categories=[...new Map(rows.map(p=>[p.categoryId,p.category])).entries()].sort((a,b)=>a[1].localeCompare(b[1],'es'));
    category.innerHTML='<option value="">Todas las categorías</option>'+categories.map(([id,name])=>`<option value="${id}">${esc(name)}</option>`).join('');category.value=selected;render();
  }
  function openProduct(product,button){
    trigger=button;form.reset();form.elements.id.value='';sizeRowsList.innerHTML='';find('#product-error').textContent='';
    if(product)for(const [name,value]of Object.entries(product)){const input=form.elements.namedItem(name);if(!input)continue;if(input.type==='checkbox')input.checked=!!value;else input.value=value??'';}
    syncSizeMode(product?.sizes??null);
    find('#product-dialog-title').textContent=product?'Editar producto':'Agregar producto';
    find('#save-product').textContent=product?'Guardar cambios':'Agregar producto';
    originalValues=snapshot();dialog.showModal();find('.product-dialog-body').scrollTop=0;form.elements.reference.focus({preventScroll:true});
  }
  async function closeProduct(){
    if(find('#save-product').disabled)return;
    if(snapshot()!==originalValues&&!(await CieloControls.confirm('Hay cambios sin guardar. ¿Quieres cerrar el formulario y descartarlos?')))return;
    dialog.close();
  }
  dialog.addEventListener('cancel',e=>{e.preventDefault();closeProduct();});
  dialog.addEventListener('close',()=>{(trigger?.isConnected?trigger:find('#add-product')).focus({preventScroll:true});});
  find('#add-product').addEventListener('click',e=>openProduct(null,e.currentTarget));
  find('#close-product').addEventListener('click',closeProduct);find('#cancel-product').addEventListener('click',closeProduct);
  addSizeBtn?.addEventListener('click',()=>{addSizeRow('','0');sizeRowsList.lastElementChild?.querySelector('.size-input')?.focus();});
  find('#product-list').addEventListener('click',e=>{
    const edit=e.target.closest('[data-product]'),remove=e.target.closest('[data-hide]');
    if(edit)openProduct(products.find(p=>p.id===Number(edit.dataset.product)),edit);
    if(remove){deleting=products.find(p=>p.id===Number(remove.dataset.hide));trigger=remove;find('#delete-product-description').textContent=`¿Quieres eliminar «${deleting.name}» del catálogo?`;find('#delete-product-error').textContent='';removeDialog.showModal();find('#cancel-delete-product').focus();}
  });
  removeDialog.addEventListener('close',()=>{(trigger?.isConnected?trigger:find('#add-product')).focus({preventScroll:true});});
  removeDialog.addEventListener('cancel',e=>{if(find('#confirm-delete-product').disabled)e.preventDefault();});
  find('#cancel-delete-product').addEventListener('click',()=>removeDialog.close());
  find('#confirm-delete-product').addEventListener('click',async e=>{
    const button=e.currentTarget;
    await busy(button,async()=>{find('#cancel-delete-product').disabled=true;try{await api('/admin/products/'+deleting.id,{method:'DELETE'});setProducts(await api('/admin/products'));removeDialog.close();announce('Producto eliminado del catálogo. Sus pedidos anteriores se conservaron.');}catch(error){find('#delete-product-error').textContent=error.message;}finally{find('#cancel-delete-product').disabled=false;}});
  });
  form.addEventListener('submit',async e=>{
    e.preventDefault();const error=find('#product-error'),button=find('#save-product'),label=button.textContent;
    const body=Object.fromEntries(new FormData(form));body.active=form.elements.active.checked;body.requiresSize=isRingSelected()||form.elements.requiresSize.checked;body.engravable=form.elements.engravable.checked;body.customizable=body.requiresSize||body.engravable;
    if(body.requiresSize){
      const rows=collectSizeRows();
      if(!rows.length){error.textContent='Agrega al menos una talla con stock.';error.focus();return;}
      const seen=new Set();let total=0;
      for(const r of rows){
        if(!/^[0-9]{1,2}(?:\.[0-9])?$/.test(r.size)){error.textContent=`Talla inválida (${r.size||'vacía'}). Usa números como 4, 5 o 6.5.`;error.focus();return;}
        if(seen.has(r.size)){error.textContent=`La talla ${r.size} está repetida.`;error.focus();return;}
        const st=Number(r.stock);if(!Number.isInteger(st)||st<0||st>1000000||r.stock===''){error.textContent=`Stock inválido para la talla ${r.size}.`;error.focus();return;}
        seen.add(r.size);total+=st;
      }
      body.sizes=rows.map(r=>({size:r.size,stock:Number(r.stock)}));body.stock=total;
    }
    error.textContent='';
    await busy(button,async()=>{button.textContent='Guardando…';find('#cancel-product').disabled=true;find('#close-product').disabled=true;
      try{await api('/admin/products',{method:'POST',body});setProducts(await api('/admin/products'));originalValues=snapshot();dialog.close();announce(body.id?'Producto actualizado.':'Producto agregado a la colección.');}
      catch(err){error.textContent=err.message;error.focus();}
      finally{button.textContent=label;find('#cancel-product').disabled=false;find('#close-product').disabled=false;}
    });
  });
  form.elements.categoryId?.addEventListener('change',()=>syncSizeMode());
  form.elements.requiresSize?.addEventListener('change',()=>syncSizeMode());
  search.addEventListener('input',updateFilters);category.addEventListener('change',updateFilters);status.addEventListener('change',updateFilters);
  window.ProductosAdmin={setProducts};
})();
