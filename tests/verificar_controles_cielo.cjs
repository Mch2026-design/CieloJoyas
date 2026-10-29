'use strict';
const {chromium,webkit}=require('/Users/flavio/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs=require('node:fs');
const ROOT=process.env.API_URL||'http://127.0.0.1:18083';
if(process.env.CIELO_TEST_ALLOW_MUTATIONS!=='1')throw Error('Usar exclusivamente una base temporal y CIELO_TEST_ALLOW_MUTATIONS=1');
(async()=>{
 const checks=[],errors=[],adminEmail='controles-admin@example.test',password='PruebaControles2026';
 function ok(value,label){if(!value)throw Error(label);checks.push(label);console.log('OK:',label);}
 for(const engine of ['chrome','webkit']){
  const browser=await(engine==='chrome'?chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true}):webkit.launch({headless:true}));
  const page=await browser.newPage({viewport:{width:1440,height:1000}});page.on('pageerror',e=>errors.push(engine+': '+e.message));page.on('dialog',d=>{errors.push('Diálogo del navegador: '+d.type());d.dismiss();});
  const go=name=>page.goto(ROOT+'/'+name+'.html',{waitUntil:'domcontentloaded'});
  async function choose(id,name){await page.locator('#'+id+'-control').click();await page.getByRole('option',{name,exact:typeof name==='string'}).click();}
  await go('catalogo');await page.locator('#sort-products-control').waitFor();
  ok(await page.locator('.whatsapp-float').isVisible(),engine+': visitante ve WhatsApp en escritorio');
  await page.locator('#sort-products-control').focus();await page.keyboard.press('ArrowDown');await page.keyboard.press('End');await page.keyboard.press('Enter');
  ok(await page.locator('#sort-products').inputValue()==='date-desc',engine+': selector operable con teclado');
  await choose('sort-products','Precio, menor a mayor');
  ok(await page.locator('#sort-products').inputValue()==='price-asc',engine+': ordenamiento usa el selector de Cielo');
  await page.locator('#sort-products-control').click();await page.screenshot({path:'verificacion/selector-'+engine+'.png'});await page.keyboard.press('Escape');
  ok(await page.locator('#sort-products-control').getAttribute('aria-expanded')==='false',engine+': Escape cierra el desplegable');
  const products=await page.evaluate(()=>Cielo.api('/products'));
  await page.locator('[data-add]:not([disabled])').first().click();await page.locator('#open-cart').click();await page.locator('#checkout').click();
  await page.locator('.checkout-submit').click();
  ok(await page.locator('.cielo-form-errors').isVisible(),engine+': errores propios al confirmar campos vacíos');
  ok(await page.locator('[name=phone]').getAttribute('aria-invalid')==='true',engine+': teléfono marcado como inválido');
  ok(await page.locator('[name=address]').getAttribute('aria-invalid')==='true',engine+': dirección marcada como inválida');
  await page.locator('[name=email]').fill(engine+'-compra@example.test');await page.locator('[name=firstName]').fill('Lucía');await page.locator('[name=lastName]').fill('Prueba');await page.locator('[name=documentId]').fill('12345678');await page.locator('[name=address]').fill('Av. Pruebas 123');await page.locator('[name=phone]').fill('987654321');
  await choose('department',/^Lima$/i);ok(!(await page.locator('#province-control').isDisabled()),engine+': departamento habilita provincias');
  await choose('province',/^Lima$/i);await choose('district',/^Lima$/i);ok(await page.locator('#district').inputValue()==='150101',engine+': distrito conserva su ubigeo');
  await page.locator('[name=paymentMethod][value=plin]').check();ok(await page.locator('#checkout-selected-payment').textContent()==='Plin',engine+': medio seleccionado aparece en el resumen');
  const invalidPhone=page.locator('[name=phone]');await invalidPhone.fill('123');await page.locator('.checkout-submit').click();ok((await page.locator('#'+await invalidPhone.getAttribute('data-cielo-error')).textContent()).includes('9 dígitos'),engine+': mensaje específico de teléfono');await invalidPhone.fill('987654321');
  let sent=0,firstKey;let release;const gate=new Promise(r=>release=r);
  await page.route('**/api/orders',async route=>{if(route.request().method()!=='POST')return route.continue();sent++;firstKey=route.request().postDataJSON().requestId;await gate;await route.continue();});
  await page.locator('.checkout-submit').click();await page.waitForFunction(()=>document.querySelector('.checkout-submit').disabled);
  await page.evaluate(()=>{const f=document.querySelector('#checkout-form');f.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));f.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));});
  ok(await page.locator('.checkout-processing').isVisible(),engine+': muestra Procesando… durante la petición');ok(await page.locator('#close-checkout').isDisabled(),engine+': no se cierra el checkout durante el envío');
  await page.screenshot({path:'verificacion/procesando-'+engine+'.png'});release();await page.locator('#pedido-dialog').waitFor();
  ok(sent===1,engine+': varios envíos rápidos generan una sola petición');
  ok((await page.locator('#pedido-title').textContent()).includes('Pedido confirmado'),engine+': confirmación con número de pedido');
  ok((await page.locator('#pedido-email-note').textContent()).includes('Enviamos el aviso'),engine+': aviso basado en envío SMTP aceptado');
  ok((await page.locator('.pedido-status').textContent()).includes('Pendiente de pago'),engine+': registrar pedido no simula pago confirmado');
  const orderId=Number((await page.locator('#pedido-reference').textContent()).match(/#(\d+)/)[1]);
  const existing=await page.evaluate(async id=>Cielo.api('/orders/'+id),orderId);ok(existing.emailSent===true,engine+': correo enviado queda guardado en el pedido');
  await page.locator('#pedido-done').click();await page.unroute('**/api/orders');
  // Reintento de la misma solicitud devuelve el pedido existente sin un segundo aviso.
  const repeated=await page.evaluate(async({key,productId,email})=>Cielo.api('/orders',{method:'POST',body:{requestId:key,email,firstName:'Lucía',lastName:'Prueba',phone:'987654321',documentType:'dni',documentId:'12345678',addressLine:'Av. Pruebas 123',ubigeo:'150101',paymentMethod:'plin',items:[{productId,quantity:1}]}}),{key:firstKey,productId:products[0].id,email:engine+'-compra@example.test'});
  ok(repeated.id===orderId&&repeated.emailSent,engine+': reintento conserva el mismo pedido y aviso');
  // Un fallo de red conserva el carrito, la dirección y la misma clave para reintentar.
  await go('catalogo');await page.locator('[data-add]:not([disabled])').first().click();await page.locator('#open-cart').click();await page.locator('#checkout').click();
  await page.locator('[name=email]').fill(engine+'-error@example.test');await page.locator('[name=firstName]').fill('Lucía');await page.locator('[name=lastName]').fill('Prueba');await page.locator('[name=documentId]').fill('12345678');await page.locator('[name=address]').fill('Av. Error 456');await page.locator('[name=phone]').fill('987654321');await choose('department',/^Lima$/i);await choose('province',/^Lima$/i);await choose('district',/^Lima$/i);
  let failedKey;await page.route('**/api/orders',route=>{failedKey=route.request().postDataJSON().requestId;route.abort();});await page.locator('.checkout-submit').click();await page.waitForFunction(()=>!!document.querySelector('#checkout-error').textContent);
  ok(!(await page.locator('.checkout-submit').isDisabled())&&await page.locator('[name=address]').inputValue()==='Av. Error 456',engine+': fallo de red conserva datos y permite reintentar');await page.unroute('**/api/orders');
  let retryKey;await page.route('**/api/orders',route=>{retryKey=route.request().postDataJSON().requestId;route.continue();});await page.locator('.checkout-submit').click();await page.locator('#pedido-dialog').waitFor();ok(failedKey===retryKey,engine+': el reintento reutiliza la clave idempotente');await page.locator('#pedido-done').click();await page.unroute('**/api/orders');
  const session=await page.evaluate(()=>Cielo.session(true));if(session.needsAdmin)await page.evaluate(({email,password})=>Cielo.api('/auth/setup',{method:'POST',body:{firstName:'Admin',lastName:'Controles',email,password,passwordConfirmation:password}}),{email:adminEmail,password});else await page.evaluate(({email,password})=>Cielo.api('/auth/login',{method:'POST',body:{email,password}}),{email:adminEmail,password});
  for(const name of ['productos','admin-pedidos','admin-reportes','admin-compras','admin-posventa','admin-promociones']){
   await go(name);await page.waitForFunction(()=>window.CieloControls);ok(await page.locator('.whatsapp-float').count()===0,engine+': WhatsApp ausente de '+name);
   await page.waitForFunction(()=>document.querySelector('#dashboard')&&!document.querySelector('#dashboard').hidden);
   ok(await page.locator('select:not([hidden])').count()===0,engine+': sin selectores nativos visibles en '+name);
  }
  await go('admin-pedidos');await page.locator('[data-status]').first().waitFor({state:'attached'});const status=page.locator('[data-status]').first();const id=await status.getAttribute('data-status');
  await page.locator('.cielo-select[data-order-state]').first().locator('.cielo-select-trigger').click();await page.getByRole('option',{name:'pagado',exact:true}).click();await page.locator('.cielo-confirm').waitFor();
  ok((await page.locator('.cielo-confirm').textContent()).includes('verificaste el pago'),engine+': confirmación de estado con diseño Cielo');await page.locator('.cielo-confirm').getByRole('button',{name:'Cancelar',exact:true}).click();
  await page.waitForFunction(id=>document.querySelector('[data-status="'+id+'"]').value==='pendiente',id);
  ok(await page.locator('[data-status="'+id+'"]').inputValue()==='pendiente',engine+': cancelar conserva el estado original');
  await page.locator('[data-status="'+id+'"]').locator('..').locator('.cielo-select-trigger').click();await page.getByRole('option',{name:'pagado',exact:true}).click();await page.locator('.cielo-confirm').getByRole('button',{name:'Confirmar',exact:true}).click();await page.waitForFunction(id=>document.querySelector('[data-status="'+id+'"]')?.dataset.orderState==='pagado',id);ok(true,engine+': confirmar guarda el estado y actualiza su color');
  await go('admin-posventa');await page.locator('[data-edit-review]').first().click();await page.locator('#admin-review-photo').setInputFiles('src/main/resources/static/assets/pago-yape-plin.png');
  ok((await page.locator('.cielo-file-name').textContent()).includes('pago-yape-plin.png'),engine+': selección de archivos usa el componente propio');
  await page.locator('#cancel-admin-review').click();await page.locator('.cielo-confirm').getByRole('button',{name:'Confirmar',exact:true}).click();await page.locator('#admin-review-dialog').waitFor({state:'hidden'});ok(!(await page.locator('#admin-review-dialog').isVisible()),engine+': descartar foto usa confirmación propia');
  await page.locator('[data-edit-review]').first().click();await page.locator('#admin-review-text').fill('Reseña revisada en la prueba de controles propios de Cielo y guardada desde Posventa.');await page.locator('#save-admin-review').click();await page.locator('#admin-review-dialog').waitFor({state:'hidden'});ok(true,engine+': guardar reseña funciona con validación propia');
  await go('admin-compras');await page.evaluate(()=>Cielo.api('/admin/vendors',{method:'POST',body:{name:'Proveedor Controles '+Date.now(),contact:'proveedor@example.test',material:'Acero',notes:'Prueba temporal',active:true}}));await page.reload({waitUntil:'domcontentloaded'});await page.waitForFunction(()=>document.querySelector('#purchase-form [name=vendorId]').options.length>1);await page.locator('#purchase-form .cielo-select-trigger').first().click();ok(await page.getByRole('option').count()>1,engine+': proveedor dinámico aparece en selector propio');await page.keyboard.press('Escape');
  await go('admin-promociones');await page.locator('.cielo-date-trigger').click();ok(await page.locator('.cielo-calendar').isVisible(),engine+': calendario propio para promociones');await page.locator('.cielo-calendar-grid button').nth(10).click();await page.getByRole('button',{name:'Listo',exact:true}).click();ok((await page.locator('[name=expiresAt]').inputValue()).includes('T'),engine+': calendario conserva fecha ISO del formulario');
  for(const viewport of [{width:375,height:812},{width:812,height:375}]){
   await page.setViewportSize(viewport);await go('catalogo');await page.locator('#sort-products-control').click();ok(await page.locator('.cielo-options:visible').evaluate(el=>{const r=el.getBoundingClientRect();return r.left>=0&&r.right<=innerWidth+1&&r.top>=0&&r.bottom<=innerHeight+1;}),engine+': desplegable encuadrado a '+viewport.width+' × '+viewport.height);await page.keyboard.press('Escape');
  }
  await page.emulateMedia({reducedMotion:'reduce'});await go('catalogo');await page.locator('#sort-products-control').focus();await page.keyboard.press('ArrowDown');ok(await page.getByRole('option').count()>0,engine+': funciona con movimiento reducido');
  await browser.close();
 }
 ok(errors.length===0,'Sin errores JavaScript ni diálogos nativos: '+errors.join('; '));fs.writeFileSync('verificacion/controles-cielo.json',JSON.stringify({result:'OK',checks:checks.length,tests:checks},null,2));
})().catch(error=>{console.error(error);process.exit(1);});
