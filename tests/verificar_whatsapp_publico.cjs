const {chromium}=require('/Users/flavio/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs=require('node:fs');const ROOT=process.env.API_URL||'http://127.0.0.1:18081';
(async()=>{
 const checks=[],errors=[];const ok=(v,label)=>{if(!v)throw Error(label);checks.push(label);console.log('OK:',label)};
 const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true,args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:1440,height:900}});page.on('pageerror',e=>errors.push(e.message));
 for(const name of ['index','novedades','catalogo','personaliza','resenas','contacto','ofertas','login','registro','configurar-admin']){
  await page.goto(ROOT+'/'+name+'.html');await page.locator('.whatsapp-float').waitFor();ok(await page.locator('.whatsapp-float').count()===1,'WhatsApp público en '+name);
 }
 await page.goto(ROOT+'/index.html');ok(await page.locator('.whatsapp-float').getAttribute('href')==='https://wa.me/51900934669','Enlace al número 900 934 669');ok(await page.locator('.whatsapp-float').getAttribute('target')==='_blank','Contacto abre en pestaña nueva');
 for(const width of [1440,375,320]){
  await page.setViewportSize({width,height:900});
  if(width<=1000){ok(!(await page.locator('.whatsapp-float').isVisible()),'Contacto flotante oculto a '+width+' px');continue;}
  await page.evaluate(()=>scrollTo({top:0,behavior:'instant'}));const first=await page.locator('.whatsapp-float').boundingBox();await page.evaluate(()=>scrollTo({top:700,behavior:'instant'}));const last=await page.locator('.whatsapp-float').boundingBox();
  ok(first.x===last.x&&first.y===last.y,'Contacto fijo en escritorio');ok(last.x>=0&&last.x+last.width<=width,'Contacto encuadrado en escritorio');
 }
 await page.goto(ROOT+'/contacto.html');ok(await page.locator('.whatsapp-contact-card').isVisible(),'WhatsApp estático en Contacto móvil');
 await page.setViewportSize({width:1440,height:900});
 await page.evaluate(()=>Cielo.api('/auth/login',{method:'POST',body:{email:'prueba-cielo@example.test',password:'CieloCuenta2026Local'}}));await page.goto(ROOT+'/cuenta.html');await page.evaluate(()=>Cielo.session());ok(await page.locator('.whatsapp-float').count()===0,'Perfil excluido para cliente');await page.goto(ROOT+'/catalogo.html');await page.locator('.whatsapp-float').waitFor();ok(true,'Cliente ve el contacto al volver a la tienda');
 await page.evaluate(()=>Cielo.api('/auth/logout',{method:'POST'}));await page.evaluate(()=>Cielo.api('/auth/login',{method:'POST',body:{email:'admin-prueba@example.test',password:'CieloPrueba2026Local'}}));
 for(const name of ['productos','admin-pedidos','admin-reportes','admin-compras','admin-posventa','admin-promociones']){await page.goto(ROOT+'/'+name+'.html');ok(await page.locator('.whatsapp-float').count()===0,'WhatsApp excluido de '+name);}
 const offline=await browser.newContext();await offline.route('**/api/**',route=>route.abort());const p=await offline.newPage();await p.goto(ROOT+'/index.html');await p.locator('.whatsapp-float').waitFor();ok(await p.locator('.whatsapp-float').isVisible(),'Contacto visible aunque la API o sesión no respondan');await offline.close();
 ok(errors.length===0,'Sin errores JavaScript: '+errors.join(', '));fs.writeFileSync('verificacion/whatsapp-publico.json',JSON.stringify({checks:checks.length,result:'OK',tests:checks},null,2));await browser.close();
})().catch(error=>{console.error(error);process.exit(1)});
