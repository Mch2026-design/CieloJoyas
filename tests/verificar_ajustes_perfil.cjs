'use strict';
const {chromium}=require('/Users/flavio/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const assert=require('node:assert/strict');
const root=process.env.API_URL||'http://127.0.0.1:18085';
if(process.env.CIELO_TEST_ALLOW_MUTATIONS!=='1')throw Error('Requiere base temporal autorizada');
(async()=>{
 const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
 const page=await browser.newPage({viewport:{width:1440,height:1000},reducedMotion:'reduce'}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 try{
  await page.goto(root+'/login.html');await page.locator('#access-email').fill('correo-invalido');await page.locator('#access-password').click();
  assert.equal(await page.locator('#error-email').textContent(),'Ingresa un correo electrónico válido.');assert.equal(await page.locator('.cielo-field-error').count(),0);
  console.log('OK: error de correo sin duplicados');
  const email='perfil-'+Date.now()+'@example.test';
  await page.evaluate(async email=>{await Cielo.session();await Cielo.api('/auth/register',{method:'POST',body:{firstName:'Ana',lastName:'Prueba',email,password:'CieloPrueba2026Local'}});},email);
  await page.goto(root+'/cuenta.html');const form=page.locator('#account-information-form');await form.waitFor({state:'visible'});
  await form.locator('[name=documentId]').fill('12345678');await form.locator('[name=address]').fill('Av. Perfil 123');await form.locator('[name=phone]').fill('987654321');
  async function choose(id,text){await page.locator('#'+id+'-control').click();await page.getByRole('option',{name:new RegExp('^'+text+'$','i')}).click();}
  await choose('info-department','Lima');await choose('info-province','Lima');await choose('info-district','Lima');await form.locator('[type=submit]').click();
  await page.waitForFunction(()=>document.querySelector('#account-information-status').textContent.includes('se guardó'));
  await page.reload();await form.waitFor({state:'visible'});assert.equal(await form.locator('[name=address]').inputValue(),'Av. Perfil 123');
  await form.locator('[name=address]').fill('Av. Editada 456');await form.locator('[type=submit]').click();await page.waitForFunction(()=>document.querySelector('#account-information-status').textContent.includes('se guardó'));
  console.log('OK: información editable y persistente en la cuenta');await page.screenshot({path:'verificacion/ajustes-mi-informacion.png',fullPage:true});
  await page.goto(root+'/catalogo.html');await page.locator('[data-add]:not([disabled])').first().click();await page.locator('#open-cart').click();
  for(const size of [{width:1440,height:1000},{width:390,height:844},{width:320,height:700}]){
   await page.setViewportSize(size);const dimensions=await page.locator('#cart-items').evaluate(el=>({height:el.clientHeight,panel:el.parentElement.clientHeight,overflow:el.parentElement.scrollWidth>el.parentElement.clientWidth}));assert(dimensions.height>=size.height*.35,JSON.stringify(dimensions));assert(!dimensions.overflow);console.log('OK: carrito adaptable '+size.width+' × '+size.height);
  }
  await page.screenshot({path:'verificacion/ajustes-carrito-movil.png'});await page.locator('#checkout').click();await page.locator('#checkout-modal').waitFor({state:'visible'});assert.equal(await page.locator('#checkout-form [name=address]').inputValue(),'Av. Editada 456');assert.equal(await page.locator('#district').inputValue(),'150101');console.log('OK: compra usa la información guardada');await page.screenshot({path:'verificacion/ajustes-checkout-movil.png'});
  await page.evaluate(()=>Cielo.api('/auth/logout',{method:'POST'}));await page.goto(root+'/login.html');
  await page.evaluate(async()=>{await Cielo.session();await Cielo.api('/auth/login',{method:'POST',body:{email:'admin-prueba@example.test',password:'CieloPrueba2026Local'}});});
  await page.setViewportSize({width:1440,height:1000});await page.goto(root+'/admin-pedidos.html');const state=page.locator('[data-status]').first();await state.waitFor({state:'attached'});const trigger=state.locator('..').locator('.cielo-select-trigger');assert(await trigger.evaluate(el=>el.readOnly));await trigger.click();assert(await page.getByRole('option').count()>0);await page.keyboard.press('Escape');console.log('OK: estados del pedido solo permiten seleccionar');
  await page.goto(root+'/admin-posventa.html');const reviewId=await page.locator('[data-edit-review]').first().getAttribute('data-edit-review');await page.locator('[data-edit-review="'+reviewId+'"]').click();const photo='https://images.unsplash.com/photo-1605100804763-247f67b3557e?perfil='+Date.now();await page.locator('#admin-review-photo-url').fill(photo);await page.locator('#save-admin-review').click();await page.locator('#admin-review-dialog').waitFor({state:'hidden'});await page.locator('[data-edit-review="'+reviewId+'"]').click();assert.equal(await page.locator('#admin-review-preview').getAttribute('src'),photo);console.log('OK: reseña guarda imagen mediante URL');
  assert.deepEqual(errors,[]);console.log('OK: sin errores JavaScript');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
