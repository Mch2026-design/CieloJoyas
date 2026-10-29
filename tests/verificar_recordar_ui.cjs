const {chromium}=require('/Users/flavio/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs=require('node:fs');const ROOT=process.env.API_URL||'http://127.0.0.1:18081';
if(process.env.CIELO_TEST_ALLOW_MUTATIONS!=='1')throw Error('Solo base temporal');
(async()=>{
 const checks=[],errors=[];const ok=(v,label)=>{if(!v)throw Error(label);checks.push(label);console.log('OK:',label)};
 const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true,args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:1440,height:900}});page.on('pageerror',e=>errors.push(e.message));
 await page.goto(ROOT+'/catalogo.html');await page.locator('[data-add]:not([disabled])').first().waitFor();
 const email='recordar-ui-'+Date.now()+'@example.test';await page.evaluate(email=>Cielo.api('/auth/register',{method:'POST',body:{firstName:'Lucía',lastName:'Pruebas',email,password:'CieloPrueba2026Local'}}),email);await page.reload();
 async function checkout(){await page.locator('[data-add]:not([disabled])').first().click();await page.locator('#open-cart').click();await page.locator('#checkout').click();await page.locator('#checkout-modal').waitFor({state:'visible'});}
 await checkout();ok(await page.locator('#remember-delivery').isEnabled()&&!await page.locator('#remember-delivery').isChecked(),'Recuérdame disponible y sin marcar por defecto');
 await page.locator('[name="documentId"]').fill('12345678');await page.locator('[name="address"]').fill('Av. Mi Entrega 789');await page.locator('[name="phone"]').fill('987654321');
 await page.locator('#department').selectOption({label:'LIMA'},{force:true});await page.locator('#province').selectOption({label:'LIMA'},{force:true});await page.locator('#district').selectOption('150101',{force:true});await page.locator('[name="postal"]').fill('15001');await page.locator('#remember-delivery').check();
 await page.locator('.checkout-submit').click();await page.locator('#pedido-dialog').waitFor({state:'visible'});
 ok(await page.evaluate(async()=>(await Cielo.api('/account/delivery')).address==='Av. Mi Entrega 789'),'Compra desde el formulario guarda los datos marcados');
 const order=await page.evaluate(async()=>{const rows=await Cielo.api('/account/orders');return rows[0]});
 async function settled(){await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));}
 for(const viewport of [{width:1440,height:900},{width:1366,height:768},{width:1280,height:640}]){
  await page.setViewportSize(viewport);await settled();
  const metrics=await page.locator('#pedido-dialog').evaluate(d=>Object.fromEntries([d,...d.querySelectorAll('.pedido-dialog-body,.pedido-summary,.pedido-payment')].map(el=>[el.className,{height:el.clientHeight,scroll:el.scrollHeight}])));
  ok(Object.values(metrics).every(m=>m.scroll<=m.height+2),'Escritorio sin contenido recortado ni scroll '+viewport.width+' × '+viewport.height+' '+JSON.stringify(metrics));
  await page.mouse.wheel(0,500);ok(await page.locator('#pedido-content').evaluate(el=>el.scrollTop===0),'Ventana fija al intentar desplazar en escritorio');
  await page.locator('[data-detail-view="delivery"]').click();ok(await page.locator('[data-receipt]').isVisible(),'Entrega y descarga PDF accesibles sin scroll');await page.locator('[data-detail-view="items"]').click();
  if(viewport.width===1366)await page.screenshot({path:'verificacion/pedido-fijo-escritorio.png'});
 }
 // Comprueba la presentación de un pedido largo sin crear compras ficticias adicionales.
 await page.evaluate(order=>CieloPedidos.open({...order,items:Array.from({length:12},(_,i)=>({...order.items[0],name:'Pieza especial '+(i+1)}))}),order);await settled();
 ok(await page.locator('.pedido-items li:visible').count()<=3 && await page.locator('#pedido-items-next').isEnabled(),'Pedido largo usa paginación en escritorio');await page.locator('#pedido-items-next').click();ok((await page.locator('#pedido-items-page').textContent()).startsWith('2 de'),'Paginación muestra las siguientes piezas');
 await page.evaluate(order=>CieloPedidos.open(order),order);
 for(const viewport of [{width:375,height:812},{width:320,height:640},{width:812,height:375}]){
  await page.setViewportSize(viewport);await settled();
  ok(await page.locator('#pedido-content').evaluate(el=>getComputedStyle(el).overflowY==='auto' && el.scrollHeight>el.clientHeight),'Móvil permite desplazamiento '+viewport.width+' × '+viewport.height);
  ok(await page.locator('#pedido-content').evaluate(el=>el.querySelector('.pedido-payment').offsetTop<el.querySelector('.pedido-summary').offsetTop),'QR y medio de pago aparecen primero en móvil');
  ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Sin desbordamiento horizontal móvil');
  if(viewport.width===375)await page.screenshot({path:'verificacion/pedido-qr-movil.png'});
 }
 await page.setViewportSize({width:1440,height:900});await page.locator('#pedido-done').click();await checkout();
 ok(await page.locator('[name="address"]').inputValue()==='Av. Mi Entrega 789'&&await page.locator('#district').inputValue()==='150101','Siguiente compra autocompleta dirección y ubicación');
 ok(!await page.locator('#remember-delivery').isChecked()&&await page.locator('#forget-delivery').isVisible(),'El usuario controla nuevas actualizaciones y puede olvidar los datos');
 await page.locator('#forget-delivery').click();await page.waitForFunction(()=>document.querySelector('#forget-delivery').hidden);ok(!await page.evaluate(async()=>(await Cielo.api('/account/delivery')).saved),'Olvidar datos funciona desde el checkout');
 await page.locator('#close-checkout').click();await page.evaluate(()=>Cielo.api('/auth/logout',{method:'POST'}));await page.reload();await checkout();
 ok(await page.locator('#remember-delivery').isDisabled()&&await page.locator('[name="address"]').inputValue()==='','Visitante no hereda los datos de la cuenta anterior');
 ok(errors.length===0,'Sin errores JavaScript: '+errors.join(', '));fs.writeFileSync('verificacion/navegador-recordar.json',JSON.stringify({checks:checks.length,result:'OK',tests:checks},null,2));await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
