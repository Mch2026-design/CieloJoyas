'use strict';
// Navegador real con API simulada: no registra pedidos ni envía correos reales.
const {chromium,webkit}=require('/Users/flavio/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const path=require('node:path');
const root=path.resolve(__dirname,'../src/main/resources/static');
const base='http://127.0.0.1:18088';
const types={'.html':'text/html','.js':'application/javascript','.css':'text/css','.json':'application/json','.png':'image/png'};

(async()=>{
 for(const engine of [chromium,webkit]){
  const browser=await engine.launch(engine===chromium?{executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true}:{headless:true});
  try{
   const context=await browser.newContext();
   const errors=[],requests=[];
   let failOrder=true,releaseOrder;
   const product={id:1,name:'Collar de prueba',categoryId:1,category:'Collares',price:20,stock:20,src:'/assets/certificado-material.png',alt:'Pieza de prueba',active:true};
   await context.route('**/*',async route=>{
    const url=new URL(route.request().url());
    if(url.origin!==base)return route.abort();
    const pathname=url.pathname;
    if(pathname==='/api/auth/me'){
     await new Promise(resolve=>setTimeout(resolve,30));
     return route.fulfill({json:{user:null,csrf:'prueba',needsAdmin:false}});
    }
    if(pathname==='/api/products')return route.fulfill({json:[product]});
    if(pathname==='/api/categories')return route.fulfill({json:[{id:1,name:'Collares'}]});
    if(pathname==='/api/config')return route.fulfill({json:{}});
    if(pathname==='/api/orders'){
     const body=route.request().postDataJSON();requests.push(body);
     if(failOrder)return route.abort();
     await new Promise(resolve=>releaseOrder=resolve);
     return route.fulfill({json:{id:123,total:20,status:'pendiente',paymentStatus:'pendiente',paymentMethod:body.paymentMethod,
      createdAt:'2026-11-05T12:00:00Z',customer:body.firstName+' '+body.lastName,email:body.email,emailSent:false,
      address:body.addressLine,district:'Lima',province:'Lima',department:'Lima',certificateAvailable:false,
      items:[{name:product.name,quantity:1,price:20}]}});
    }
    if(pathname.startsWith('/api/'))throw Error('API no prevista: '+pathname);
    const file=path.resolve(root,'.'+decodeURIComponent(pathname));
    assert(file.startsWith(root+path.sep));
    try{return await route.fulfill({body:await fs.readFile(file),contentType:types[path.extname(file)]||'application/octet-stream'});}
    catch{return route.fulfill({status:404,body:''});}
   });
   const page=await context.newPage();page.on('pageerror',error=>errors.push(error.message));
   await page.goto(base+'/catalogo.html');
   await page.locator('[data-add="1"]').click();
   await page.locator('#open-cart').click();await page.locator('#checkout').click();
   await page.locator('#checkout-modal').waitFor({state:'visible'});
   for(const [name,value] of Object.entries({email:'checkout@example.test',firstName:'Lucía',lastName:'Prueba',documentId:'12345678',address:'Av. Pruebas 123',phone:'987654321'})){
    await page.locator('#checkout-form [name="'+name+'"]').fill(value);
   }
   await page.waitForFunction(()=>document.querySelector('#department').options.length>1);
   await page.evaluate(()=>{
    for(const [id,value] of [['department','LIMA'],['province','LIMA'],['district','150101']]){
     const field=document.getElementById(id);field.value=value;field.dispatchEvent(new Event('change',{bubbles:true}));
    }
    CieloControls.refresh();
   });
   await page.locator('.checkout-submit').click();
   await page.waitForFunction(()=>!!document.querySelector('#checkout-error').textContent,null,{timeout:3000}).catch(()=>{
    assert.fail('El botón no envió el pedido. Errores JavaScript: '+errors.join('; '));
   });
   assert.equal(requests.length,1,'El formulario debe alcanzar POST /orders después de verificar la sesión');
   assert.match(await page.locator('#checkout-error').textContent(),/conexión/);
   assert.equal(await page.locator('.checkout-submit').isDisabled(),false);
   assert.equal(await page.locator('[name=address]').inputValue(),'Av. Pruebas 123');
   assert.equal(await page.locator('#bag-count').textContent(),'1');
   failOrder=false;
   await page.locator('.checkout-submit').click();
   await page.waitForFunction(()=>!document.querySelector('.checkout-processing').hidden);
   await page.evaluate(()=>{
    for(let i=0;i<2;i++)document.querySelector('#checkout-form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
   });
   await page.waitForTimeout(150);
   assert.equal(requests.length,2,'Los envíos repetidos no duplican la petición');
   assert.equal(await page.locator('#close-checkout').isDisabled(),true);
   assert.equal(requests[0].requestId,requests[1].requestId,'El reintento conserva la clave idempotente');
   assert.equal(requests[1].addressLine,'Av. Pruebas 123');
   assert.equal(requests[1].ubigeo,'150101');
   releaseOrder();
   await page.locator('#pedido-dialog').waitFor({state:'visible'});
   await page.waitForFunction(()=>document.querySelector('#bag-count').textContent==='0');
   assert.match(await page.locator('#pedido-reference').textContent(),/#123/);
   assert.equal(await page.locator('.checkout-submit').isDisabled(),false);
   assert.deepEqual(errors,[],'Sin errores JavaScript');
   console.log(engine.name()+': OK. Envío, fallo de red, reintento, protección contra duplicados y confirmación.');
  }finally{await browser.close();}
 }
})().catch(error=>{console.error(error);process.exitCode=1;});
