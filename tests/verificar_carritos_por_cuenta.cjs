'use strict';
// Sesiones y compras simuladas: no crea cuentas ni pedidos en Supabase.
const {chromium,webkit}=require('/Users/flavio/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const assert=require('node:assert/strict');
const base=process.env.API_URL||'http://127.0.0.1:18088';
(async()=>{
 for(const engine of [chromium,webkit]){
  const browser=await engine.launch(engine===chromium?{executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true}:{headless:true});
  const context=await browser.newContext();let user=null;const errors=[];
  const users={A:{id:101,email:'a@example.test',name:'Ana',firstName:'Ana',role:'CLIENTE'},B:{id:202,email:'b@example.test',name:'Bea',firstName:'Bea',role:'CLIENTE'}};
  const product=id=>({id,name:'Pieza '+id,reference:'P'+id,categoryId:1,category:'Collares',price:20,stock:20,salesCount:0,createdAt:'2026-01-01T00:00:00Z',src:'/assets/certificado-material.png',alt:'Imagen de prueba',material:'Acero',active:true});
  await context.route('**/api/**',async route=>{
   const path=new URL(route.request().url()).pathname;
   if(path==='/api/auth/me')return route.fulfill({json:{user,csrf:'prueba',needsAdmin:false}});
   if(path==='/api/auth/login'){user=users[route.request().postDataJSON().account];return route.fulfill({json:{user,csrf:'prueba'}});}
   if(path==='/api/auth/logout'){user=null;return route.fulfill({json:{ok:true}});}
   if(path==='/api/products')return route.fulfill({json:[product(1),product(2)]});
   if(path==='/api/categories')return route.fulfill({json:[{id:1,name:'Collares'}]});
   if(path==='/api/config')return route.fulfill({json:{}});
   if(path==='/api/coupons/validate')return route.fulfill({json:{code:'CIELO10',discount:10}});
   if(path==='/api/account/delivery')return route.fulfill({json:{saved:false}});
   return route.fulfill({status:400,json:{error:'Operación no prevista en esta prueba'}});
  });
  const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{if(!sessionStorage.getItem('legacy-seeded')){localStorage.setItem('cielo-cart-v2',JSON.stringify([{key:'old',productId:1,quantity:9,engraving:'Antiguo',size:''}]));sessionStorage.setItem('cielo-coupon','LEGACY');sessionStorage.setItem('legacy-seeded','1');}});
  async function ready(p=page){await p.locator('[data-add="1"]').waitFor();}
  async function count(n,p=page){await p.waitForFunction(value=>document.querySelector('#bag-count').textContent===String(value),n);}
  async function go(){await page.goto(base+'/catalogo.html');await ready();}
  async function login(account){await page.evaluate(account=>Cielo.api('/auth/login',{method:'POST',body:{account}}),account);await go();}
  await go();await count(0); // Carrito viejo sin dueño no se atribuye al visitante.
  await page.locator('[data-add="2"]').click();await count(1);
  await login('A');await count(0); // No hereda la compra del visitante.
  await page.locator('[data-add="1"]').click();await count(1);
  await page.locator('#open-cart').click();await page.locator('#cart-panel.open').waitFor();
  await page.locator('.gift-note summary').click();await page.locator('#gift-message').fill('Solo para Ana');
  await page.locator('#coupon').fill('CIELO10');await page.locator('#apply-coupon').click();await page.getByText('Cupón CIELO10: 10% de descuento.',{exact:true}).waitFor();
  await page.reload();await ready();await count(1);
  assert.equal(await page.locator('#gift-message').inputValue(),'Solo para Ana');
  const other=await context.newPage();other.on('pageerror',e=>errors.push(e.message));await other.goto(base+'/catalogo.html');await ready(other);await count(1,other);
  await page.evaluate(()=>Cielo.api('/auth/logout',{method:'POST'}));await go();await count(1); // Recupera solo el carrito visitante.
  await count(1,other);await other.waitForFunction(()=>cartOwner==='guest');
  assert.equal(await page.locator('#gift-message').inputValue(),'');
  await login('B');await count(0);await other.waitForFunction(()=>cartOwner==='user:202');await count(0,other);
  assert.equal(await page.locator('#gift-message').inputValue(),'');assert.equal(await page.locator('#coupon').inputValue(),'');
  await page.locator('[data-add="2"]').click();await page.locator('[data-add="2"]').click();await count(2);
  await page.locator('#open-cart').click();await page.locator('#cart-panel.open').waitFor();assert.equal(await page.locator('#cart-items h3').textContent(),'Pieza 2');
  await login('A');await count(1);assert.equal(await page.locator('#gift-message').inputValue(),'Solo para Ana');
  await page.locator('#open-cart').click();await page.locator('#cart-panel.open').waitFor();assert.equal(await page.locator('#cart-items h3').textContent(),'Pieza 1');
  await page.locator('#forget-cart').click();await count(0);
  await login('B');await count(2); // Vaciar A no altera B.
  await page.locator('#open-cart').click();await page.locator('#cart-panel.open').waitFor();await page.locator('#checkout').click();await page.locator('#checkout-modal.open').waitFor();
  assert.equal(await page.locator('#checkout-form [name=email]').inputValue(),'b@example.test');
  assert.deepEqual(errors,[]);await browser.close();console.log('OK '+engine.name()+': visitante, A, B, recarga, cupones, regalo, pestañas, checkout y vaciado independientes');
 }
})().catch(e=>{console.error(e);process.exit(1);});
