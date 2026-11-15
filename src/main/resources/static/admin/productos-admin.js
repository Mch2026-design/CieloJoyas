'use strict';
(() => {
  const {api,esc,money,busy}=Cielo;
  const find=s=>document.querySelector(s),form=find('#product-form'),dialog=find('#product-dialog'),removeDialog=find('#delete-product-dialog');
  let products=[],originalValues='',trigger=null,deleting=null,categoryInitialized=false;
  const params=new URLSearchParams(location.search);
  const search=find('#product-search'),category=find('#product-category-filter'),status=find('#product-status-filter');
  search.value=params.get('q')||'';status.value=params.get('estado')||'';
  const icon=kind=>`<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">${kind==='edit'?'<path d="m16 3 5 5-12 12-6 1 1-6L16 3ZM13 6l5 5"/>':'<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7"/>'}</svg>`;
  const snapshot=()=>JSON.stringify([...new FormData(form)]);
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
    trigger=button;form.reset();form.elements.id.value='';find('#product-error').textContent='';
    if(product)for(const [name,value]of Object.entries(product)){const input=form.elements.namedItem(name);if(!input)continue;if(input.type==='checkbox')input.checked=!!value;else input.value=value??'';}
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
    const body=Object.fromEntries(new FormData(form));body.active=form.elements.active.checked;body.customizable=form.elements.customizable.checked;
    error.textContent='';
    await busy(button,async()=>{button.textContent='Guardando…';find('#cancel-product').disabled=true;find('#close-product').disabled=true;
      try{await api('/admin/products',{method:'POST',body});setProducts(await api('/admin/products'));originalValues=snapshot();dialog.close();announce(body.id?'Producto actualizado.':'Producto agregado a la colección.');}
      catch(err){error.textContent=err.message;error.focus();}
      finally{button.textContent=label;find('#cancel-product').disabled=false;find('#close-product').disabled=false;}
    });
  });
  search.addEventListener('input',updateFilters);category.addEventListener('change',updateFilters);status.addEventListener('change',updateFilters);
  window.ProductosAdmin={setProducts};
})();
