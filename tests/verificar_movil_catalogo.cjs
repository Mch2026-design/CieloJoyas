'use strict';
const {chromium}=require('/Users/flavio/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs=require('node:fs');
const ROOT=process.env.API_URL||'http://127.0.0.1:18083';
(async()=>{
 const checks=[],errors=[];
 function ok(condition,label){if(!condition)throw Error(label);checks.push(label);console.log('OK:',label);}
 const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
 const page=await browser.newPage();page.on('pageerror',e=>errors.push(e.message));
 const settle=()=>page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
 await page.goto(ROOT+'/catalogo.html');
 const all=await page.evaluate(()=>Cielo.api('/products')),fresh=all.filter(p=>/^[A-Z]{3}-02-\d{2}$/.test(p.reference));
 ok(fresh.length===60,'API devuelve los 60 productos de migración 02');
 ok(fresh.filter(p=>p.customizable).length===30,'Exactamente 30 productos nuevos personalizables');
 ok(new Set(fresh.map(p=>p.src.split('?')[0])).size===60,'60 fotografías diferentes, sin contar parámetros de URL');
 ok(fresh.every(p=>p.price>=40&&p.price<=250),'Precios dentro de S/ 40 a S/ 250');
 for(const category of ['Anillos','Collares','Pulseras','Aretes','Tobilleras','Sets']){
  ok(fresh.filter(p=>p.category===category).length===10,'Diez productos nuevos en '+category);
  await page.goto(ROOT+'/catalogo.html?categoria='+encodeURIComponent(category));
  await page.waitForFunction(()=>document.querySelectorAll('.product-card').length>0);
  ok(await page.locator('.product-card').count()===all.filter(p=>p.category===category).length,'Filtro real de '+category);
 }
 await page.goto(ROOT+'/personaliza.html');await page.waitForFunction(()=>document.querySelectorAll('#product-grid .product-card').length>0);
 ok(await page.locator('#product-grid .product-card').count()===all.filter(p=>p.customizable).length,'Personaliza incluye todas las piezas configurables');
 await page.goto(ROOT+'/catalogo.html?min=100&max=250');await page.waitForFunction(()=>document.querySelectorAll('.product-card').length>0);
 ok(await page.locator('.product-card').count()===all.filter(p=>p.price>=100&&p.price<=250).length,'Filtro de precio con sets de hasta S/ 239');
 const custom=fresh.find(p=>p.reference==='COL-02-01');
 await page.goto(ROOT+'/catalogo.html?q='+encodeURIComponent(custom.name));await page.locator(`[data-view="${custom.id}"]`).first().click();
 ok(await page.locator('#product-engraving').isVisible(),'Detalle permite introducir la personalización');
 await page.locator('#product-engraving').fill('María');await page.locator('#product-size').fill('45 cm');await page.locator('#product-modal-add').click();
 await page.goto(ROOT+'/contacto.html');await page.locator('#open-cart').click();
 ok((await page.locator('#cart-items').textContent()).includes('Grabado: María'),'Grabado conservado al cambiar de sección');
 ok((await page.locator('#cart-items').textContent()).includes('45 cm'),'Medida conservada al cambiar de sección');
 await page.locator('#close-cart').click();
 for(const viewport of [{width:320,height:900},{width:375,height:812},{width:600,height:900},{width:812,height:375},{width:1000,height:900},{width:1440,height:900}]){
  await page.setViewportSize(viewport);await page.goto(ROOT+'/index.html');await page.waitForFunction(()=>document.querySelector('.nav-preview'));
  ok(await page.locator('.whatsapp-float').isVisible()===(viewport.width>1000),'Visibilidad de WhatsApp a '+viewport.width+' px');
  if(viewport.width<=1000){
   await page.locator('#menu-toggle').click();await settle();
   ok(await page.locator('.nav-preview').isVisible(),'Espacio del menú ocupado por la colección a '+viewport.width+' px');
   ok(await page.locator('#nav>a:not(.nav-preview)').evaluateAll(els=>els.every(el=>el.getBoundingClientRect().height>=56)),'Accesos de 56 px a '+viewport.width+' px');
   ok(await page.locator('#nav').evaluate(el=>el.getBoundingClientRect().bottom<=innerHeight),'Menú dentro del alto de pantalla a '+viewport.width+' px');
   if(viewport.width===375){await page.locator('.nav-preview img').evaluate(img=>img.decode().catch(()=>{}));await page.screenshot({path:'verificacion/menu-movil-final.png'});}
   await page.keyboard.press('Escape');ok(await page.locator('#menu-toggle').getAttribute('aria-expanded')==='false','Escape cierra el menú');
  }
  ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Sin desborde horizontal a '+viewport.width+' px');
  await page.goto(ROOT+'/contacto.html');
  ok(await page.locator('.whatsapp-contact-card').isVisible(),'Tarjeta estática disponible en Contacto a '+viewport.width+' px');
  ok(await page.locator('.whatsapp-contact-card').evaluate(el=>getComputedStyle(el).position==='static'),'Contacto dentro del flujo a '+viewport.width+' px');
  await page.goto(ROOT+'/catalogo.html');await page.locator('#open-filters').click();await page.locator('#apply-price').scrollIntoViewIfNeeded();await settle();
  ok(await page.locator('#apply-price').evaluate(el=>{const r=el.getBoundingClientRect(),footer=document.querySelector('.filter-drawer-footer').getBoundingClientRect();return r.top>=0&&r.bottom<=footer.top+1;}),'Aplicar precio accesible sobre el botón fijo a '+viewport.width+' px');
  if(viewport.width===375)await page.screenshot({path:'verificacion/filtros-movil-final.png'});
  await page.locator('#close-filters').click();
 }
 await page.emulateMedia({reducedMotion:'reduce'});await page.setViewportSize({width:375,height:812});await page.goto(ROOT+'/contacto.html');
 ok(await page.locator('.whatsapp-contact-card').getAttribute('href')==='https://wa.me/51900934669','Número de WhatsApp correcto');
 await page.screenshot({path:'verificacion/contacto-movil-final.png'});
 ok(errors.length===0,'Sin errores JavaScript: '+errors.join(', '));
 fs.writeFileSync('verificacion/movil-catalogo-02.json',JSON.stringify({result:'OK',checks:checks.length,tests:checks},null,2));await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
